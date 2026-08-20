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
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.wear.bluetooth.WearBluetoothScanner
import me.kavishdevar.librepods.wear.core.AirPodsController
import me.kavishdevar.librepods.wear.core.AirPodsDevice

@Composable
fun AirPodsHomeScreen(controller: AirPodsController, scanner: WearBluetoothScanner, onConnect: () -> Unit, modifier: Modifier = Modifier) {
    val state by controller.state.collectAsState()
    val devices by scanner.devices.collectAsState()
    val scanning by scanner.scanning.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
        Text("LibrePods Wear", style = MaterialTheme.typography.titleLarge)
        Text(state.deviceName)
        Text(when { state.connecting -> "Connecting…"; state.connected -> "Connected"; state.lastError != null -> state.lastError!!; else -> "Disconnected" })

        BatteryRow("Left", state.leftBattery, state.leftCharging)
        BatteryRow("Right", state.rightBattery, state.rightCharging)
        BatteryRow("Case", state.caseBattery, state.caseCharging)

        if (devices.isNotEmpty() || scanning) {
            Text(if (scanning) "Scanning…" else "Nearby devices", style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(devices, key = { it.address }) { device -> DeviceRow(device) { controller.connectToDevice(device.address, device.name) } }
            }
            Button(onClick = { if (scanning) scanner.stopScan() else scanner.startScan() }) { Text(if (scanning) "Stop scan" else "Scan again") }
        } else {
            Button(onClick = onConnect, enabled = !state.connecting && !state.connected) { Text("Scan for AirPods") }
        }

        if (state.connected) Button(onClick = controller::disconnect) { Text("Disconnect") }
    }
}

@Composable
private fun DeviceRow(device: AirPodsDevice, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column { Text(device.name); Text(if (device.bonded) "Paired • ${device.address}" else device.address, style = MaterialTheme.typography.labelSmall) }
        Text(device.rssi?.let { "${it} dBm" } ?: "")
    }
}

@Composable
private fun BatteryRow(label: String, level: Int?, charging: Boolean) {
    val value = when { level == null || level == 255 -> "--"; level in 0..100 -> "$level%"; else -> "?" }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(if (charging && value != "--") "$value  ⚡" else value) }
}
