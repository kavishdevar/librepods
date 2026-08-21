package me.kavishdevar.librepods.wear.core

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.bluetooth.BLEManager
import me.kavishdevar.librepods.data.Capability
import me.kavishdevar.librepods.data.CustomEq
import me.kavishdevar.librepods.wear.bluetooth.AirPodsProtocolDiagnostics
import me.kavishdevar.librepods.wear.bluetooth.WearBluetoothConnection

/** Wear-facing controller for the autonomous AirPods stack. */
class AirPodsController(private val context: Context, private val transport: WearBluetoothConnection) {
    private val tag = "AirPodsController"
    private val stateStore = AirPodsStateStore()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences("librepods_wear", Context.MODE_PRIVATE)
    val state: StateFlow<AirPodsState> = stateStore.state
    private var aacp: AACPManager? = null
    private var ble: BLEManager? = null
    private var connectedDevice: BluetoothDevice? = null
    private var aacpReaderJob: Job? = null
    private var readyWatchJob: Job? = null

    private val bleListener = object : BLEManager.AirPodsStatusListener {
        override fun onDeviceStatusChanged(device: BLEManager.AirPodsStatus, previousStatus: BLEManager.AirPodsStatus?) = applyBleStatus(device)
        override fun onBroadcastFromNewAddress(device: BLEManager.AirPodsStatus) = applyBleStatus(device)
        override fun onLidStateChanged(lidOpen: Boolean) { stateStore.update { it.copy(caseLidOpen = lidOpen) } }
        override fun onEarStateChanged(device: BLEManager.AirPodsStatus, leftInEar: Boolean, rightInEar: Boolean) = applyBleStatus(device)
        override fun onBatteryChanged(device: BLEManager.AirPodsStatus) = applyBleStatus(device)
        override fun onDeviceDisappeared() { Log.d(tag, "AirPods BLE advertisement disappeared") }
    }

    private val aacpCallback = object : AACPManager.PacketCallback {
        override fun onBatteryInfoReceived(batteryInfo: ByteArray) {
            recordPacket(batteryInfo)
            val parsed = AirPodsProtocolDiagnostics.parseBattery(batteryInfo)
            if (parsed == null) {
                Log.w(tag, "AACP battery frame rejected: ${AirPodsProtocolDiagnostics.hex(batteryInfo)}")
                return
            }
            val left = parsed.firstOrNull { it.type == AirPodsProtocolDiagnostics.Component.LEFT }
            val right = parsed.firstOrNull { it.type == AirPodsProtocolDiagnostics.Component.RIGHT }
            val case = parsed.firstOrNull { it.type == AirPodsProtocolDiagnostics.Component.CASE }
            stateStore.update {
                it.copy(
                    leftBattery = left?.level ?: it.leftBattery,
                    rightBattery = right?.level ?: it.rightBattery,
                    caseBattery = case?.level ?: it.caseBattery,
                    leftCharging = left?.charging ?: it.leftCharging,
                    rightCharging = right?.charging ?: it.rightCharging,
                    caseCharging = case?.charging ?: it.caseCharging,
                    protocolStage = "READY",
                    connected = true,
                    connecting = false,
                )
            }
        }

        override fun onEarDetectionReceived(earDetection: ByteArray) {
            recordPacket(earDetection)
            AirPodsProtocolDiagnostics.parseEarDetection(earDetection)?.let { (left, right) ->
                onEarDetection(left, right)
            }
        }

        override fun onConversationAwarenessReceived(conversationAwareness: ByteArray) { recordPacket(conversationAwareness) }
        override fun onControlCommandReceived(controlCommand: ByteArray) { recordPacket(controlCommand) }
        override fun onDeviceInformationReceived(deviceInformation: AACPManager.AirPodsInformation) {
            stateStore.update { it.copy(deviceName = deviceInformation.name.ifBlank { it.deviceName }, protocolStage = "READY", connected = true, connecting = false) }
        }
        override fun onHeadTrackingReceived(headTracking: ByteArray) { recordPacket(headTracking) }
        override fun onUnknownPacketReceived(packet: ByteArray) {
            recordPacket(packet)
            Log.d(tag, "AACP unknown packet: ${AirPodsProtocolDiagnostics.hex(packet)}")
        }
        override fun onProximityKeysReceived(proximityKeys: ByteArray) { recordPacket(proximityKeys) }
        override fun onStemPressReceived(stemPress: ByteArray) { recordPacket(stemPress) }
        override fun onAudioSourceReceived(audioSource: ByteArray) { recordPacket(audioSource) }
        override fun onOwnershipChangeReceived(owns: Boolean) { recordPacket(null); Log.d(tag, "AACP ownership=$owns") }
        override fun onConnectedDevicesReceived(connectedDevices: List<AACPManager.ConnectedDevice>) { recordPacket(null) }
        override fun onOwnershipToFalseRequest(sender: String, reasonReverseTapped: Boolean) { Log.d(tag, "AACP ownership revoke requested by $sender") }
        override fun onShowNearbyUI(sender: String) { Log.d(tag, "AACP nearby UI requested by $sender") }
        override fun onHeadphoneAccommodationReceived(eqData: FloatArray) { recordPacket(null) }
        override fun onCustomEqReceived(customEq: CustomEq) { recordPacket(null) }
        override fun onCapabilitiesReceived(capabilities: List<Capability>) { recordPacket(null) }
    }

