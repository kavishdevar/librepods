package me.kavishdevar.librepods

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.bluetooth.BLEManager
import me.kavishdevar.librepods.wear.bluetooth.WearBluetoothConnection
import me.kavishdevar.librepods.wear.bluetooth.WearBluetoothScanner
import me.kavishdevar.librepods.wear.core.AirPodsController
import me.kavishdevar.librepods.wear.ui.AirPodsHomeScreen

/** Thin Wear OS entry point; discovery and connection are owned by the Wear stack. */
class MainActivity : ComponentActivity() {
    private lateinit var controller: AirPodsController
    private lateinit var scanner: WearBluetoothScanner

    private val bluetoothPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = android.os.Build.VERSION.SDK_INT < 31 || (result[Manifest.permission.BLUETOOTH_CONNECT] == true && result[Manifest.permission.BLUETOOTH_SCAN] == true)
        if (granted) scanner.startScan() else controller.onError("Bluetooth permission was denied")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transport = WearBluetoothConnection(this)
        controller = AirPodsController(this, transport).also { it.initialize(AACPManager(), BLEManager(this)) }
        scanner = WearBluetoothScanner(this)
        setContent { AirPodsHomeScreen(controller = controller, scanner = scanner, onConnect = ::requestBluetoothAndScan) }
    }

    private fun requestBluetoothAndScan() {
        if (android.os.Build.VERSION.SDK_INT < 31) { scanner.startScan(); return }
        val connectGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        val scanGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        if (connectGranted && scanGranted) scanner.startScan() else bluetoothPermissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN))
    }

    override fun onDestroy() { if (::scanner.isInitialized) scanner.stopScan(); controller.shutdown(); super.onDestroy() }
}
