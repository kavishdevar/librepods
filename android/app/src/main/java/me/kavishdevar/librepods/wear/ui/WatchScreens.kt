package me.kavishdevar.librepods.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Wear OS presentation skeleton for the autonomous LibrePods client.
 *
 * This layer deliberately contains no Bluetooth or protocol calls yet. It is
 * a stable UI contract that the AirPods controller can be connected to later.
 */
@Composable
fun AirPodsHomeScreen(
    deviceName: String = "AirPods",
    connected: Boolean = false,
    leftBattery: Int? = null,
    rightBattery: Int? = null,
    caseBattery: Int? = null,
    listeningMode: ListeningMode = ListeningMode.OFF,
    onConnect: () -> Unit = {},
    onListeningModeChanged: (ListeningMode) -> Unit = {},
    onOpenAdvanced: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(deviceName, style = MaterialTheme.typography.titleLarge)
        Text(
            text = if (connected) "Connected" else "Not connected",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(8.dp))

        BatteryRow(leftBattery, rightBattery, caseBattery)

        Spacer(Modifier.height(10.dp))

        ListeningModeRow(
            selected = listeningMode,
            onSelected = onListeningModeChanged,
        )

        Spacer(Modifier.height(8.dp))

        if (!connected) {
            Button(onClick = onConnect) {
                Text("Connect")
            }
        } else {
            OutlinedButton(onClick = onOpenAdvanced) {
                Text("More")
            }
        }
    }
}

@Composable
private fun BatteryRow(left: Int?, right: Int?, case: Int?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        BatteryValue("L", left)
        BatteryValue("R", right)
        BatteryValue("Case", case)
    }
}

@Composable
private fun BatteryValue(label: String, value: Int?) {
    Text(
        text = if (value == null) "$label --" else "$label $value%",
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
private fun ListeningModeRow(
    selected: ListeningMode,
    onSelected: (ListeningMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ListeningMode.entries.forEach { mode ->
            if (mode == selected) {
                Button(onClick = { onSelected(mode) }) {
                    Text(mode.shortLabel)
                }
            } else {
                OutlinedButton(onClick = { onSelected(mode) }) {
                    Text(mode.shortLabel)
                }
            }
        }
    }
}

enum class ListeningMode(val shortLabel: String) {
    ANC("ANC"),
    TRANSPARENCY("Trans"),
    OFF("Off"),
}
