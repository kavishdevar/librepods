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

@file:OptIn(ExperimentalEncodingApi::class)

package me.kavishdevar.librepods.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.kavishdevar.librepods.R
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable
fun AudioSettings(
    adaptiveVolumeCapability: Boolean,
    conversationalAwarenessCapability: Boolean,
    loudSoundReductionCapability: Boolean,
    adaptiveAudioCapability: Boolean,
    customEqCapability: Boolean,

    adaptiveVolumeChecked: Boolean,
    onAdaptiveVolumeCheckedChange: (Boolean) -> Unit,

    conversationalAwarenessChecked: Boolean,
    onConversationalAwarenessCheckedChange: (Boolean) -> Unit,

    loudSoundReductionChecked: Boolean,
    onLoudSoundReductionCheckedChange: (Boolean) -> Unit,

    systemSpatialAudioSupported: Boolean = false,
    systemSpatialAudioEnabled: Boolean = false,
    systemSpatialAudioAvailable: Boolean = false,
    systemHeadTrackerAvailable: Boolean = false,
    onOpenSystemSpatialAudio: (() -> Unit)? = null,

    navigateToAdaptiveStrength: () -> Unit,
    navigateToEqualizer: () -> Unit,

    vendorIdHook: Boolean,
    isPremium: Boolean
) {
    if (adaptiveVolumeCapability || conversationalAwarenessCapability || loudSoundReductionCapability || adaptiveAudioCapability || onOpenSystemSpatialAudio != null) {
        StyledList(title = stringResource(R.string.audio)) {
            if (onOpenSystemSpatialAudio != null) {
                val spatialAudioDescription = when {
                    !systemSpatialAudioSupported ->
                        stringResource(R.string.system_spatial_audio_unsupported)
                    systemSpatialAudioEnabled && systemSpatialAudioAvailable ->
                        stringResource(R.string.system_spatial_audio_active)
                    systemSpatialAudioEnabled ->
                        stringResource(R.string.system_spatial_audio_enabled_unavailable)
                    else -> stringResource(R.string.system_spatial_audio_off)
                }
                StyledListItem(
                    name = stringResource(R.string.system_spatial_audio),
                    description = spatialAudioDescription,
                    onClick = onOpenSystemSpatialAudio,
                )
                StyledListItem(
                    name = stringResource(R.string.head_tracked_spatial_audio),
                    description = if (systemHeadTrackerAvailable) {
                        stringResource(R.string.system_head_tracking_available)
                    } else {
                        stringResource(R.string.system_head_tracking_airpods_unavailable)
                    },
                    onClick = null,
                    enabled = true,
                )
            }

            if (adaptiveVolumeCapability) {
                StyledToggle(
                    label = stringResource(R.string.personalized_volume),
                    description = stringResource(R.string.personalized_volume_description),
                    checked = adaptiveVolumeChecked,
                    onCheckedChange = onAdaptiveVolumeCheckedChange,
                    enabled = isPremium,
                )
            }

            if (conversationalAwarenessCapability) {
                StyledToggle(
                    label = stringResource(R.string.conversational_awareness),
                    description = stringResource(R.string.conversational_awareness_description),
                    checked = conversationalAwarenessChecked,
                    onCheckedChange = onConversationalAwarenessCheckedChange,
                    enabled = isPremium,
                )
            }

            if (loudSoundReductionCapability && vendorIdHook) {
                StyledToggle(
                    label = stringResource(R.string.loud_sound_reduction),
                    description = stringResource(R.string.loud_sound_reduction_description),
                    checked = loudSoundReductionChecked,
                    onCheckedChange = onLoudSoundReductionCheckedChange,
                    enabled = isPremium,
                )
            }

            if (adaptiveAudioCapability) {
                StyledListItem(
                    name = stringResource(R.string.adaptive_audio),
                    onClick = navigateToAdaptiveStrength,
                )
            }

            if (customEqCapability) {
                StyledListItem(
                    name = stringResource(R.string.equalizer),
                    onClick = navigateToEqualizer,
                )
            }
        }
    }
}
