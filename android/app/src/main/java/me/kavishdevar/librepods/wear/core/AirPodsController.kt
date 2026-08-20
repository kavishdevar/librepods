package me.kavishdevar.librepods.wear.core

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.StateFlow
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

    val state: StateFlow<AirPodsState> = stateStore.state

    private var aacp: AACPManager? = null
    private var ble: BLEManager? = null
    private var connectedDevice: BluetoothDevice? = null

    fun initialize(aacpManager: AACPManager, bleManager: BLEManager) {
        aacp = aacpManager
        ble = bleManager
        Log.d(tag, "Protocol core initialized")
    }

    /** Discover a paired AirPods device without claiming protocol connectivity. */
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

            // Discovery is deliberately separated from protocol connection.
            // A device is not marked connected until AACP/ATT succeeds.
            stateStore.update {
                it.copy(
                    deviceName = device.name ?: "AirPods",
                    address = device.address,
                    connected = false,
                    connecting = false,
                    lastError = "AirPods found; protocol connection is next",
                )
            }
            return true
        } catch (security: SecurityException) {
            onError("Bluetooth permission is required")
            return false
        } catch (error: Throwable) {
            onError("Bluetooth discovery failed: ${error.message ?: error.javaClass.simpleName}", error)
            return false
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
        runCatching { transport.close() }
        stateStore.update { it.copy(connected = false, connecting = false) }
    }

    fun shutdown() {
        disconnect()
        aacp = null
        ble = null
        stateStore.reset()
        Log.d(tag, "Protocol core shut down")
    }
}
