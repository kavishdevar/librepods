/*
 * LibrePods - AirPods liberated from Apple’s ecosystem
 *
 * Copyright (C) 2025 LibrePods contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

@file:OptIn(ExperimentalEncodingApi::class)

package me.kavishdevar.librepods.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.services.ServiceManager
import me.kavishdevar.librepods.utils.AACPManager
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable
fun AccessibilitySettings() {
    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val backgroundColor = if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val service = ServiceManager.getService()!!
    Text(
        text = stringResource(R.string.accessibility).uppercase(),
        style = TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.Light,
            color = textColor.copy(alpha = 0.6f)
        ),
        modifier = Modifier.padding(8.dp, bottom = 2.dp)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(14.dp))
            .padding(top = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = stringResource(R.string.tone_volume),
                modifier = Modifier
                    .padding(end = 8.dp, bottom = 2.dp, start = 2.dp)
                    .fillMaxWidth(),
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            )

            ToneVolumeSlider()
        }

        val pressSpeedOptions = mapOf(
            0.toByte() to "Default",
            1.toByte() to "Slower",
            2.toByte() to "Slowest"
        )
        val selectedPressSpeedValue = service.aacpManager.controlCommandStatusList.find { it.identifier == AACPManager.Companion.ControlCommandIdentifiers.DOUBLE_CLICK_INTERVAL }?.value?.takeIf { it.isNotEmpty() }?.get(0)
        var selectedPressSpeed by remember { mutableStateOf(pressSpeedOptions[selectedPressSpeedValue] ?: pressSpeedOptions[0]) }
        DropdownMenuComponent(
            label = "Press Speed",
            options = pressSpeedOptions.values.toList(),
            selectedOption = selectedPressSpeed.toString(),
            onOptionSelected = { newValue ->
                selectedPressSpeed = newValue
                service.aacpManager.sendControlCommand(
                    identifier = AACPManager.Companion.ControlCommandIdentifiers.DOUBLE_CLICK_INTERVAL.value,
                    value = pressSpeedOptions.filterValues { it == newValue }.keys.firstOrNull() ?: 0.toByte()
                )
            },
            textColor = textColor
        )

        val pressAndHoldDurationOptions = mapOf(
            0.toByte() to "Default",
            1.toByte() to "Slower",
            2.toByte() to "Slowest"
        )

        val selectedPressAndHoldDurationValue = service.aacpManager.controlCommandStatusList.find { it.identifier == AACPManager.Companion.ControlCommandIdentifiers.CLICK_HOLD_INTERVAL }?.value?.takeIf { it.isNotEmpty() }?.get(0)
        var selectedPressAndHoldDuration by remember { mutableStateOf(pressAndHoldDurationOptions[selectedPressAndHoldDurationValue] ?: pressAndHoldDurationOptions[0]) }
        DropdownMenuComponent(
            label = "Press and Hold Duration",
            options = pressAndHoldDurationOptions.values.toList(),
            selectedOption = selectedPressAndHoldDuration.toString(),
            onOptionSelected = { newValue ->
                selectedPressAndHoldDuration = newValue
                service.aacpManager.sendControlCommand(
                    identifier = AACPManager.Companion.ControlCommandIdentifiers.CLICK_HOLD_INTERVAL.value,
                    value = pressAndHoldDurationOptions.filterValues { it == newValue }.keys.firstOrNull() ?: 0.toByte()
                )
            },
            textColor = textColor
        )

        val volumeSwipeSpeedOptions = mapOf<Byte, String>(
            1.toByte() to "Default",
            2.toByte() to "Longer",
            3.toByte() to "Longest"
        )
        val selectedVolumeSwipeSpeedValue = service.aacpManager.controlCommandStatusList.find { it.identifier == AACPManager.Companion.ControlCommandIdentifiers.VOLUME_SWIPE_INTERVAL }?.value?.takeIf { it.isNotEmpty() }?.get(0)
        var selectedVolumeSwipeSpeed by remember { mutableStateOf(volumeSwipeSpeedOptions[selectedVolumeSwipeSpeedValue] ?: volumeSwipeSpeedOptions[1]) }
        DropdownMenuComponent(
            label = "Volume Swipe Speed",
            options = volumeSwipeSpeedOptions.values.toList(),
            selectedOption = selectedVolumeSwipeSpeed.toString(),
            onOptionSelected = { newValue ->
                selectedVolumeSwipeSpeed = newValue
                service.aacpManager.sendControlCommand(
                    identifier = AACPManager.Companion.ControlCommandIdentifiers.VOLUME_SWIPE_INTERVAL.value,
                    value = volumeSwipeSpeedOptions.filterValues { it == newValue }.keys.firstOrNull() ?: 1.toByte()
                )
            },
            textColor = textColor
        )

        SinglePodANCSwitch()
        VolumeControlSwitch()
    }
}

@Composable
fun DropdownMenuComponent(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    textColor: Color
) {
    var expanded by remember { mutableStateOf(false) }

    Column (
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(8.dp)
        ) {
            Text(
                text = selectedOption,
                modifier = Modifier.padding(16.dp),
                color = textColor
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                    text = { Text(text = option) }
                )
            }
        }
    }
}

@Preview
@Composable
fun AccessibilitySettingsPreview() {
    AccessibilitySettings()
}
