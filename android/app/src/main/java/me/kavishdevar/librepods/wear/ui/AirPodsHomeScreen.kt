package me.kavishdevar.librepods.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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

/**
 * First useful diagnostic screen: connection state plus the three battery
 * components. Unknown values stay visible as "--" so protocol problems are
 * distinguishable from a real zero-percent reading.
 */
@Composable
fun AirPodsHomeScreen(
    controller: AirPodsController,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by controller.state.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Text("LibrePods Wear", style = MaterialTheme.typography.titleLarge)
        Text(state.deviceName)
        Text(
            when {
                state.connecting -> "Connecting…"
                state.connected -> "Connected"
                state.lastError != null -> state.lastError!!
                else -> "Disconnected"
            },
        )

        BatteryRow("Left", state.leftBattery, state.leftCharging)
        BatteryRow("Right", state.rightBattery, state.rightCharging)
        BatteryRow("Case", state.caseBattery, state.caseCharging)

        state.address?.let { Text(it, style = MaterialTheme.typography.labelSmall) }

        Button(
            onClick = onConnect,
            enabled = !state.connecting && !state.connected,
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

@Composable
private fun BatteryRow(label: String, level: Int?, charging: Boolean) {
    val value = when {
        level == null -> "--"
        level == 255 -> "--"
        level in 0..100 -> "$level%"
        else -> "?"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Text(if (charging && value != "--") "$value  ⚡" else value)
    }
}
