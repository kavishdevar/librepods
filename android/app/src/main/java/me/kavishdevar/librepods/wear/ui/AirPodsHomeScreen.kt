package me.kavishdevar.librepods.wear.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.wear.bluetooth.WearBluetoothScanner
import me.kavishdevar.librepods.wear.core.AirPodsController
import me.kavishdevar.librepods.wear.core.AirPodsDevice

/** Compact Wear UI: system pairing first, LibrePods protocol second. */
@Composable
fun AirPodsHomeScreen(
    controller: AirPodsController,
    scanner: WearBluetoothScanner,
    onOpenSystemBluetooth: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by controller.state.collectAsState()
    val devices by scanner.devices.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("LibrePods", style = MaterialTheme.typography.titleSmall)
        Text(
            when {
                state.connecting -> "Connecting…"
                state.connected -> "Connected"
                state.lastError != null -> state.lastError!!
                else -> "System Bluetooth"
            },
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (state.connected || state.leftBattery != null || state.rightBattery != null || state.caseBattery != null) {
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), Arrangement.SpaceEvenly) {
                BatteryRow("L", state.leftBattery, state.leftCharging)
                BatteryRow("R", state.rightBattery, state.rightCharging)
                BatteryRow("C", state.caseBattery, state.caseCharging)
            }
        }

        if (state.connected) {
            ListeningModeRow(selected = state.listeningMode, onSelected = { mode ->
                if (!controller.setListeningMode(mode)) controller.onError("Failed to set listening mode")
            })
            Row(
                Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
            ) {
                ToggleChip("Ear", state.earDetectionEnabled == true) { enabled ->
                    if (!controller.setEarDetection(enabled)) controller.onError("Failed to set ear detection")
                }
                ToggleChip("Conv", state.conversationalAwarenessEnabled == true) { enabled ->
                    if (!controller.setConversationalAwareness(enabled)) controller.onError("Failed to set conversation awareness")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            if (devices.isEmpty()) {
                item { Text("No paired AirPods", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(4.dp)) }
            }
            items(devices, key = { it.address }) { device ->
                DeviceRow(device) { controller.connectToDevice(device.address, device.name) }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
        ) {
            Button(onClick = onOpenSystemBluetooth) { Text("Pair") }
            Button(onClick = onRefresh) { Text("Refresh") }
        }
    }
}

@Composable
private fun DeviceRow(device: AirPodsDevice, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 3.dp),
        Arrangement.SpaceBetween,
        Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(if (device.appleManufacturer) " ${device.name}" else device.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (device.bonded) "Paired · tap to connect" else "BLE", style = MaterialTheme.typography.labelSmall)
        }
        Text(device.rssi?.let { "${it} dBm" } ?: "—", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun BatteryRow(label: String, level: Int?, charging: Boolean) {
    val value = if (level in 0..100) "$level%" else "--"
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(if (charging && value != "--") "$value ⚡" else value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ToggleChip(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    if (checked) {
        Button(onClick = { onCheckedChange(false) }) { Text("$label ON") }
    } else {
        OutlinedButton(onClick = { onCheckedChange(true) }) { Text("$label OFF") }
    }
}
