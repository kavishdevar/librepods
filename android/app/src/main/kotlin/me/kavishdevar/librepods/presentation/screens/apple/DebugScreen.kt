package me.kavishdevar.librepods.presentation.screens.apple

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.bluetooth.aacp.packet.AACPPacketType
import me.kavishdevar.librepods.bluetooth.aacp.packet.BatteryInfoPacket
import me.kavishdevar.librepods.bluetooth.aacp.packet.ControlCommandPacket
import me.kavishdevar.librepods.bluetooth.aacp.packet.EarDetectionResponsePacket
import me.kavishdevar.librepods.bluetooth.aacp.packet.MagicKeyResponsePacket
import me.kavishdevar.librepods.bluetooth.aacp.packet.RTBuddyPacket
import me.kavishdevar.librepods.bluetooth.aacp.packet.RenamePacket
import me.kavishdevar.librepods.bluetooth.aacp.rtbuddy.proto.SensorServiceType
import me.kavishdevar.librepods.bluetooth.aacp.types.MessageOpcode
import me.kavishdevar.librepods.bluetooth.aacp.types.RTBuddyDescriptor
import me.kavishdevar.librepods.bluetooth.aacp.types.SensorDataWxBuddyPayload
import me.kavishdevar.librepods.devices.DeviceComponent
import me.kavishdevar.librepods.devices.PacketDestination
import me.kavishdevar.librepods.presentation.components.primitives.StyledButton
import me.kavishdevar.librepods.presentation.components.primitives.StyledInputField
import me.kavishdevar.librepods.presentation.components.primitives.StyledList
import me.kavishdevar.librepods.presentation.components.primitives.StyledListItem
import me.kavishdevar.librepods.presentation.components.primitives.StyledListItemOrientation
import me.kavishdevar.librepods.presentation.components.primitives.StyledScaffold
import me.kavishdevar.librepods.presentation.icons.richText
import me.kavishdevar.librepods.presentation.viewmodel.AppleUiState
import me.kavishdevar.librepods.presentation.viewmodel.AppleViewModel
import me.kavishdevar.librepods.utils.nonScaledSp

@Composable
fun DebugRoute(
    viewModel: AppleViewModel,
    navigateBack: (() -> Unit)?
) {
    val uiState by viewModel.uiState.collectAsState()

    DebugScreen(
        uiState = uiState,
        navigateBack = navigateBack,
        sendPacket = viewModel::sendRawPacket,
    )
}

