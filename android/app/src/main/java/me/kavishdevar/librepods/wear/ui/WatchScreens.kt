package me.kavishdevar.librepods.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.wear.core.ListeningMode

/** Reusable compact Wear controls bound to the AirPods controller state. */

@Composable
fun ListeningModeRow(
    selected: ListeningMode,
    onSelected: (ListeningMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ListeningMode.entries.forEach { mode ->
            if (mode == selected) {
                Button(onClick = { onSelected(mode) }) { Text(mode.shortLabel) }
            } else {
                OutlinedButton(onClick = { onSelected(mode) }) { Text(mode.shortLabel) }
            }
        }
    }
}

private val ListeningMode.shortLabel: String
    get() = when (this) {
        ListeningMode.ANC -> "ANC"
        ListeningMode.TRANSPARENCY -> "Trans"
        ListeningMode.OFF -> "Off"
    }
