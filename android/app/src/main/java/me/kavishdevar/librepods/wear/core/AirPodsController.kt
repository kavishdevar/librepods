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
class AirPodsController(
    private val context: Context,
    private val transport: WearBluetoothConnection,
) {
    private val tag = "AirPodsController"
    private val stateStore = AirPodsStateStore()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val state: StateFlow<AirPodsState> = stateStore.state

    private var aacp: AACPManager? = null
    private var ble: BLEManager? = null
    private var connectedDevice: BluetoothDevice? = null
    private var aacpReaderJob: Job? = null

    private val bleListener = object : BLEManager.AirPodsStatusListener {
        override fun onDeviceStatusChanged(
            device: BLEManager.AirPodsStatus,
            previousStatus: BLEManager.AirPodsStatus?,
        ) {
            applyBleStatus(device)
        }

        override fun onBroadcastFromNewAddress(device: BLEManager.AirPodsStatus) {
            applyBleStatus(device)
        }

        override fun onLidStateChanged(lidOpen: Boolean) {
            stateStore.update { it.copy(caseLidOpen = lidOpen) }
        }

        override fun onEarStateChanged(
            device: BLEManager.AirPodsStatus,
            leftInEar: Boolean,
            rightInEar: Boolean,
        ) {
            applyBleStatus(device)
        }

        override fun onBatteryChanged(device: BLEManager.AirPodsStatus) {
            applyBleStatus(device)
        }

        override fun onDeviceDisappeared() {
            Log.d(tag, "AirPods BLE advertisement disappeared")
        }
    }

    fun initialize(aacpManager: AACPManager, bleManager: BLEManager) {
        aacp = aacpManager
        ble = bleManager
        bleManager.setAirPodsStatusListener(bleListener)
        runCatching { bleManager.startScanning() }
            .onFailure { Log.w(tag, "BLE status scanner could not start", it) }
        Log.d(tag, "Protocol core initialized; BLE status monitoring started")
    }

    /**
     * Find the paired AirPods and immediately establish the real AACP control
     * channel. The BLE scanner supplies battery/ear/case telemetry in parallel.
     */
    @SuppressLint("MissingPermission")
    fun connectToBondedAirPods(): Boolean {
        markConnecting()
        try {
            val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            if (adapter == null || !adapter.isEnabled) {
                onError("Bluetooth is disabled or unavailable")
                return false
            }

            val device = adapter.bondedDevices.firstOrNull { device ->
                val name = device.name.orEmpty()
                name.contains("AirPods", ignoreCase = true) ||
                    name.contains("Pods", ignoreCase = true)
            }

            if (device == null) {
                onError("No paired AirPods found")
                return false
            }

            connectedDevice = device
            stateStore.update {
                it.copy(
                    deviceName = device.name ?: "AirPods",
                    address = device.address,
                    connecting = true,
                    connected = false,
                    lastError = null,
                )
            }

            scope.launch {
                try {
                    // Classic AACP uses the documented L2CAP PSM 0x1001.
                    transport.connectAacp(device)

                    val manager = aacp ?: error("AACP manager is not initialized")
                    // The original LibrePods AACP implementation remains intact;
                    // BluetoothConnectionManager is bound to our Wear session.
                    manager.sendPacket(manager.createHandshakePacket())
                    manager.sendNotificationRequest()

                    startAacpReader(manager)

                    stateStore.update {
                        it.copy(
                            connecting = false,
                            connected = true,
                            lastError = null,
                        )
                    }
                    Log.i(tag, "AirPods AACP connection established")
                } catch (error: Throwable) {
                    onError(
                        "AACP connection failed: ${error.message ?: error.javaClass.simpleName}",
                        error,
                    )
                    runCatching { transport.close() }
                }
            }
            return true
        } catch (security: SecurityException) {
            onError("Bluetooth permission is required", security)
            return false
        } catch (error: Throwable) {
            onError("Bluetooth discovery failed: ${error.message ?: error.javaClass.simpleName}", error)
            return false
        }
    }

    private fun startAacpReader(manager: AACPManager) {
        aacpReaderJob?.cancel()
        aacpReaderJob = scope.launch {
            val input = transport.aacpInput()
            val buffer = ByteArray(4096)
            while (true) {
                val count = input.read(buffer)
                if (count <= 0) break
                val packet = buffer.copyOf(count)
                runCatching { manager.receivePacket(packet) }
                    .onFailure { Log.w(tag, "Failed to parse AACP packet", it) }
            }
            Log.d(tag, "AACP reader stopped")
        }
    }

    private fun applyBleStatus(device: BLEManager.AirPodsStatus) {
        stateStore.update {
            it.copy(
                deviceName = if (device.model != "Unknown") device.model else it.deviceName,
                address = device.address,
                leftBattery = device.leftBattery,
                rightBattery = device.rightBattery,
                caseBattery = device.caseBattery,
                leftCharging = device.isLeftCharging,
                rightCharging = device.isRightCharging,
                caseCharging = device.isCaseCharging,
                caseLidOpen = device.lidOpen,
                leftInEar = device.isLeftInEar,
                rightInEar = device.isRightInEar,
            )
        }
    }

    fun markConnecting() {
        stateStore.update { it.copy(connecting = true, lastError = null) }
    }

    fun attachDevice(device: BluetoothDevice, name: String? = null) {
        connectedDevice = device
        stateStore.update {
            it.copy(
                deviceName = name ?: "AirPods",
                address = runCatching { device.address }.getOrNull(),
                connecting = false,
                connected = true,
                lastError = null,
            )
        }
    }

    fun onBattery(left: Int?, right: Int?, caseBattery: Int?) {
        stateStore.update { it.copy(leftBattery = left, rightBattery = right, caseBattery = caseBattery) }
    }

    fun onEarDetection(leftInEar: Boolean, rightInEar: Boolean) {
        stateStore.update { it.copy(leftInEar = leftInEar, rightInEar = rightInEar) }
    }

    fun onListeningModeChanged(mode: ListeningMode) {
        stateStore.update { it.copy(listeningMode = mode) }
    }

    fun onError(message: String, cause: Throwable? = null) {
        Log.e(tag, message, cause)
        stateStore.update { it.copy(connecting = false, connected = false, lastError = message) }
    }

    fun disconnect() {
        connectedDevice = null
        aacpReaderJob?.cancel()
        aacpReaderJob = null
        runCatching { transport.close() }
        stateStore.update { it.copy(connected = false, connecting = false) }
    }

    fun shutdown() {
        disconnect()
        runCatching { ble?.stopScanning() }
        scope.cancel()
        aacp = null
        ble = null
        stateStore.reset()
        Log.d(tag, "Protocol core shut down")
    }
}
