package me.kavishdevar.librepods

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import me.kavishdevar.librepods.wear.ui.AirPodsHomeScreen

/**
 * Wear OS entry point. UI is intentionally decoupled from the Bluetooth
 * protocol layer so the controller can be integrated without another rewrite.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AirPodsHomeScreen()
        }
    }
}
