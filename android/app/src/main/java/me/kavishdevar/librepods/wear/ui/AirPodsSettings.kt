package me.kavishdevar.librepods.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import me.kavishdevar.librepods.bluetooth.AACPManager.Companion.ControlCommandIdentifiers
import me.kavishdevar.librepods.wear.core.AirPodsState

/**
 * Boolean AirPods settings exposed on the watch.
 *
 * All of them use the same control command encoding: `0x01` enabled,
 * `0x02` disabled, and the AirPods report the current value back with the
 * same identifier.
 */
val BooleanSettings: List<Pair<String, ControlCommandIdentifiers>> = listOf(
    "Adaptive volume" to ControlCommandIdentifiers.ADAPTIVE_VOLUME_CONFIG,
    "Volume swipe" to ControlCommandIdentifiers.VOLUME_SWIPE_MODE,
    "ANC with one bud" to ControlCommandIdentifiers.ONE_BUD_ANC_MODE,
    "Allow Off mode" to ControlCommandIdentifiers.ALLOW_OFF_OPTION,
    "Auto connect" to ControlCommandIdentifiers.AUTOMATIC_CONNECTION_CONFIG,
    "Sleep detection" to ControlCommandIdentifiers.SLEEP_DETECTION_CONFIG,
    "Hearing protection" to ControlCommandIdentifiers.PPE_TOGGLE_CONFIG,
    "Dynamic end of charge" to ControlCommandIdentifiers.DYNAMIC_END_OF_CHARGE,
)

/** A control command value is reported as enabled when its first byte is `0x01`. */
fun AirPodsState.isControlEnabled(identifier: ControlCommandIdentifiers): Boolean =
    controlValues[identifier] == 0x01

/** Adaptive strength is sent inverted: the slider percentage is `100 - rawValue`. */
fun AirPodsState.adaptiveStrengthPercent(): Int? =
    controlValues[ControlCommandIdentifiers.AUTO_ANC_STRENGTH]?.let { 100 - it.coerceIn(0, 100) }

@Composable
fun StepperRow(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    step: Int = 10,
    onValueChange: (Int) -> Unit,
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(onClick = { onValueChange((value - step).coerceIn(0, 100)) }) { Text("-") }
            Text("$value%", style = MaterialTheme.typography.bodyMedium)
            FilledTonalButton(onClick = { onValueChange((value + step).coerceIn(0, 100)) }) { Text("+") }
        }
    }
}
