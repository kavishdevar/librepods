package me.kavishdevar.librepods.wear.core

import android.bluetooth.BluetoothDevice
import android.util.Log
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.bluetooth.ATTManagerv2
import me.kavishdevar.librepods.bluetooth.BLEManager
import me.kavishdevar.librepods.bluetooth.BluetoothConnectionManager

/**
 * Wear-facing controller for the autonomous AirPods protocol stack.
 *
 * This class is intentionally small. Android phone-specific takeover,
 * notifications, widgets, telephony and root/Xposed workarounds stay out of
 * the Wear layer. Protocol implementations remain reusable underneath it.
 */
class AirPodsController {
    private val tag = "AirPodsController"

    private var aacp: AACPManager? = null
    private var att: ATTManagerv2? = null
    private var ble: BLEManager? = null

    var connectedDevice: BluetoothDevice? = null
        private set

    val isConnected: Boolean
        get() = BluetoothConnectionManager.aacpSocket?.isConnected == true

    fun initialize(
        aacpManager: AACPManager = AACPManager(),
        attManager: ATTManagerv2 = ATTManagerv2(),
        bleManager: BLEManager,
    ) {
        aacp = aacpManager
        att = attManager
        ble = bleManager
        Log.d(tag, "AirPods protocol controller initialized")
    }

    fun attachDevice(device: BluetoothDevice) {
        connectedDevice = device
    }

    fun detachDevice() {
        connectedDevice = null
    }

    fun shutdown() {
        connectedDevice = null
        aacp = null
        att = null
        ble = null
        Log.d(tag, "AirPods protocol controller shut down")
    }
}
