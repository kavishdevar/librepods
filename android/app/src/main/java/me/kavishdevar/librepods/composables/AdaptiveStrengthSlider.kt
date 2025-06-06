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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.kavishdevar.librepods.services.ServiceManager
import me.kavishdevar.librepods.utils.AACPManager
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveStrengthSlider() {
    val sliderValue = remember { mutableFloatStateOf(0f) }
    val service = ServiceManager.getService()!!
    LaunchedEffect(sliderValue) {
        val sliderValueFromAACP = service.aacpManager.controlCommandStatusList.find {
            it.identifier == AACPManager.Companion.ControlCommandIdentifiers.AUTO_ANC_STRENGTH
        }?.value?.takeIf { it.isNotEmpty() }?.get(0)
        sliderValueFromAACP?.toFloat()?.let { sliderValue.floatValue = (100 - it) }
    }

    val isDarkTheme = isSystemInDarkTheme()

    val trackColor = if (isDarkTheme) Color(0xFFB3B3B3) else Color(0xFFD9D9D9)
    val thumbColor = if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFFFFFFFF)
    val labelTextColor = if (isDarkTheme) Color.White else Color.Black

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Slider(
            value = sliderValue.floatValue,
            onValueChange = {
                sliderValue.floatValue = it
            },
            valueRange = 0f..100f,
            onValueChangeFinished = {
                sliderValue.floatValue = sliderValue.floatValue.roundToInt().toFloat()
                service.aacpManager.sendControlCommand(
                    identifier = AACPManager.Companion.ControlCommandIdentifiers.AUTO_ANC_STRENGTH.value,
                    value = (100 - sliderValue.floatValue).toInt()
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            colors = SliderDefaults.colors(
                thumbColor = thumbColor,
                inactiveTrackColor = trackColor
            ),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .shadow(4.dp, CircleShape)
                        .background(thumbColor, CircleShape)
                )
            },
            track = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    contentAlignment = Alignment.CenterStart
                )
                {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(trackColor, RoundedCornerShape(4.dp))
                    )
                }

            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Less Noise",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = labelTextColor
                ),
                modifier = Modifier.padding(start = 4.dp)
            )
            Text(
                text = "More Noise",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = labelTextColor
                ),
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}

@Preview
@Composable
fun AdaptiveStrengthSliderPreview() {
    AdaptiveStrengthSlider()
}
