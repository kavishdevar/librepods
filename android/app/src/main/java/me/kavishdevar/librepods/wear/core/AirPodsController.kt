package me.kavishdevar.librepods.wear.core

import android.bluetooth.BluetoothDevice
import android.util.Log
import kotlinx.coroutines.flow.StateFlow
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.bluetooth.BLEManager
import me.kavishdevar.librepods.wear.bluetooth.WearBluetoothConnection

/**
 * Wear-facing controller for the autonomous AirPods stack.
 *
 * The controller owns application state and commands. Transport and protocol
 * implementations remain behind this boundary.
 */
class AirPodsController(
    private val transport: WearBluetoothConnection? = null,
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

    fun markConnecting() {
        stateStore.update { it.copy(connecting = true, lastError = null) }
    }

    fun onBattery(left: Int?, right: Int?, caseBattery: Int?) {
        stateStore.update {
            it.copy(leftBattery = left, rightBattery = right, caseBattery = caseBattery)
        }
    }

    fun onEarDetection(leftInEar: Boolean, rightInEar: Boolean) {
        stateStore.update { it.copy(leftInEar = leftInEar, rightInEar = rightInEar) }
    }

    fun onListeningModeChanged(mode: ListeningMode) {
        stateStore.update { it.copy(listeningMode = mode) }
    }

    fun onError(message: String, cause: Throwable? = null) {
        Log.e(tag, message, cause)
        stateStore.update { it.copy(connecting = false, lastError = message) }
    }

    fun detachDevice() {
        connectedDevice = null
        transport?.close()
        stateStore.update { it.copy(connected = false, connecting = false) }
    }

    fun shutdown() {
        detachDevice()
        aacp = null
        ble = null
        stateStore.reset()
        Log.d(tag, "Protocol core shut down")
    }
}
