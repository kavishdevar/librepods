package me.kavishdevar.librepods.wear.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.kavishdevar.librepods.wear.core.AirPodsDevice

/**
 * Thin boundary over the platform BLE scanner.
 *
 * Wear OS owns discovery. We deliberately use an unfiltered low-latency scan:
 * AirPods can advertise without a useful local name, so filtering by name or
 * service UUID here would make discovery unreliable.
 */
class WearBluetoothScanner(context: Context) {
    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter
    private val scanner: BluetoothLeScanner?
        get() = adapter?.bluetoothLeScanner

    private val mutableDevices = MutableStateFlow<List<AirPodsDevice>>(emptyList())
    val devices: StateFlow<List<AirPodsDevice>> = mutableDevices.asStateFlow()

    private val mutableScanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = mutableScanning.asStateFlow()

    private val mutableScanError = MutableStateFlow<Int?>(null)
    val scanError: StateFlow<Int?> = mutableScanError.asStateFlow()

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) = addResult(result)

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::addResult)
        }

        override fun onScanFailed(errorCode: Int) {
            mutableScanError.value = errorCode
            mutableScanning.value = false
        }
    }

    fun isSupported(): Boolean = adapter != null
    fun isEnabled(): Boolean = adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    private fun addResult(result: ScanResult) {
        if (!hasScanPermission()) return
        val device = result.device
        val name = runCatching { device.name }.getOrNull()
            ?: result.scanRecord?.deviceName
            ?: "Unknown device"
        val item = AirPodsDevice(
            name = name,
            address = device.address,
            rssi = result.rssi,
            bonded = isBonded(device),
        )
        mutableDevices.value = (mutableDevices.value.filterNot { it.address == item.address } + item)
            .sortedWith(compareByDescending<AirPodsDevice> { it.bonded }.thenByDescending { it.rssi ?: -127 })
    }

    @SuppressLint("MissingPermission")
    private fun isBonded(device: BluetoothDevice): Boolean =
        hasConnectPermission() && runCatching { device.bondState == BluetoothDevice.BOND_BONDED }.getOrDefault(false)

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!hasScanPermission()) {
            mutableScanError.value = -1
            return
        }
        if (!isEnabled() || mutableScanning.value) return

        mutableScanError.value = null
        // Keep already paired devices visible while doing a fresh BLE scan.
        mutableDevices.value = bondedDevices().distinctBy { it.address }

        val bleScanner = scanner
        if (bleScanner == null) {
            mutableScanError.value = -2
            return
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        runCatching { bleScanner.startScan(null, settings, callback) }
            .onSuccess { mutableScanning.value = true }
            .onFailure { mutableScanError.value = -3 }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!mutableScanning.value) return
        if (hasScanPermission()) runCatching { scanner?.stopScan(callback) }
        mutableScanning.value = false
    }

    @SuppressLint("MissingPermission")
    fun bondedDevices(): List<AirPodsDevice> {
        if (!hasConnectPermission()) return emptyList()
        return adapter?.bondedDevices?.map { device ->
            AirPodsDevice(
                name = runCatching { device.name }.getOrNull() ?: "Paired device",
                address = device.address,
                bonded = true,
            )
        }.orEmpty()
    }

    private fun hasConnectPermission(): Boolean =
        android.os.Build.VERSION.SDK_INT < 31 ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun hasScanPermission(): Boolean =
        android.os.Build.VERSION.SDK_INT < 31 ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
}
