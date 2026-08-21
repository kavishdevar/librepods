package me.kavishdevar.librepods

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
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

/** Wear OS entry point; pairing is delegated to the system Bluetooth UI. */
class MainActivity : ComponentActivity() {
    private lateinit var controller: AirPodsController
    private lateinit var scanner: WearBluetoothScanner

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (android.os.Build.VERSION.SDK_INT >= 31 &&
            (result[Manifest.permission.BLUETOOTH_CONNECT] != true ||
                result[Manifest.permission.BLUETOOTH_SCAN] != true)
        ) controller.onError("Bluetooth permission was denied")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transport = WearBluetoothConnection(this)
        controller = AirPodsController(this, transport).also {
            it.initialize(AACPManager(), BLEManager(this))
        }
        scanner = WearBluetoothScanner(this)
        requestBluetoothPermissionIfNeeded()

        setContent {
            AirPodsHomeScreen(
                controller = controller,
                scanner = scanner,
                onOpenSystemBluetooth = ::openSystemBluetoothSettings,
                onRefresh = ::refreshDevices
            )
        }
    }

    private fun requestBluetoothPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT < 31) return
        val connect = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        val scan = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        if (!connect || !scan) bluetoothPermissionLauncher.launch(
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        )
    }

    /** Open the actual Wear OS Bluetooth pairing/settings screen. */
    private fun openSystemBluetoothSettings() {
        runCatching { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
            .onFailure { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }

    private fun refreshDevices() {
        if (android.os.Build.VERSION.SDK_INT < 31 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        ) scanner.refreshPairedDevices()
    }

    override fun onResume() {
        super.onResume()
        if (::scanner.isInitialized) refreshDevices()
    }

    override fun onDestroy() {
        if (::scanner.isInitialized) scanner.stopScan()
        if (::controller.isInitialized) controller.shutdown()
        super.onDestroy()
    }
}
