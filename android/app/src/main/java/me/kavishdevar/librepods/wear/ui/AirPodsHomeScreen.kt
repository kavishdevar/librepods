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

/** Compact diagnostic-first Wear screen; protocol controls are added after discovery is reliable. */
@Composable
fun AirPodsHomeScreen(
    controller: AirPodsController,
    scanner: WearBluetoothScanner,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by controller.state.collectAsState()
    val devices by scanner.devices.collectAsState()
    val scanning by scanner.scanning.collectAsState()
    val scanError by scanner.scanError.collectAsState()
    val callbackCount by scanner.callbackCount.collectAsState()
    val scanLog by scanner.log.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
    ) {
        Text("LibrePods", style = MaterialTheme.typography.titleSmall)
        Text(
            when {
                state.connecting -> "Connecting…"
                state.connected -> "Connected"
                state.lastError != null -> state.lastError!!
                scanning -> "Scanning…"
                else -> "Ready"
            },
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (state.connected || state.leftBattery != null || state.rightBattery != null || state.caseBattery != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                BatteryRow("L", state.leftBattery, state.leftCharging)
                BatteryRow("R", state.rightBattery, state.rightCharging)
                BatteryRow("C", state.caseBattery, state.caseCharging)
            }
        }

        Text(
            if (scanning) "BLE ${devices.size} · callbacks $callbackCount" else scanLog,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (scanError != null) {
            Text("Scan error ${scanError}", style = MaterialTheme.typography.labelSmall)
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(devices, key = { it.address }) { device ->
                DeviceRow(device) { controller.connectToDevice(device.address, device.name) }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { if (scanning) scanner.stopScan() else onConnect() },
                enabled = !state.connecting
            ) {
                Text(if (scanning) "Stop" else "Scan")
            }
            if (state.connected) {
                Button(onClick = controller::disconnect, enabled = true) { Text("Disconnect") }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: AirPodsDevice, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (device.appleManufacturer) " ${device.name}" else device.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                buildString {
                    append(if (device.bonded) "Paired" else "BLE")
                    if (device.serviceUuids.isNotEmpty()) append(" · ${device.serviceUuids.size} svc")
                },
                style = MaterialTheme.typography.labelSmall
            )
        }
        Text(device.rssi?.let { "${it} dBm" } ?: "—", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun BatteryRow(label: String, level: Int?, charging: Boolean) {
    val value = when {
        level == null || level == 255 -> "--"
        level in 0..100 -> "$level%"
        else -> "?"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(if (charging && value != "--") "$value ⚡" else value, style = MaterialTheme.typography.bodySmall)
    }
}
