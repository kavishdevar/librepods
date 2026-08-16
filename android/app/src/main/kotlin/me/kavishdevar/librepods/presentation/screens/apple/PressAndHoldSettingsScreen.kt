/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

@file:OptIn(ExperimentalStdlibApi::class, ExperimentalEncodingApi::class)

package me.kavishdevar.librepods.presentation.screens.apple

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.aacp.types.ControlCommandIdentifier
import me.kavishdevar.librepods.data.StemAction
import me.kavishdevar.librepods.presentation.components.primitives.StyledButton
import me.kavishdevar.librepods.presentation.components.primitives.StyledList
import me.kavishdevar.librepods.presentation.components.primitives.StyledListItem
import me.kavishdevar.librepods.presentation.components.primitives.StyledListItemOrientation
import me.kavishdevar.librepods.presentation.components.primitives.StyledScaffold
import me.kavishdevar.librepods.presentation.viewmodel.AppleViewModel
import kotlin.experimental.and
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable
fun LongPress(
    viewModel: AppleViewModel,
    name: String,
    navigateBack: (() -> Unit)?,
    navigateToPurchase: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val state = uiState.state
    val settings = uiState.settings

    val modesByte = state.controlStates[ControlCommandIdentifier.LISTENING_MODE_CONFIGS]?.get(0) ?: 0

    Log.d("PressAndHoldSettingsScreen", "Current modes state: ${modesByte.toString(2)}")
    Log.d("PressAndHoldSettingsScreen", "Off mode: ${(modesByte and 0x01) != 0.toByte()}")
    Log.d("PressAndHoldSettingsScreen", "Transparency mode: ${(modesByte and 0x04) != 0.toByte()}")
    Log.d("PressAndHoldSettingsScreen", "Noise Cancellation mode: ${(modesByte and 0x02) != 0.toByte()}")
    Log.d("PressAndHoldSettingsScreen", "Adaptive mode: ${(modesByte and 0x08) != 0.toByte()}")

    val longPressAction = if (name == stringResource(R.string.left)) settings.leftLongPressAction else settings.rightLongPressAction

    val scrollState = rememberScrollState()

    // ik enum would take like 1 minute to implement, idc
    val side = if (name == stringResource(R.string.left)) "left" else "right"

    StyledScaffold(
        title = name,
        navigateBack = navigateBack
    ) { topPadding, bottomPadding ->
        Column (
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .verticalScroll(scrollState)
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(topPadding))

            StyledList {
                StyledListItem(
                    contentText = stringResource(R.string.noise_control),
                    selected = longPressAction == StemAction.CYCLE_NOISE_CONTROL_MODES,
                    onClick = {
                        viewModel.setLongPressAction(
                            side,
                            StemAction.CYCLE_NOISE_CONTROL_MODES
                        )
                    }
                )

                StyledListItem(
                    contentText = stringResource(R.string.digital_assistant),
                    selected = longPressAction == StemAction.DIGITAL_ASSISTANT,
                    onClick = {
                        viewModel.setLongPressAction(
                            side,
                            StemAction.DIGITAL_ASSISTANT
                        )
                    },
                    enabled = uiState.isPremium
                )
            }

            if (!uiState.isPremium) {
                Spacer(modifier = Modifier.height(24.dp))
                StyledButton(
                    onClick = navigateToPurchase,
                    backdrop = rememberLayerBackdrop(),
                    modifier = Modifier.fillMaxWidth(),
                    maxScale = 0.05f,
                    surfaceColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        stringResource(R.string.unlock_advanced_features),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (longPressAction == StemAction.CYCLE_NOISE_CONTROL_MODES) {
                Spacer(modifier = Modifier.height(32.dp))

                val currentByte = state.controlStates[ControlCommandIdentifier.LISTENING_MODE_CONFIGS]?.get(0)?.toInt() ?: 0

                StyledList(
                    title = stringResource(R.string.noise_control),
                    description = stringResource(R.string.press_and_hold_noise_control_description)
                ) {
                    if (state.controlStates[ControlCommandIdentifier.ALLOW_OFF_OPTION]?.get(0) == 1.toByte()) {
                        StyledListItem(
                            contentText = stringResource(R.string.off),
                            supportingText = stringResource(R.string.listening_mode_off_description),
                            selected = (currentByte and 0x01) != 0,
                            onClick = {
                                viewModel.toggleListeningMode(0x01)
                            },
                            orientation = StyledListItemOrientation.Vertical,
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_noise_cancellation),
                                    contentDescription = "Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .height(42.dp)
                                        .wrapContentWidth()
                                )
                            }
                        )
                    }

                    StyledListItem(
                        contentText = stringResource(R.string.transparency),
                        supportingText = stringResource(R.string.listening_mode_transparency_description),
                        selected = (currentByte and 0x04) != 0,
                        onClick = {
                            viewModel.toggleListeningMode(0x04)
                        },
                        orientation = StyledListItemOrientation.Vertical,
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_transparency),
                                contentDescription = "Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .height(42.dp)
                                    .wrapContentWidth()
                            )
                        }
                    )

                    StyledListItem(
                        contentText = stringResource(R.string.adaptive),
                        supportingText = stringResource(R.string.listening_mode_adaptive_description),
                        selected = (currentByte and 0x08) != 0,
                        onClick = {
                            viewModel.toggleListeningMode(0x08)
                        },
                        orientation = StyledListItemOrientation.Vertical,
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_adaptive),
                                contentDescription = "Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .height(42.dp)
                                    .wrapContentWidth()
                            )
                        }
                    )

                    StyledListItem(
                        contentText = stringResource(R.string.noise_cancellation),
                        supportingText = stringResource(R.string.listening_mode_noise_cancellation_description),
                        selected = (currentByte and 0x02) != 0,
                        onClick = {
                            viewModel.toggleListeningMode(0x02)
                        },
                        orientation = StyledListItemOrientation.Vertical,
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_noise_cancellation),
                                contentDescription = "Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .height(42.dp)
                                    .wrapContentWidth()
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
}
