package me.kavishdevar.librepods.wear.core

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.StateFlow
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.bluetooth.BLEManager
import me.kavishdevar.librepods.wear.bluetooth.AirPodsConnectionSession
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

    @SuppressLint("MissingPermission")
    fun connectToBondedAirPods(): Boolean {
        markConnecting()
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

        // Discovery of the exact AACP/ATT PSM values remains protocol work.
        // For the first build we expose the real bonded-device status without
        // pretending that a protocol connection succeeded.
        stateStore.update {
            it.copy(
                deviceName = device.name ?: "AirPods",
                address = device.address,
                connected = false,
                connecting = false,
                lastError = "AirPods found: protocol connection is not wired yet",
            )
        }
        return true
    }

    fun markConnecting() {
        stateStore.update { it.copy(connecting = true, lastError = null) }
    }

    fun attachDevice(device: BluetoothDevice, name: String? = null) {
        connectedDevice = device
        stateStore.update {
            it.copy(
                deviceName = name ?: device.name ?: "AirPods",
                address = device.address,
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
        transport.close()
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
