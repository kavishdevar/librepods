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

package me.kavishdevar.librepods.presentation.screens.apple

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.flow.debounce
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.aacp.types.ControlCommandIdentifier
import me.kavishdevar.librepods.bluetooth.att.ATTHandle
import me.kavishdevar.librepods.devices.AirPodsSpecs
import me.kavishdevar.librepods.devices.BaseCapability
import me.kavishdevar.librepods.presentation.components.primitives.StyledButton
import me.kavishdevar.librepods.presentation.components.primitives.StyledList
import me.kavishdevar.librepods.presentation.components.primitives.StyledListItem
import me.kavishdevar.librepods.presentation.components.primitives.StyledScaffold
import me.kavishdevar.librepods.presentation.components.primitives.StyledSlider
import me.kavishdevar.librepods.presentation.components.primitives.StyledToggle
import me.kavishdevar.librepods.presentation.icons.LocalIcons
import me.kavishdevar.librepods.presentation.viewmodel.AppleViewModel
import kotlin.time.Duration.Companion.milliseconds

//private var phoneMediaDebounceJob: Job? = null

@Composable
fun AccessibilitySettingsScreen(
    viewModel: AppleViewModel,
    navigateBack: (() -> Unit)?,
    navigateToPurchase: () -> Unit,
    navigateToTransparencyCustomization: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val state = uiState.state
    val metadata = uiState.metadata

    val hearingAidEnabled =
        state.controlStates[ControlCommandIdentifier.HEARING_AID]?.getOrNull(
            1
        )
            ?.toInt() == 1 && state.controlStates[ControlCommandIdentifier.HEARING_AID]?.getOrNull(
            0
        )?.toInt() == 1


    StyledScaffold(
        title = stringResource(R.string.accessibility),
        navigateBack = navigateBack
    ) { topPadding, bottomPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.height(topPadding))

            if (!uiState.isPremium) {
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

//            val phoneMediaEQ = remember { mutableStateOf(FloatArray(8) { 0.5f }) }
//            val phoneEQEnabled = remember { mutableStateOf(false) }
//            val mediaEQEnabled = remember { mutableStateOf(false) }

            val pressSpeedOptions = mapOf(
                0.toByte() to stringResource(R.string.default_option),
                1.toByte() to stringResource(R.string.slower),
                2.toByte() to stringResource(R.string.slowest)
            )

            val selectedPressSpeedValue =
                state.controlStates[ControlCommandIdentifier.DOUBLE_CLICK_INTERVAL]?.getOrNull(
                    0
                )
            var selectedPressSpeed by remember {
                mutableStateOf(
                    pressSpeedOptions[selectedPressSpeedValue] ?: pressSpeedOptions[0]
                )
            }

            val pressAndHoldDurationOptions = mapOf(
                0.toByte() to stringResource(R.string.default_option),
                1.toByte() to stringResource(R.string.slower),
                2.toByte() to stringResource(R.string.slowest)
            )

            val selectedPressAndHoldDurationValue =
                state.controlStates[ControlCommandIdentifier.CLICK_HOLD_INTERVAL]?.getOrNull(
                    0
                )
            var selectedPressAndHoldDuration by remember {
                mutableStateOf(
                    pressAndHoldDurationOptions[selectedPressAndHoldDurationValue]
                        ?: pressAndHoldDurationOptions[0]
                )
            }

            val volumeSwipeSpeedOptions = mapOf(
                1.toByte() to stringResource(R.string.default_option),
                2.toByte() to stringResource(R.string.longer),
                3.toByte() to stringResource(R.string.longest)
            )
            val selectedVolumeSwipeSpeedValue =
                state.controlStates[ControlCommandIdentifier.VOLUME_SWIPE_INTERVAL]?.getOrNull(
                    0
                )
            var selectedVolumeSwipeSpeed by remember {
                mutableStateOf(
                    volumeSwipeSpeedOptions[selectedVolumeSwipeSpeedValue]
                        ?: volumeSwipeSpeedOptions[1]
                )
            }

//        val phoneMediaEQ = remember { mutableStateOf(FloatArray(8) { 0.5f }) }
//        val phoneEQEnabled = remember { mutableStateOf(false) }
//        val mediaEQEnabled = remember { mutableStateOf(false) }
//
//        LaunchedEffect(phoneMediaEQ.value, phoneEQEnabled.value, mediaEQEnabled.value) {
//            phoneMediaDebounceJob?.cancel()
//            phoneMediaDebounceJob = CoroutineScope(Dispatchers.IO).launch {
//                delay(150.milliseconds)
//                try {
//                    val phoneByte = if (phoneEQEnabled.value) 0x01.toByte() else 0x02.toByte()
//                    val mediaByte = if (mediaEQEnabled.value) 0x01.toByte() else 0x02.toByte()
//                    Log.d(
//                        "AccessibilitySettingsScreen",
//                        "Sending phone/media EQ (phoneEnabled=${phoneEQEnabled.value}, mediaEnabled=${mediaEQEnabled.value})"
//                    )
//                    viewModel.sendPhoneMediaEQ(phoneMediaEQ.value, phoneByte, mediaByte)
//                } catch (e: Exception) {
//                    Log.w(
//                        "AccessibilitySettingsScreen",
//                        "Error sending phone/media EQ: ${e.message}"
//                    )
//                }
//            }
//        }

            StyledList(
                title = stringResource(R.string.press_speed),
                description = stringResource(R.string.press_speed_description)
            ) {
                pressSpeedOptions.forEach { (value, label) ->
                    StyledListItem(
                        contentText = label,
                        selected = selectedPressSpeed == label,
                        onClick = {
                            selectedPressSpeed = label

                            viewModel.setControlCommand(
                                identifier = ControlCommandIdentifier.DOUBLE_CLICK_INTERVAL,
                                value = value
                            )
                        }
                    )
                }
            }

            StyledList(
                title = stringResource(R.string.press_and_hold_duration),
                description = stringResource(R.string.press_and_hold_duration_description)
            ) {
                pressAndHoldDurationOptions.forEach { (value, label) ->
                    StyledListItem(
                        contentText = label,
                        selected = selectedPressAndHoldDuration == label,
                        onClick = {
                            selectedPressAndHoldDuration = label

                            viewModel.setControlCommand(
                                identifier = ControlCommandIdentifier.CLICK_HOLD_INTERVAL,
                                value = value
                            )
                        }
                    )
                }
            }

            StyledToggle(
                title = stringResource(R.string.noise_control),
                label = stringResource(R.string.noise_cancellation_single_airpod),
                description = stringResource(R.string.noise_cancellation_single_airpod_description),
                checked = state.controlStates[ControlCommandIdentifier.ONE_BUD_ANC_MODE]?.getOrNull(
                    0
                ) == 0x01.toByte(),
                onCheckedChange = {
                    viewModel.setControlCommand(
                        ControlCommandIdentifier.ONE_BUD_ANC_MODE, it
                    )
                },
                enabled = uiState.isPremium
            )

            if (AirPodsSpecs.getSpec(metadata.model).baseCapabilities.contains(BaseCapability.LOUD_SOUND_REDUCTION) && uiState.vendorIdHook) {
                StyledToggle(
                    label = stringResource(R.string.loud_sound_reduction),
                    description = stringResource(R.string.loud_sound_reduction_description),
                    checked = state.loudSoundReductionEnabled,
                    onCheckedChange = {
                        viewModel.writeATTCharacteristic(
                            ATTHandle.LOUD_SOUND_REDUCTION,
                            if (it) byteArrayOf(0x01) else byteArrayOf(0x00)
                        )
                    },
                    enabled = uiState.isPremium
                )
            }

            if (!hearingAidEnabled && uiState.vendorIdHook) {
                StyledListItem(
                    contentText = stringResource(R.string.customize_transparency_mode),
                    onClick = navigateToTransparencyCustomization,
                    enabled = uiState.isPremium
                )
            }

            val toneVolumeValue = remember { mutableFloatStateOf(state.controlStates[ControlCommandIdentifier.CHIME_VOLUME]?.getOrNull(0)?.toFloat() ?: 75f) }

            LaunchedEffect(toneVolumeValue) {
                snapshotFlow {
                    toneVolumeValue.floatValue
                }
                    .debounce(100.milliseconds)
                    .collect {
                        viewModel.setControlCommand(
                            ControlCommandIdentifier.CHIME_VOLUME,
                            byteArrayOf(it.toInt().toByte(), 0x50)
                        )
                    }
            }

            StyledSlider(
                label = stringResource(R.string.tone_volume),
                description = stringResource(R.string.tone_volume_description),
                value = toneVolumeValue.floatValue,
                onValueChange = {
                    toneVolumeValue.floatValue = it
                },
                valueRange = 0f..100f,
                snapPoints = listOf(75f),
                startImageVector = LocalIcons.current.SpeakerMin,
                endImageVector = LocalIcons.current.SpeakerMax,
                independent = true,
                enabled = uiState.isPremium
            )

            if (AirPodsSpecs.getSpec(metadata.model).baseCapabilities.contains(BaseCapability.SWIPE_FOR_VOLUME)) {
                val volumeSwipeEnabled =
                    state.controlStates[ControlCommandIdentifier.VOLUME_SWIPE_MODE]?.getOrNull(
                        0
                    )?.toInt() == 0x01
                StyledToggle(
                    label = stringResource(R.string.volume_control),
                    description = stringResource(R.string.volume_control_description),
                    checked = volumeSwipeEnabled,
                    onCheckedChange = {
                        viewModel.setControlCommand(
                            ControlCommandIdentifier.VOLUME_SWIPE_MODE, it
                        )
                    },
                    enabled = uiState.isPremium
                )

                StyledList(
                    title = stringResource(R.string.volume_swipe_speed),
                    description = stringResource(R.string.volume_swipe_speed_description)
                ) {
                    volumeSwipeSpeedOptions.forEach { (value, label) ->
                        StyledListItem(
                            contentText = label,
                            selected = selectedVolumeSwipeSpeed == label,
                            onClick = {
                                selectedVolumeSwipeSpeed = label

                                viewModel.setControlCommand(
                                    identifier = ControlCommandIdentifier.VOLUME_SWIPE_INTERVAL,
                                    value = value
                                )
                            }
                        )
                    }
                }
            }

//            if (!hearingAidEnabled && XposedState.isAvailable) {
//                Text(
//                    text = stringResource(R.string.apply_eq_to), style = TextStyle(
//                        fontSize = 14.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = textColor.copy(alpha = 0.6f),
//                        fontFamily = FontFamily(Font(R.font.sf_pro))
//                    ), modifier = Modifier.padding(8.dp, bottom = 0.dp)
//                )
//                Column(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .background(
//                            if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
//                            RoundedCornerShape(28.dp)
//                        )
//                        .padding(vertical = 0.dp)
//                ) {
//                    val darkModeLocal = isSystemInDarkTheme()
//
//                    val phoneShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
//                    var phoneBackgroundColor by remember {
//                        mutableStateOf(
//                            if (darkModeLocal) Color(
//                                0xFF1C1C1E
//                            ) else Color(0xFFFFFFFF)
//                        )
//                    }
//                    val phoneAnimatedBackgroundColor by animateColorAsState(
//                        targetValue = phoneBackgroundColor,
//                        animationSpec = tween(durationMillis = 500)
//                    )
//
//                    Row(
//                        modifier = Modifier
//                            .height(48.dp)
//                            .fillMaxWidth()
//                            .background(phoneAnimatedBackgroundColor, phoneShape)
//                            .pointerInput(Unit) {
//                                detectTapGestures(
//                                    onPress = {
//                                        phoneBackgroundColor =
//                                            if (darkModeLocal) Color(0x40888888) else Color(
//                                                0x40D9D9D9
//                                            )
//                                        tryAwaitRelease()
//                                        phoneBackgroundColor =
//                                            if (darkModeLocal) Color(0xFF1C1C1E) else Color(
//                                                0xFFFFFFFF
//                                            )
//                                        phoneEQEnabled.value = !phoneEQEnabled.value
//                                    })
//                            }
//                            .padding(horizontal = 16.dp),
//                        verticalAlignment = Alignment.CenterVertically) {
//                        Text(
//                            stringResource(R.string.phone),
//                            fontSize = 16.sp,
//                            color = textColor,
//                            fontFamily = FontFamily(Font(R.font.sf_pro)),
//                            modifier = Modifier.weight(1f)
//                        )
//                        Checkbox(
//                            checked = phoneEQEnabled.value,
//                            onCheckedChange = { phoneEQEnabled.value = it },
//                            colors = CheckboxDefaults.colors().copy(
//                                checkedCheckmarkColor = Color(0xFF007AFF),
//                                uncheckedCheckmarkColor = Color.Transparent,
//                                checkedBoxColor = Color.Transparent,
//                                uncheckedBoxColor = Color.Transparent,
//                                checkedBorderColor = Color.Transparent,
//                                uncheckedBorderColor = Color.Transparent
//                            ),
//                            modifier = Modifier
//                                .height(24.dp)
//                                .scale(1.5f)
//                        )
//                    }
//
//                    HorizontalDivider(
//                        thickness = 1.dp, color = Color(0x40888888)
//                    )
//
//                    val mediaShape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
//                    var mediaBackgroundColor by remember {
//                        mutableStateOf(
//                            if (darkModeLocal) Color(
//                                0xFF1C1C1E
//                            ) else Color(0xFFFFFFFF)
//                        )
//                    }
//                    val mediaAnimatedBackgroundColor by animateColorAsState(
//                        targetValue = mediaBackgroundColor,
//                        animationSpec = tween(durationMillis = 500)
//                    )
//
//                    Row(
//                        modifier = Modifier
//                            .height(48.dp)
//                            .fillMaxWidth()
//                            .background(mediaAnimatedBackgroundColor, mediaShape)
//                            .pointerInput(Unit) {
//                                detectTapGestures(
//                                    onPress = {
//                                        mediaBackgroundColor =
//                                            if (darkModeLocal) Color(0x40888888) else Color(
//                                                0x40D9D9D9
//                                            )
//                                        tryAwaitRelease()
//                                        mediaBackgroundColor =
//                                            if (darkModeLocal) Color(0xFF1C1C1E) else Color(
//                                                0xFFFFFFFF
//                                            )
//                                        mediaEQEnabled.value = !mediaEQEnabled.value
//                                    })
//                            }
//                            .padding(horizontal = 16.dp),
//                        verticalAlignment = Alignment.CenterVertically) {
//                        Text(
//                            stringResource(R.string.media),
//                            fontSize = 16.sp,
//                            color = textColor,
//                            fontFamily = FontFamily(Font(R.font.sf_pro)),
//                            modifier = Modifier.weight(1f)
//                        )
//                        Checkbox(
//                            checked = mediaEQEnabled.value,
//                            onCheckedChange = { mediaEQEnabled.value = it },
//                            colors = CheckboxDefaults.colors().copy(
//                                checkedCheckmarkColor = Color(0xFF007AFF),
//                                uncheckedCheckmarkColor = Color.Transparent,
//                                checkedBoxColor = Color.Transparent,
//                                uncheckedBoxColor = Color.Transparent,
//                                checkedBorderColor = Color.Transparent,
//                                uncheckedBorderColor = Color.Transparent
//                            ),
//                            modifier = Modifier
//                                .height(24.dp)
//                                .scale(1.5f)
//                        )
//                    }
//                }
//
////                 EQ Settings. Don't seem to have an effect?
//                Column(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .background(
//                            if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
//                            RoundedCornerShape(28.dp)
//                        )
//                        .padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    val trackColor = if (isDarkTheme) Color(0xFFB3B3B3) else Color(0xFF929491)
//                    val activeTrackColor = if (isDarkTheme) Color(0xFF007AFF) else Color(0xFF3C6DF5)
//                    val thumbColor = if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFFFFFFFF)
//
//                    for (i in 0 until 8) {
//                        val eqPhoneValue =
//                            remember(phoneMediaEQ.value[i]) { mutableFloatStateOf(phoneMediaEQ.value[i]) }
//                        Row(
//                            horizontalArrangement = Arrangement.SpaceBetween,
//                            verticalAlignment = Alignment.CenterVertically,
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .height(38.dp)
//                        ) {
//                            Text(
//                                text = String.format("%.2f", eqPhoneValue.floatValue),
//                                fontSize = 12.sp,
//                                color = textColor,
//                                modifier = Modifier.padding(bottom = 4.dp)
//                            )
//
//                            Slider(
//                                value = eqPhoneValue.floatValue,
//                                onValueChange = { newVal ->
//                                    eqPhoneValue.floatValue = newVal
//                                    val newEQ = phoneMediaEQ.value.copyOf()
//                                    newEQ[i] = eqPhoneValue.floatValue
//                                    phoneMediaEQ.value = newEQ
//                                },
//                                valueRange = 0f..100f,
//                                modifier = Modifier
//                                    .fillMaxWidth(0.9f)
//                                    .height(36.dp),
//                                colors = SliderDefaults.colors(
//                                    thumbColor = thumbColor,
//                                    activeTrackColor = activeTrackColor,
//                                    inactiveTrackColor = trackColor
//                                ),
//                                thumb = {
//                                    Box(
//                                        modifier = Modifier
//                                            .size(24.dp)
//                                            .shadow(4.dp, CircleShape)
//                                            .background(thumbColor, CircleShape)
//                                    )
//                                },
//                                track = {
//                                    Box(
//                                        modifier = Modifier
//                                            .fillMaxWidth()
//                                            .height(12.dp),
//                                        contentAlignment = Alignment.CenterStart
//                                    ) {
//                                        Box(
//                                            modifier = Modifier
//                                                .fillMaxWidth()
//                                                .height(4.dp)
//                                                .background(trackColor, RoundedCornerShape(4.dp))
//                                        )
//                                        Box(
//                                            modifier = Modifier
//                                                .fillMaxWidth(eqPhoneValue.floatValue / 100f)
//                                                .height(4.dp)
//                                                .background(
//                                                    activeTrackColor, RoundedCornerShape(4.dp)
//                                                )
//                                        )
//                                    }
//                                })
//
//                            Text(
//                                text = stringResource(R.string.band_label, i + 1),
//                                fontSize = 12.sp,
//                                color = textColor,
//                                modifier = Modifier.padding(top = 4.dp)
//                            )
//                        }
//                    }
//                }
//            }
            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
}
