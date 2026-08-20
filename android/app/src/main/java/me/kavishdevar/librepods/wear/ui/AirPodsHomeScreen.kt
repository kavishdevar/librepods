package me.kavishdevar.librepods.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.wear.core.AirPodsController

/** First-build status screen. Protocol controls are added after transport stabilizes. */
@Composable
fun AirPodsHomeScreen(
    controller: AirPodsController,
    modifier: Modifier = Modifier,
) {
    val state by controller.state.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("LibrePods Wear", style = MaterialTheme.typography.titleLarge)
        Text(state.deviceName ?: "No AirPods selected")
        Text(
            when {
                state.connecting -> "Connecting…"
                state.connected -> "Connected"
                state.lastError != null -> state.lastError!!
                else -> "Disconnected"
            },
        )
        state.address?.let { Text(it) }

        Button(
            onClick = { controller.connectToBondedAirPods() },
            enabled = !state.connecting,
        ) {
            Text(if (state.connected) "Connected" else "Connect")
        }

        if (state.connected) {
            Button(onClick = controller::disconnect) {
                Text("Disconnect")
            }
        }
    }
}