@Composable
fun DebugScreen(
    uiState: AppleUiState,
    navigateBack: (() -> Unit)?,
    sendPacket: (ByteArray) -> Boolean,
) {
    val state = uiState.state

//    Log.d("DebugScreen", "Screen ${state.aacpPackets.size}")

    StyledScaffold(
        title = "debug",
        navigateBack = navigateBack
    ) { topPadding, bottomPadding ->
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.padding(top = topPadding))

            val inputState = remember { TextFieldState() }
            val focusRequester = remember { FocusRequester() }

            val success = remember { mutableStateOf(false) }
            val firstPacketSent = remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            LaunchedEffect(inputState.text) {
                val inputText = inputState.text
                if (inputText.isNotEmpty()) {
                    val hexRegex = Regex("^[0-9A-Fa-f ]+$")
                    if (!hexRegex.matches(inputText)) {
                        inputState.edit {
                            delete(inputText.length - 1, inputText.length)
                        }
                    }
                }
            }

            StyledInputField(
                inputState = inputState,
                focusRequester = focusRequester,
                placeholder = "data (in hex)",
            )

            StyledButton(
                modifier = Modifier
                    .fillMaxWidth(),
                onClick = {
                    val inputText = inputState.text
                    if (inputText.isNotEmpty()) {
                        firstPacketSent.value = true

                        val hexString = inputText.toString().replace(" ", "")
                        if (hexString.length % 2 != 0) {
                            Log.d("DebugScreen", "Invalid hex string length: ${hexString.length}")
                            success.value = false
                            return@StyledButton
                        }

                        val byteArray = hexString.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

                        Log.d("DebugScreen", "Sending packet: ${byteArray.toHexString()}")

                        success.value = sendPacket(byteArray)
                    }
                },
                enabled = inputState.text.isNotEmpty() && inputState.text.toString().replace(" ", "").length % 2 == 0 && inputState.text.matches(Regex("^[0-9A-Fa-f ]+$"))
            ) {
                val text = richText("\\icon{Send,onPrimary} Send Packet")
                Text(
                    text = text.text,
                    inlineContent = text.inlineContent,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center
                )
            }

            if (firstPacketSent.value) {
                Text(
                    text = if (success.value) "Sent" else "Failed",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (success.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            StyledList(
                modifier = Modifier
                    .weight(1f),
                scrollEnabled = true,
                title = "Packets ${state.aacpPackets.size}",
            ) {
                state.aacpPackets.reversed().forEach { packet ->
                    StyledListItem(
                        content = {
                            val text = richText(
                                if (packet.type == AACPPacketType.MESSAGE) {
                                    when (packet) {
                                        is EarDetectionResponsePacket -> {
                                            val left = packet.componentStates.find { it.component == DeviceComponent.LEFT }
                                            val right = packet.componentStates.find { it.component == DeviceComponent.RIGHT }
                                            val HEADSET = packet.componentStates.find { it.component == DeviceComponent.HEADSET }

                                            if (left != null && right != null) {
                                                "\\icon{LeftCircleFill} ${left.status.name} " +
                                                    "\\icon{RightCircleFill} ${right.status.name}"
                                            } else {
                                                HEADSET?.status?.name ?: "Unknown"
                                            }
                                        }

                                        is ControlCommandPacket -> {
                                            val controlCommand = packet.controlCommand
                                            "${controlCommand.identifier.name} - ${controlCommand.value.toHexString()}"
                                        }

                                        is BatteryInfoPacket -> {
                                            "Battery Info"
                                        }

                                        is RenamePacket -> {
                                            "Rename to ${packet.name}"
                                        }

                                        is MagicKeyResponsePacket -> {
                                            "Magic Keys (${packet.magicKeys.size})"
                                        }

                                        is RTBuddyPacket -> {
                                            val rtBuddyPayload = packet.rtBuddyPayload
                                            when (rtBuddyPayload.descriptor) {
                                                RTBuddyDescriptor.SENSOR_DATA_WX -> {
                                                    val sensorDataWxBuddyPayload =
                                                        rtBuddyPayload as SensorDataWxBuddyPayload
                                                    val data = sensorDataWxBuddyPayload.data

                                                    if (data.hasCommand()) {
                                                        when (data.command.service) {
                                                            SensorServiceType.ACTIVITY, SensorServiceType.DEVMOTION6 -> {
                                                                val payload = data.command.payload.toByteArray()

                                                                fun i16(offset: Int): Int =
                                                                    (payload[offset].toInt() and 0xFF) or
                                                                        ((payload[offset + 1].toInt() and 0xFF) shl 8)
                                                                            .let { value ->
                                                                                if (value and 0x8000 != 0) value - 0x10000 else value
                                                                            }

                                                                fun vec3(offset: Int): String =
                                                                    "(${i16(offset)}, ${i16(offset + 2)}, ${i16(offset + 4)})"

                                                                """
                                                                DEVMOTION
                                                                  v0:  ${vec3(20)}
                                                                  v1:  ${vec3(26)}
                                                                  v2:  ${vec3(32)}
                                                                  v3:  ${vec3(38)}
                                                                  v4:  ${vec3(44)}
                                                                """.trimIndent()
                                                            }

                                                            SensorServiceType.HEARTRATEv2 -> {
                                                                val payload = data.command.payload.toByteArray()

                                                                if (payload.size >= 11) {
                                                                    val heartRate = payload[1].toInt() and 0xFF

                                                                    "Possible HR: $heartRate, payload=${payload.toHexString()}"
                                                                }  else "???"
                                                            }

                                                            else -> {
                                                                "Unhandled sensor command service: ${data.command.service}"
                                                            }
                                                        }
                                                    } else "?????"
                                                }

                                                else -> {
                                                    "Unhandled descriptor: ${rtBuddyPayload.descriptor}"
                                                }
                                            }
                                        }

                                        else -> packet.opcode.toString()
                                    }
                                } else {
                                    packet.type.toString()
                                }
                            )

                            Text(
                                text = text.text,
                                inlineContent = text.inlineContent,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        supportingContent = {
                            val text = richText(
                                when (packet) {
                                    is BatteryInfoPacket -> buildString {
                                        for (battery in packet.batteries) {
                                            append("${battery.component} ${battery.status} ${battery.level}\n")
                                        }
                                    }
                                    is MagicKeyResponsePacket -> buildString {
                                        packet.magicKeys.entries.forEach { (key, value) ->
                                            append("${key.name}: ${value.toHexString()}\n")
                                        }
                                    }
                                    else -> packet.payload.toHexString()
                                }
                            )
                            Text(
                                text = text.text,
                                inlineContent = text.inlineContent,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(0.8f)
                            )
                        },
                        leadingContent = {
                            val text = richText(
                                when (packet.destination) {
                                    PacketDestination.HOST -> "\\icon{Incoming,primary}"
                                    PacketDestination.DEVICE -> "\\icon{Outgoing,tertiary}"
                                }
                            )
                            Text(
                                text = text.text,
                                inlineContent = text.inlineContent,
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 28.nonScaledSp()),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        orientation = StyledListItemOrientation.Vertical,
                        onClick = if (packet.destination == PacketDestination.DEVICE || packet.opcode == MessageOpcode.CONTROL_COMMAND) {
                            {
                                inputState.edit {
                                    delete(0, inputState.text.length)
                                    append(packet.rawPacket.toHexString())
                                }
                            }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.padding(top = bottomPadding))
        }
    }
}
