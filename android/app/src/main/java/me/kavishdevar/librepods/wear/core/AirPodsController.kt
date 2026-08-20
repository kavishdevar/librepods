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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.bluetooth.BLEManager
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

    private val bleListener = object : BLEManager.AirPodsStatusListener {
        override fun onDeviceStatusChanged(device: BLEManager.AirPodsStatus, previousStatus: BLEManager.AirPodsStatus?) = applyBleStatus(device)
        override fun onBroadcastFromNewAddress(device: BLEManager.AirPodsStatus) = applyBleStatus(device)
        override fun onLidStateChanged(lidOpen: Boolean) { stateStore.update { it.copy(caseLidOpen = lidOpen) } }
        override fun onEarStateChanged(device: BLEManager.AirPodsStatus, leftInEar: Boolean, rightInEar: Boolean) = applyBleStatus(device)
        override fun onBatteryChanged(device: BLEManager.AirPodsStatus) = applyBleStatus(device)
        override fun onDeviceDisappeared() {
            // Listener contract is Unit; keep disappearance as a diagnostic event only.
            Log.d(tag, "AirPods BLE advertisement disappeared")
        }
    }

    fun initialize(aacpManager: AACPManager, bleManager: BLEManager) {
        aacp = aacpManager
        ble = bleManager
        aacpManager.bindTransport(transport)
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
            stateStore.update { it.copy(deviceName = name, address = address, connecting = true, connected = false, lastError = null) }
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
            transport.connectAacp(device)
            val manager = aacp ?: error("AACP manager is not initialized")
            startAacpReader(manager)
            check(manager.startSession()) { "AACP handshake could not be sent" }
            stateStore.update { it.copy(connecting = false, connected = true, lastError = null) }
        } catch (e: Throwable) {
            onError("AACP connection failed: ${e.message ?: e.javaClass.simpleName}", e)
            runCatching { transport.close() }
        }
    }

    private fun startAacpReader(manager: AACPManager) {
        aacpReaderJob?.cancel()
        aacpReaderJob = scope.launch {
            val input = transport.aacpInput
            val buffer = ByteArray(4096)
            while (true) { val count = input.read(buffer); if (count <= 0) break; manager.receivePacket(buffer.copyOf(count)) }
        }
    }

    private fun applyBleStatus(device: BLEManager.AirPodsStatus) {
        stateStore.update { it.copy(deviceName = if (device.model != "Unknown") device.model else it.deviceName, address = device.address, leftBattery = device.leftBattery, rightBattery = device.rightBattery, caseBattery = device.caseBattery, leftCharging = device.isLeftCharging, rightCharging = device.isRightCharging, caseCharging = device.isCaseCharging, caseLidOpen = device.lidOpen, leftInEar = device.isLeftInEar, rightInEar = device.isRightInEar) }
    }

    fun markConnecting() { stateStore.update { it.copy(connecting = true, lastError = null) } }
    fun onBattery(left: Int?, right: Int?, caseBattery: Int?) { stateStore.update { it.copy(leftBattery = left, rightBattery = right, caseBattery = caseBattery) } }
    fun onEarDetection(leftInEar: Boolean, rightInEar: Boolean) { stateStore.update { it.copy(leftInEar = leftInEar, rightInEar = rightInEar) } }
    fun onListeningModeChanged(mode: ListeningMode) { stateStore.update { it.copy(listeningMode = mode) } }
    fun onError(message: String, cause: Throwable? = null) { Log.e(tag, message, cause); stateStore.update { it.copy(connecting = false, connected = false, lastError = message) } }
    private fun fail(message: String, cause: Throwable? = null): Boolean { onError(message, cause); return false }

    fun disconnect() {
        connectedDevice = null
        aacpReaderJob?.cancel(); aacpReaderJob = null
        runCatching { transport.close() }
        aacp?.let { if (it.sessionState != AACPManager.SessionState.IDLE) it.unbindTransport() }
        stateStore.update { it.copy(connected = false, connecting = false) }
    }

    fun shutdown() { disconnect(); runCatching { ble?.stopScanning() }; aacp?.unbindTransport(); scope.cancel(); aacp = null; ble = null; stateStore.reset() }
}