    fun initialize(aacpManager: AACPManager, bleManager: BLEManager) {
        aacp = aacpManager
        ble = bleManager
        aacpManager.bindTransport(transport)
        aacpManager.setPacketCallback(aacpCallback)
        bleManager.setAirPodsStatusListener(bleListener)
        runCatching { bleManager.startScanning() }.onFailure { Log.w(tag, "BLE status scanner could not start", it) }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String, name: String = "AirPods"): Boolean {
        markConnecting()
        return try {
            val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
                ?: return fail("Bluetooth is unavailable")
            if (!adapter.isEnabled) return fail("Bluetooth is disabled")
            val device = adapter.getRemoteDevice(address)
            connectedDevice = device
            prefs.edit().putString("selected_address", address).putString("selected_name", name).apply()
            stateStore.update { it.copy(deviceName = name, address = address, connecting = true, connected = false, protocolStage = "CONNECTING", lastError = null) }
            scope.launch { connectTransport(device) }
            true
        } catch (e: SecurityException) { fail("Bluetooth permission is required", e) }
        catch (e: IllegalArgumentException) { fail("Invalid Bluetooth device", e) }
    }

    @SuppressLint("MissingPermission")
    fun connectToBondedAirPods(): Boolean {
        val saved = prefs.getString("selected_address", null)
        if (saved != null) return connectToDevice(saved, prefs.getString("selected_name", "AirPods") ?: "AirPods")
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        val device = adapter?.bondedDevices?.firstOrNull { it.name.orEmpty().contains("AirPods", true) || it.name.orEmpty().contains("Pods", true) }
            ?: return fail("No paired AirPods found")
        return connectToDevice(device.address, device.name ?: "AirPods")
    }

    private suspend fun connectTransport(device: BluetoothDevice) {
        try {
            stateStore.update { it.copy(protocolStage = "L2CAP") }
            transport.connectAacp(device)
            val manager = aacp ?: error("AACP manager is not initialized")
            startAacpReader(manager)
            check(manager.startSession()) { "AACP handshake could not be sent" }
            stateStore.update { it.copy(protocolStage = "HANDSHAKE_SENT", connecting = true, connected = false) }
            readyWatchJob?.cancel()
            readyWatchJob = scope.launch {
                repeat(50) {
                    delay(100)
                    if (manager.sessionState == AACPManager.SessionState.READY) {
                        stateStore.update { it.copy(protocolStage = "READY", connecting = false, connected = true, lastError = null) }
                        return@launch
                    }
                }
                if (manager.sessionState != AACPManager.SessionState.READY) {
                    onError("AACP handshake timeout (${manager.sessionState})")
                }
            }
        } catch (e: Throwable) {
            onError("AACP connection failed: ${e.message ?: e.javaClass.simpleName}", e)
            runCatching { transport.close() }
        }
    }

    private fun startAacpReader(manager: AACPManager) {
        aacpReaderJob?.cancel()
        aacpReaderJob = scope.launch {
            try {
                val input = transport.aacpInput
                val buffer = ByteArray(4096)
                while (true) {
                    val count = input.read(buffer)
                    if (count <= 0) break
                    val packet = buffer.copyOf(count)
                    recordPacket(packet)
                    manager.receivePacket(packet)
                }
                if (connectedDevice != null) onError("AACP socket closed by device")
            } catch (e: Throwable) {
                if (connectedDevice != null) onError("AACP reader stopped: ${e.message ?: e.javaClass.simpleName}", e)
            }
        }
    }

    private fun recordPacket(packet: ByteArray?) {
        if (packet == null) return
        val frame = AirPodsProtocolDiagnostics.decode(packet)
        stateStore.update {
            it.copy(
                lastPacketOpcode = AirPodsProtocolDiagnostics.opcodeName(frame?.opcode),
                lastPacketHex = AirPodsProtocolDiagnostics.hex(packet),
            )
        }
    }

    private fun applyBleStatus(device: BLEManager.AirPodsStatus) {
        stateStore.update { it.copy(deviceName = if (device.model != "Unknown") device.model else it.deviceName, address = device.address, leftBattery = device.leftBattery, rightBattery = device.rightBattery, caseBattery = device.caseBattery, leftCharging = device.isLeftCharging, rightCharging = device.isRightCharging, caseCharging = device.isCaseCharging, caseLidOpen = device.lidOpen, leftInEar = device.isLeftInEar, rightInEar = device.isRightInEar) }
    }

    fun markConnecting() { stateStore.update { it.copy(connecting = true, lastError = null, protocolStage = "CONNECTING") } }
    fun onBattery(left: Int?, right: Int?, caseBattery: Int?) { stateStore.update { it.copy(leftBattery = left, rightBattery = right, caseBattery = caseBattery) } }
    fun onEarDetection(leftInEar: Boolean, rightInEar: Boolean) { stateStore.update { it.copy(leftInEar = leftInEar, rightInEar = rightInEar) } }
    fun onListeningModeChanged(mode: ListeningMode) { stateStore.update { it.copy(listeningMode = mode) } }
    fun onError(message: String, cause: Throwable? = null) { Log.e(tag, message, cause); stateStore.update { it.copy(connecting = false, connected = false, protocolStage = "FAILED", lastError = message) } }
    private fun fail(message: String, cause: Throwable? = null): Boolean { onError(message, cause); return false }

    fun disconnect() {
        connectedDevice = null
        readyWatchJob?.cancel(); readyWatchJob = null
        aacpReaderJob?.cancel(); aacpReaderJob = null
        runCatching { transport.close() }
        aacp?.let { if (it.sessionState != AACPManager.SessionState.IDLE) it.unbindTransport() }
        stateStore.update { it.copy(connected = false, connecting = false, protocolStage = "IDLE") }
    }

    fun shutdown() { disconnect(); runCatching { ble?.stopScanning() }; aacp?.unbindTransport(); scope.cancel(); aacp = null; ble = null; stateStore.reset() }
}
