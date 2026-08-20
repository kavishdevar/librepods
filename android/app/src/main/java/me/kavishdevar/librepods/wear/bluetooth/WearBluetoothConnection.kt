package me.kavishdevar.librepods.wear.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Low-level GATT lifecycle boundary for Wear OS.
 *
 * Protocol-specific ATT/AACP handling must remain above this class. Keeping
 * this adapter small makes the protocol core reusable if the transport later
 * changes from GATT to another Android Bluetooth primitive.
 */
class WearBluetoothConnection(context: Context) {
    private val appContext = context.applicationContext
    private var gatt: BluetoothGatt? = null

    private val mutableConnected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = mutableConnected.asStateFlow()

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            mutableConnected.value = newState == BluetoothGatt.STATE_CONNECTED
            if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                close()
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            // ATT packet dispatch will be connected here after the transport
            // adapter is aligned with the existing LibrePods ATT manager.
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            // Compatibility callback for older Wear OS API levels.
        }
    }

    fun connect(device: BluetoothDevice): Boolean {
        close()
        gatt = device.connectGatt(appContext, false, callback)
        return gatt != null
    }

    fun close() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        mutableConnected.value = false
    }
}
