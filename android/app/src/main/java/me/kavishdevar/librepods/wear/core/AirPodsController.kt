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
    private var reconnectJob: Job? = null
    private var manualDisconnect = false

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
            val parsed = AirPodsProtocolDiagnostics.parseBattery(batteryInfo) ?: return
            val left = parsed.firstOrNull { it.type == AirPodsProtocolDiagnostics.Component.LEFT }
            val right = parsed.firstOrNull { it.type == AirPodsProtocolDiagnostics.Component.RIGHT }
            val case = parsed.firstOrNull { it.type == AirPodsProtocolDiagnostics.Component.CASE }
            stateStore.update { it.copy(leftBattery = left?.level ?: it.leftBattery, rightBattery = right?.level ?: it.rightBattery, caseBattery = case?.level ?: it.caseBattery, leftCharging = left?.charging ?: it.leftCharging, rightCharging = right?.charging ?: it.rightCharging, caseCharging = case?.charging ?: it.caseCharging, protocolStage = "READY", connected = true, connecting = false) }
        }
        override fun onEarDetectionReceived(earDetection: ByteArray) { recordPacket(earDetection); AirPodsProtocolDiagnostics.parseEarDetection(earDetection)?.let { (left, right) -> onEarDetection(left, right) } }
        override fun onConversationAwarenessReceived(conversationAwareness: ByteArray) { recordPacket(conversationAwareness) }
        override fun onControlCommandReceived(controlCommand: ByteArray) {
            recordPacket(controlCommand)
            runCatching { AACPManager.ControlCommand.fromByteArray(controlCommand) }.onSuccess { command ->
                when (AACPManager.Companion.ControlCommandIdentifiers.fromByte(command.identifier)) {
                    AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE -> when (command.value.firstOrNull()?.toInt()?.and(0xFF)) { 1 -> onListeningModeChanged(ListeningMode.OFF); 2 -> onListeningModeChanged(ListeningMode.ANC); 3 -> onListeningModeChanged(ListeningMode.TRANSPARENCY); else -> Unit }
                    AACPManager.Companion.ControlCommandIdentifiers.EAR_DETECTION_CONFIG -> stateStore.update { it.copy(earDetectionEnabled = command.value.firstOrNull()?.toInt()?.and(0xFF) == 1) }
                    AACPManager.Companion.ControlCommandIdentifiers.CONVERSATION_DETECT_CONFIG -> stateStore.update { it.copy(conversationalAwarenessEnabled = command.value.firstOrNull()?.toInt()?.and(0xFF) == 1) }
                    else -> Unit
                }
            }
        }
        override fun onDeviceInformationReceived(deviceInformation: AACPManager.Companion.AirPodsInformation) { stateStore.update { it.copy(deviceName = deviceInformation.name.ifBlank { it.deviceName }, protocolStage = "READY", connected = true, connecting = false) } }
        override fun onHeadTrackingReceived(headTracking: ByteArray) { recordPacket(headTracking) }
        override fun onUnknownPacketReceived(packet: ByteArray) { recordPacket(packet) }
        override fun onProximityKeysReceived(proximityKeys: ByteArray) { recordPacket(proximityKeys) }
        override fun onStemPressReceived(stemPress: ByteArray) { recordPacket(stemPress) }
        override fun onAudioSourceReceived(audioSource: ByteArray) { recordPacket(audioSource) }
        override fun onOwnershipChangeReceived(owns: Boolean) { Log.d(tag, "AACP ownership=$owns") }
        override fun onConnectedDevicesReceived(connectedDevices: List<AACPManager.Companion.ConnectedDevice>) { Log.d(tag, "AACP connected devices=${connectedDevices.size}") }
        override fun onOwnershipToFalseRequest(sender: String, reasonReverseTapped: Boolean) { Log.d(tag, "AACP ownership revoke requested by $sender") }
        override fun onShowNearbyUI(sender: String) { Log.d(tag, "AACP nearby UI requested by $sender") }
        override fun onHeadphoneAccommodationReceived(eqData: FloatArray) { Log.d(tag, "AACP EQ frame received: ${eqData.size} values") }
        override fun onCustomEqReceived(customEq: CustomEq) { Log.d(tag, "AACP custom EQ received") }
        override fun onCapabilitiesReceived(capabilities: List<Capability>) { Log.d(tag, "AACP capabilities=${capabilities.size}") }
    }

    fun initialize(aacpManager: AACPManager, bleManager: BLEManager) { aacp = aacpManager; ble = bleManager; aacpManager.bindTransport(transport); aacpManager.setPacketCallback(aacpCallback); bleManager.setAirPodsStatusListener(bleListener); runCatching { bleManager.startScanning() }.onFailure { Log.w(tag, "BLE status scanner could not start", it) } }

    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String, name: String = "AirPods"): Boolean {
        manualDisconnect = false; reconnectJob?.cancel(); markConnecting()
        return try {
            val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return fail("Bluetooth is unavailable")
            if (!adapter.isEnabled) return fail("Bluetooth is disabled")
            val device = adapter.getRemoteDevice(address); connectedDevice = device
            prefs.edit().putString("selected_address", address).putString("selected_name", name).apply()
            stateStore.update { it.copy(deviceName = name, address = address, connecting = true, connected = false, protocolStage = "CONNECTING", lastError = null) }
            scope.launch { connectTransport(device) }; true
        } catch (e: SecurityException) { fail("Bluetooth permission is required", e) } catch (e: IllegalArgumentException) { fail("Invalid Bluetooth device", e) }
    }

    @SuppressLint("MissingPermission")
    fun connectToBondedAirPods(): Boolean {
        val saved = prefs.getString("selected_address", null)
        if (saved != null) return connectToDevice(saved, prefs.getString("selected_name", "AirPods") ?: "AirPods")
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        val device = adapter?.bondedDevices?.firstOrNull { it.name.orEmpty().contains("AirPods", true) || it.name.orEmpty().contains("Pods", true) } ?: return fail("No paired AirPods found")
        return connectToDevice(device.address, device.name ?: "AirPods")
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectTransport(device: BluetoothDevice) {
        try {
            stateStore.update { it.copy(protocolStage = "L2CAP") }; transport.connectAacp(device)
            val manager = aacp ?: error("AACP manager is not initialized")
            startAacpReader(manager); check(manager.startSession()) { "AACP handshake could not be sent" }
            stateStore.update { it.copy(protocolStage = "HANDSHAKE_SENT", connecting = true, connected = false) }
            readyWatchJob?.cancel(); readyWatchJob = scope.launch {
                repeat(50) { delay(100); if (manager.sessionState == AACPManager.SessionState.READY) { stateStore.update { it.copy(protocolStage = "READY", connecting = false, connected = true, lastError = null) }; refreshState(); return@launch } }
                if (manager.sessionState != AACPManager.SessionState.READY) onError("AACP handshake timeout (${manager.sessionState})")
            }
        } catch (e: Throwable) { onError("AACP connection failed: ${e.message ?: e.javaClass.simpleName}", e); runCatching { transport.close() } }
    }

    private fun startAacpReader(manager: AACPManager) {
        aacpReaderJob?.cancel(); aacpReaderJob = scope.launch {
            try {
                val input = transport.aacpInput; val buffer = ByteArray(4096)
                while (true) { val count = input.read(buffer); if (count <= 0) break; val packet = buffer.copyOf(count); recordPacket(packet); manager.receivePacket(packet) }
                if (!manualDisconnect && connectedDevice != null) scheduleReconnect("AACP socket closed by device")
            } catch (e: Throwable) { if (!manualDisconnect && connectedDevice != null) scheduleReconnect("AACP reader stopped: ${e.message ?: e.javaClass.simpleName}") }
        }
    }

    private fun scheduleReconnect(reason: String) {
        if (manualDisconnect || connectedDevice == null || reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            onError(reason); val device = connectedDevice ?: return@launch
            for (attempt in 1..3) { if (manualDisconnect) return@launch; delay(attempt * 1000L); Log.i(tag, "AACP reconnect attempt $attempt/3"); runCatching { transport.close() }; runCatching { aacp?.unbindTransport(); aacp?.bindTransport(transport) }; stateStore.update { it.copy(connecting = true, connected = false, protocolStage = "RECONNECT_$attempt", lastError = null) }; connectTransport(device); if (aacp?.sessionState == AACPManager.SessionState.READY) return@launch }
            onError("AACP reconnect failed after 3 attempts")
        }
    }

    private fun recordPacket(packet: ByteArray?) { if (packet == null) return; val frame = AirPodsProtocolDiagnostics.decode(packet); stateStore.update { it.copy(lastPacketOpcode = AirPodsProtocolDiagnostics.opcodeName(frame?.opcode), lastPacketHex = AirPodsProtocolDiagnostics.hex(packet)) } }
    private fun applyBleStatus(device: BLEManager.AirPodsStatus) { stateStore.update { it.copy(deviceName = if (device.model != "Unknown") device.model else it.deviceName, address = device.address, leftBattery = device.leftBattery, rightBattery = device.rightBattery, caseBattery = device.caseBattery, leftCharging = device.isLeftCharging, rightCharging = device.isRightCharging, caseCharging = device.isCaseCharging, caseLidOpen = device.lidOpen, leftInEar = device.isLeftInEar, rightInEar = device.isRightInEar) } }
    fun markConnecting() { stateStore.update { it.copy(connecting = true, lastError = null, protocolStage = "CONNECTING") } }
    fun onBattery(left: Int?, right: Int?, caseBattery: Int?) { stateStore.update { it.copy(leftBattery = left, rightBattery = right, caseBattery = caseBattery) } }
    fun onEarDetection(leftInEar: Boolean, rightInEar: Boolean) { stateStore.update { it.copy(leftInEar = leftInEar, rightInEar = rightInEar) } }
    fun onListeningModeChanged(mode: ListeningMode) { stateStore.update { it.copy(listeningMode = mode) } }

    /** Execute a typed controller command; used by UI and service entry points. */
    fun submit(command: AirPodsCommand): Boolean = when (command) {
        AirPodsCommand.Connect -> connectToBondedAirPods()
        AirPodsCommand.Disconnect -> { disconnect(); true }
        AirPodsCommand.RefreshState -> refreshState()
        is AirPodsCommand.SetListeningMode -> setListeningMode(command.mode)
        is AirPodsCommand.SetEarDetection -> setEarDetection(command.enabled)
        is AirPodsCommand.SetConversationalAwareness -> setConversationalAwareness(command.enabled)
    }

    /**
     * Ask the AirPods to re-send their current state.
     *
     * The notification request is the only inherited packet that makes the
     * device replay battery, ear detection and control command values, so it
     * doubles as a state refresh once the session is READY.
     */
    fun refreshState(): Boolean {
        val manager = aacp ?: return false
        if (manager.sessionState != AACPManager.SessionState.READY) return false
        return manager.sendNotificationRequest()
    }

    fun setListeningMode(mode: ListeningMode): Boolean {
        val value = when (mode) { ListeningMode.OFF -> 1; ListeningMode.ANC -> 2; ListeningMode.TRANSPARENCY -> 3 }.toByte()
        val sent = aacp?.sendControlCommand(AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE.value, byteArrayOf(value, 0, 0, 0)) == true
        if (sent) onListeningModeChanged(mode); return sent
    }
    fun setConversationalAwareness(enabled: Boolean): Boolean { val value = if (enabled) 1 else 2; val sent = aacp?.sendControlCommand(AACPManager.Companion.ControlCommandIdentifiers.CONVERSATION_DETECT_CONFIG.value, byteArrayOf(value.toByte(), 0, 0, 0)) == true; if (sent) stateStore.update { it.copy(conversationalAwarenessEnabled = enabled) }; return sent }
    fun setEarDetection(enabled: Boolean): Boolean { val value = if (enabled) 1 else 2; val sent = aacp?.sendControlCommand(AACPManager.Companion.ControlCommandIdentifiers.EAR_DETECTION_CONFIG.value, byteArrayOf(value.toByte(), 0, 0, 0)) == true; if (sent) stateStore.update { it.copy(earDetectionEnabled = enabled) }; return sent }
    fun onError(message: String, cause: Throwable? = null) { Log.e(tag, message, cause); stateStore.update { it.copy(connecting = false, connected = false, protocolStage = "FAILED", lastError = message) } }
    private fun fail(message: String, cause: Throwable? = null): Boolean { onError(message, cause); return false }
    fun disconnect() { manualDisconnect = true; connectedDevice = null; reconnectJob?.cancel(); reconnectJob = null; readyWatchJob?.cancel(); readyWatchJob = null; aacpReaderJob?.cancel(); aacpReaderJob = null; runCatching { transport.close() }; aacp?.unbindTransport(); stateStore.update { it.copy(connected = false, connecting = false, protocolStage = "IDLE") } }
    fun shutdown() { disconnect(); runCatching { ble?.stopScanning() }; aacp?.unbindTransport(); scope.cancel(); aacp = null; ble = null; stateStore.reset() }
}
