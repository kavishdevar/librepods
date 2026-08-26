package me.kavishdevar.librepods.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import me.kavishdevar.librepods.wear.bluetooth.WearBluetoothScanner
import me.kavishdevar.librepods.wear.core.AirPodsController
import me.kavishdevar.librepods.wear.core.AirPodsDevice
import me.kavishdevar.librepods.wear.core.AirPodsState

/** Wear home screen: status, battery, AirPods controls and paired device list. */
@Composable
fun AirPodsHomeScreen(
    controller: AirPodsController,
    scanner: WearBluetoothScanner,
    onOpenSystemBluetooth: () -> Unit,
    onRefresh: () -> Unit,
) {
    val state by controller.state.collectAsState()
    val devices by scanner.devices.collectAsState()
    val listState = rememberScalingLazyListState()

    MaterialTheme {
        AppScaffold {
            ScreenScaffold(scrollState = listState) { contentPadding ->
            ScalingLazyColumn(
                state = listState,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxWidth(),
            ) {
                item { ListHeader { Text(state.deviceName) } }
                item { StatusText(state) }

                if (state.connected || state.leftBattery != null || state.rightBattery != null || state.caseBattery != null) {
                    item { BatteryRow(state) }
                }

                if (state.connected) {
                    item {
                        ListeningModeRow(selected = state.listeningMode) { mode ->
                            if (!controller.setListeningMode(mode)) controller.onError("Failed to set listening mode")
                        }
                    }
                    item {
                        ToggleRow("Ear detection", state.earDetectionEnabled == true) { enabled ->
                            if (!controller.setEarDetection(enabled)) controller.onError("Failed to set ear detection")
                        }
                    }
                    item {
                        ToggleRow("Conversation", state.conversationalAwarenessEnabled == true) { enabled ->
                            if (!controller.setConversationalAwareness(enabled)) controller.onError("Failed to set conversation awareness")
                        }
                    }
                    item { EarStatusText(state) }
                    item {
                        Button(onClick = { controller.disconnect() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Disconnect")
                        }
                    }
                } else {
                    item { ListHeader { Text("Paired devices") } }
                    if (devices.isEmpty()) {
                        item { Text("No paired devices", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                    }
                    deviceItems(devices, controller)
                    item {
                        Button(onClick = onOpenSystemBluetooth, modifier = Modifier.fillMaxWidth()) { Text("Pair in settings") }
                    }
                    item {
                        Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text("Refresh") }
                    }
                }
                }
            }
        }
    }
}

private fun ScalingLazyListScope.deviceItems(
    devices: List<AirPodsDevice>,
    controller: AirPodsController,
) {
    devices.forEach { device ->
        item(key = device.address) {
            Button(
                onClick = { controller.connectToDevice(device.address, device.name) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Text(device.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (device.bonded) "Paired · tap to connect" else "Nearby",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusText(state: AirPodsState) {
    val status = when {
        state.connecting -> "Connecting… (${state.protocolStage})"
        state.connected -> "Connected"
        state.lastError != null -> state.lastError
        else -> "Not connected"
    }
    Text(
        status.orEmpty(),
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun EarStatusText(state: AirPodsState) {
    if (state.leftInEar == null && state.rightInEar == null) return
    val text = "In ear: ${state.leftInEar.asEarLabel()} / ${state.rightInEar.asEarLabel()}"
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun Boolean?.asEarLabel(): String = when (this) {
    true -> "yes"
    false -> "no"
    null -> "—"
}

@Composable
private fun BatteryRow(state: AirPodsState) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        BatteryCell("L", state.leftBattery, state.leftCharging)
        BatteryCell("R", state.rightBattery, state.rightCharging)
        BatteryCell("C", state.caseBattery, state.caseCharging)
    }
}

@Composable
private fun BatteryCell(label: String, level: Int?, charging: Boolean) {
    val value = if (level != null && level in 0..100) "$level%" else "--"
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(if (charging && value != "--") "$value +" else value, style = MaterialTheme.typography.bodySmall)
    }
}
