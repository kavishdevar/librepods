package me.kavishdevar.librepods

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import me.kavishdevar.librepods.wear.service.LibrePodsWearService
import me.kavishdevar.librepods.wear.ui.AirPodsHomeScreen
import me.kavishdevar.librepods.wear.ui.StartupScreen

/**
 * Wear OS entry point.
 *
 * The activity owns no protocol state: it binds to [LibrePodsWearService],
 * which keeps the AirPods session alive independently of the UI. Pairing is
 * delegated to the system Bluetooth UI.
 */
class MainActivity : ComponentActivity() {
    private var service by mutableStateOf<LibrePodsWearService?>(null)
    private var permissionDenied by mutableStateOf(false)
    private var bound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as? LibrePodsWearService.LocalBinder)?.service
            refreshDevices()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionDenied = requiredPermissions().any { result[it] == false }
        if (!permissionDenied) startAndBindService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasAllPermissions()) startAndBindService() else permissionLauncher.launch(requiredPermissions())

        setContent {
            val active = service
            if (active == null) {
                StartupScreen(
                    message = if (permissionDenied) "Bluetooth permission is required" else "Starting…",
                    onOpenSettings = ::openSystemBluetoothSettings,
                )
            } else {
                AirPodsHomeScreen(
                    controller = active.controller,
                    scanner = active.scanner,
                    onOpenSystemBluetooth = ::openSystemBluetoothSettings,
                    onRefresh = ::refreshDevices,
                )
            }
        }
    }

    private fun startAndBindService() {
        LibrePodsWearService.start(this)
        if (!bound) {
            bound = bindService(
                Intent(this, LibrePodsWearService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE,
            )
        }
    }

    private fun requiredPermissions(): Array<String> = buildList {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (android.os.Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    private fun hasAllPermissions(): Boolean = requiredPermissions().all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    /** Open the actual Wear OS Bluetooth pairing/settings screen. */
    private fun openSystemBluetoothSettings() {
        runCatching { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
            .onFailure { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }

    private fun refreshDevices() {
        if (android.os.Build.VERSION.SDK_INT < 31 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        ) service?.scanner?.refreshPairedDevices()
    }

    override fun onResume() {
        super.onResume()
        refreshDevices()
    }

    override fun onDestroy() {
        if (bound) {
            unbindService(serviceConnection)
            bound = false
        }
        super.onDestroy()
    }
}
