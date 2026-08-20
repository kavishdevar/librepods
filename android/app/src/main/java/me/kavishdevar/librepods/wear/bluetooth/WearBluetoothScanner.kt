package me.kavishdevar.librepods.wear.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Wear OS Bluetooth discovery boundary.
 *
 * The scanner deliberately exposes platform devices only. AirPods-specific
 * identification belongs in the protocol/controller layer.
 */
class WearBluetoothScanner(context: Context) {
    private val appContext = context.applicationContext
    private val bluetoothManager =
        appContext.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private val mutableDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = mutableDevices.asStateFlow()

    fun isSupported(): Boolean = adapter != null

    fun isEnabled(): Boolean = adapter?.isEnabled == true

    fun bondedDevices(): List<BluetoothDevice> {
        if (!hasConnectPermission()) return emptyList()
        return adapter?.bondedDevices?.toList().orEmpty()
    }

    fun remember(device: BluetoothDevice) {
        if (!hasConnectPermission()) return
        mutableDevices.value = (mutableDevices.value + device).distinctBy { it.address }
    }

    private fun hasConnectPermission(): Boolean =
        android.os.Build.VERSION.SDK_INT < 31 ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) == PackageManager.PERMISSION_GRANTED
}
