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

/** BLE diagnostics plus a system-paired-device view. Pairing itself is owned by Wear OS. */
class WearBluetoothScanner(context: Context) {
    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? get() = bluetoothManager?.adapter
    private val scanner: BluetoothLeScanner? get() = adapter?.bluetoothLeScanner

    private val mutableDevices = MutableStateFlow<List<AirPodsDevice>>(emptyList())
    val devices: StateFlow<List<AirPodsDevice>> = mutableDevices.asStateFlow()
    private val mutableScanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = mutableScanning.asStateFlow()
    private val mutableScanError = MutableStateFlow<Int?>(null)
    val scanError: StateFlow<Int?> = mutableScanError.asStateFlow()
    private val mutableCallbackCount = MutableStateFlow(0L)
    val callbackCount: StateFlow<Long> = mutableCallbackCount.asStateFlow()
    private val mutableLog = MutableStateFlow("No paired devices")
    val log: StateFlow<String> = mutableLog.asStateFlow()

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            mutableCallbackCount.value++
            addResult(result)
        }
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            mutableCallbackCount.value += results.size
            results.forEach(::addResult)
        }
        override fun onScanFailed(errorCode: Int) {
            mutableScanError.value = errorCode
            mutableScanning.value = false
            mutableLog.value = "Scan failed: $errorCode"
        }
    }

    fun isSupported(): Boolean = adapter != null
    fun isEnabled(): Boolean = adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    private fun addResult(result: ScanResult) {
        if (!hasScanPermission()) return
        val device = result.device
        val record = result.scanRecord
        val name = runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: record?.deviceName?.takeIf { it.isNotBlank() } ?: "Unknown BLE device"
        val ids = record?.manufacturerSpecificData?.let { data -> (0 until data.size()).map { data.keyAt(it) } }.orEmpty()
        val item = AirPodsDevice(
            name = name, address = device.address, rssi = result.rssi,
            bonded = isBonded(device), appleManufacturer = ids.any { it == APPLE_COMPANY_ID },
            serviceUuids = record?.serviceUuids.orEmpty().map { it.uuid.toString() }
        )
        val previous = mutableDevices.value.firstOrNull { it.address == item.address }
        mutableDevices.value = (mutableDevices.value.filterNot { it.address == item.address } + item)
            .sortedWith(compareByDescending<AirPodsDevice> { it.appleManufacturer }.thenByDescending { it.bonded }.thenByDescending { it.rssi ?: -127 })
        if (previous == null) mutableLog.value = "Found ${if (item.appleManufacturer) "Apple BLE" else name} (${result.rssi} dBm)"
    }

    @SuppressLint("MissingPermission")
    private fun isBonded(device: BluetoothDevice): Boolean =
        hasConnectPermission() && runCatching { device.bondState == BluetoothDevice.BOND_BONDED }.getOrDefault(false)

    /** Refresh only the system-paired list; no custom discovery is involved. */
    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        if (!hasConnectPermission()) return
        stopScan()
        mutableDevices.value = bondedDevices()
        mutableScanError.value = null
        mutableLog.value = if (mutableDevices.value.isEmpty()) "No paired devices — use Pair in system settings" else "${mutableDevices.value.size} paired device(s)"
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!hasScanPermission()) { mutableScanError.value = ERROR_PERMISSION; mutableLog.value = "BLUETOOTH_SCAN permission missing"; return }
        if (!isSupported()) { mutableScanError.value = ERROR_UNSUPPORTED; mutableLog.value = "Bluetooth unavailable"; return }
        if (!isEnabled()) { mutableScanError.value = ERROR_DISABLED; mutableLog.value = "Bluetooth disabled"; return }
        if (mutableScanning.value) return
        mutableScanError.value = null
        mutableCallbackCount.value = 0
        mutableDevices.value = bondedDevices().distinctBy { it.address }
        val bleScanner = scanner ?: run { mutableScanError.value = ERROR_SCANNER; return }
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        runCatching { bleScanner.startScan(null, settings, callback) }
            .onSuccess { mutableScanning.value = true; mutableLog.value = "Scanning BLE…" }
            .onFailure { mutableScanError.value = ERROR_START; mutableLog.value = "Scan start failed: ${it.message ?: it.javaClass.simpleName}" }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (hasScanPermission()) runCatching { scanner?.stopScan(callback) }
        mutableScanning.value = false
    }

    @SuppressLint("MissingPermission")
    fun bondedDevices(): List<AirPodsDevice> {
        if (!hasConnectPermission()) return emptyList()
        return adapter?.bondedDevices?.map { device ->
            AirPodsDevice(
                name = runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Paired device",
                address = device.address, bonded = true
            )
        }.orEmpty()
    }

    private fun hasConnectPermission() = android.os.Build.VERSION.SDK_INT < 31 || ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    private fun hasScanPermission() = android.os.Build.VERSION.SDK_INT < 31 || ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val APPLE_COMPANY_ID = 0x004C
        const val ERROR_PERMISSION = -100
        const val ERROR_UNSUPPORTED = -101
        const val ERROR_DISABLED = -102
        const val ERROR_SCANNER = -103
        const val ERROR_START = -104
    }
}
