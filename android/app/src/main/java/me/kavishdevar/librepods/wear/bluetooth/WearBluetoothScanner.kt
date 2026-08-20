package me.kavishdevar.librepods.wear.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thin boundary over the platform BLE scanner.
 *
 * Device discovery is deliberately owned by Android/Wear OS. This class only
 * normalizes scan results for the Wear UI and never implements AirPods
 * protocol detection itself.
 */
class WearBluetoothScanner(context: Context) {
    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter
    private val scanner: BluetoothLeScanner?
        get() = adapter?.bluetoothLeScanner

    private val mutableDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = mutableDevices.asStateFlow()

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (!hasScanPermission()) return
            val device = result.device
            val current = mutableDevices.value.toMutableList()
            val index = current.indexOfFirst { it.address == device.address }
            if (index >= 0) current[index] = device else current += device
            mutableDevices.value = current
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { onScanResult(ScanSettingsCallbackType.BATCH, it) }
        }

        override fun onScanFailed(errorCode: Int) {
            scanError.value = errorCode
        }
    }

    private object ScanSettingsCallbackType { const val BATCH = 1 }

    private val mutableScanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = mutableScanning.asStateFlow()

    private val mutableScanError = MutableStateFlow<Int?>(null)
    val scanError: StateFlow<Int?> = mutableScanError.asStateFlow()

    private val scanErrorAlias get() = mutableScanError

    fun isSupported(): Boolean = adapter != null
    fun isEnabled(): Boolean = adapter?.isEnabled == true

    fun startScan() {
        if (!hasScanPermission() || !isEnabled() || mutableScanning.value) return
        mutableScanError.value = null
        mutableDevices.value = emptyList()
        scanner?.startScan(callback)
        mutableScanning.value = true
    }

    fun stopScan() {
        if (!mutableScanning.value) return
        if (hasScanPermission()) runCatching { scanner?.stopScan(callback) }
        mutableScanning.value = false
    }

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
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun hasScanPermission(): Boolean =
        android.os.Build.VERSION.SDK_INT < 31 ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
}
