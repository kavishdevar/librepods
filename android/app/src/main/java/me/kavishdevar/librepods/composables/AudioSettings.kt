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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.kavishdevar.librepods.R
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable
fun AudioSettings() {
    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) Color.White else Color.Black

    Text(
        text = stringResource(R.string.audio).uppercase(),
        style = TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.Light,
            color = textColor.copy(alpha = 0.6f)
        ),
        modifier = Modifier.padding(8.dp, bottom = 2.dp)
    )

    val backgroundColor = if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(14.dp))
            .padding(top = 2.dp)
    ) {

        ConversationalAwarenessSwitch()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.adaptive_audio),
                modifier = Modifier
                    .padding(end = 8.dp, bottom = 2.dp, start = 2.dp)
                    .fillMaxWidth(),
                style = TextStyle(
                    fontSize = 16.sp,
                    color = textColor
                )
            )
            Text(
                text = stringResource(R.string.adaptive_audio_description),
                modifier = Modifier
                    .padding(bottom = 8.dp, top = 2.dp)
                    .padding(end = 2.dp, start = 2.dp)
                    .fillMaxWidth(),
                style = TextStyle(
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.6f)
                )
            )

            AdaptiveStrengthSlider()
        }
    }
}

@Preview
@Composable
fun AudioSettingsPreview() {
    AudioSettings()
}
