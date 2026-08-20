package me.kavishdevar.librepods

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.bluetooth.BLEManager
import me.kavishdevar.librepods.wear.bluetooth.WearBluetoothConnection
import me.kavishdevar.librepods.wear.core.AirPodsController
import me.kavishdevar.librepods.wear.ui.AirPodsHomeScreen

/** Thin Wear OS entry point; Bluetooth resources are owned by the controller. */
class MainActivity : ComponentActivity() {
    private lateinit var controller: AirPodsController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val transport = WearBluetoothConnection(this)
        controller = AirPodsController(this, transport).also {
            it.initialize(AACPManager(), BLEManager(this))
        }

        setContent {
            AirPodsHomeScreen(controller)
        }
    }

    override fun onDestroy() {
        controller.shutdown()
        super.onDestroy()
    }
}
