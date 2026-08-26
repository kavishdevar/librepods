package me.kavishdevar.librepods.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import me.kavishdevar.librepods.wear.core.ListeningMode

/** Reusable compact Wear controls bound to the AirPods controller state. */
@Composable
fun ListeningModeRow(
    selected: ListeningMode,
    modifier: Modifier = Modifier,
    onSelected: (ListeningMode) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    ) {
        ListeningMode.entries.forEach { mode ->
            if (mode == selected) {
                FilledTonalButton(
                    onClick = { onSelected(mode) },
                    modifier = Modifier.weight(1f),
                ) { Text(mode.shortLabel, textAlign = TextAlign.Center) }
            } else {
                OutlinedButton(
                    onClick = { onSelected(mode) },
                    modifier = Modifier.weight(1f),
                ) { Text(mode.shortLabel, textAlign = TextAlign.Center) }
            }
        }
    }
}

@Composable
fun ToggleRow(
    label: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit,
) {
    SwitchButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
    )
}

/** Shown while the UI is waiting for the AirPods service or permissions. */
@Composable
fun StartupScreen(message: String, onOpenSettings: () -> Unit) {
    MaterialTheme {
        AppScaffold {
            ScreenScaffold {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) { Text("Settings") }
            }
            }
        }
    }
}

private val ListeningMode.shortLabel: String
    get() = when (this) {
        ListeningMode.ANC -> "ANC"
        ListeningMode.TRANSPARENCY -> "Trns"
        ListeningMode.OFF -> "Off"
    }
