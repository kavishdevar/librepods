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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.kavishdevar.librepods.services.ServiceManager
import me.kavishdevar.librepods.utils.AACPManager
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable
fun ConversationalAwarenessSwitch() {
    val service = ServiceManager.getService()!!
    val conversationEnabledValue = service.aacpManager.controlCommandStatusList.find {
        it.identifier == AACPManager.Companion.ControlCommandIdentifiers.CONVERSATION_DETECT_CONFIG
    }?.value?.takeIf { it.isNotEmpty() }?.get(0)
    var conversationalAwarenessEnabled by remember {
        mutableStateOf(
            conversationEnabledValue == 1.toByte()
        )
    }

    fun updateConversationalAwareness(enabled: Boolean) {
        conversationalAwarenessEnabled = enabled
        service.aacpManager.sendControlCommand(
            AACPManager.Companion.ControlCommandIdentifiers.CONVERSATION_DETECT_CONFIG.value,
            enabled
        )
    }

    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) Color.White else Color.Black

    val isPressed = remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                shape = RoundedCornerShape(14.dp),
                color = if (isPressed.value) Color(0xFFE0E0E0) else Color.Transparent
            )
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed.value = true
                        tryAwaitRelease()
                        isPressed.value = false
                    }
                )
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                updateConversationalAwareness(!conversationalAwarenessEnabled)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp)
        ) {
            Text(
                text = "Conversational Awareness",
                fontSize = 16.sp,
                color = textColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Lowers media volume and reduces background noise when you start speaking to other people.",
                fontSize = 12.sp,
                color = textColor.copy(0.6f),
                lineHeight = 14.sp,
            )
        }
        StyledSwitch(
            checked = conversationalAwarenessEnabled,
            onCheckedChange = {
                updateConversationalAwareness(it)
            },
        )
    }
}

@Preview
@Composable
fun ConversationalAwarenessSwitchPreview() {
    ConversationalAwarenessSwitch()
}
