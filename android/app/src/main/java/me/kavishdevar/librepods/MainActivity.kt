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
import me.kavishdevar.librepods.wear.core.AirPodsController
import me.kavishdevar.librepods.wear.ui.AirPodsHomeScreen

/** Thin Wear OS entry point; Bluetooth resources are owned by the controller. */
class MainActivity : ComponentActivity() {
    private lateinit var controller: AirPodsController

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.BLUETOOTH_CONNECT] == true &&
            result[Manifest.permission.BLUETOOTH_SCAN] == true
        if (granted) {
            controller.connectToBondedAirPods()
        } else {
            controller.onError("Bluetooth permission was denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val transport = WearBluetoothConnection(this)
        controller = AirPodsController(this, transport).also {
            it.initialize(AACPManager(), BLEManager(this))
        }

        setContent {
            AirPodsHomeScreen(
                controller = controller,
                onConnect = ::requestBluetoothAndConnect,
            )
        }
    }

    private fun requestBluetoothAndConnect() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
            controller.connectToBondedAirPods()
            return
        }

        val connectGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
        val scanGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_SCAN,
        ) == PackageManager.PERMISSION_GRANTED

        if (connectGranted && scanGranted) {
            controller.connectToBondedAirPods()
        } else {
            bluetoothPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                ),
            )
        }
    }

    override fun onDestroy() {
        controller.shutdown()
        super.onDestroy()
    }
}
