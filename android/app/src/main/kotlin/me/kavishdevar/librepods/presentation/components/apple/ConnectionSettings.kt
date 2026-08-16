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

package me.kavishdevar.librepods.presentation.components.apple

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.primitives.StyledList
import me.kavishdevar.librepods.presentation.components.primitives.StyledToggle
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable
fun ConnectionSettings(
    automaticEarDetectionEnabled: Boolean,
    onAutomaticEarDetectionChanged: (Boolean) -> Unit,
    automaticConnectionEnabled: Boolean,
    onAutomaticConnectionChanged: (Boolean) -> Unit,
    disconnectWhenNotWearing: Boolean,
    onDisconnectWhenNotWearingChanged: (Boolean) -> Unit,

    takeoverWhenDisconnected: Boolean,
    onTakeoverWhenDisconnectedChanged: (Boolean) -> Unit,
    takeoverWhenIdle: Boolean,
    onTakeoverWhenIdleChanged: (Boolean) -> Unit,
    takeoverWhenMusic: Boolean,
    onTakeoverWhenMusicChanged: (Boolean) -> Unit,
    takeoverWhenCall: Boolean,
    onTakeoverWhenCallChanged: (Boolean) -> Unit,

    takeoverWhenRingingCall: Boolean,
    onTakeoverWhenRingingCallChanged: (Boolean) -> Unit,
    takeoverWhenMediaStart: Boolean,
    onTakeoverWhenMediaStartChanged: (Boolean) -> Unit,

    isPremium: Boolean
) {
    StyledList {
        StyledToggle(
            label = stringResource(R.string.ear_detection),
            checked = automaticEarDetectionEnabled,
            onCheckedChange = onAutomaticEarDetectionChanged
        )

        StyledToggle(
            label = stringResource(R.string.automatically_connect),
            description = stringResource(R.string.automatically_connect_description),
            checked = automaticConnectionEnabled,
            onCheckedChange = onAutomaticConnectionChanged
        )

        StyledToggle(
            label = stringResource(R.string.disconnect_when_not_wearing),
            description = stringResource(R.string.disconnect_when_not_wearing_description),
            checked = disconnectWhenNotWearing,
            onCheckedChange = onDisconnectWhenNotWearingChanged
        )
    }

//    StyledList(title = stringResource(R.string.takeover_airpods_state)) {
//        StyledToggle(
//            label = stringResource(R.string.takeover_disconnected),
//            description = stringResource(R.string.takeover_disconnected_desc),
//            checked = takeoverWhenDisconnected,
//            onCheckedChange = onTakeoverWhenDisconnectedChanged,
//            enabled = isPremium
//        )
//        StyledToggle(
//            label = stringResource(R.string.takeover_idle),
//            description = stringResource(R.string.takeover_idle_desc),
//            checked = takeoverWhenIdle,
//            onCheckedChange = onTakeoverWhenIdleChanged,
//            enabled = isPremium
//        )
//        StyledToggle(
//            label = stringResource(R.string.takeover_music),
//            description = stringResource(R.string.takeover_music_desc),
//            checked = takeoverWhenMusic,
//            onCheckedChange = onTakeoverWhenMusicChanged,
//            enabled = isPremium
//        )
//
//        StyledToggle(
//            label = stringResource(R.string.takeover_call),
//            description = stringResource(R.string.takeover_call_desc),
//            checked = takeoverWhenCall,
//            onCheckedChange = onTakeoverWhenCallChanged,
//            enabled = isPremium
//        )
//    }
//
//    StyledList(title = stringResource(R.string.takeover_phone_state)) {
//        StyledToggle(
//            label = stringResource(R.string.takeover_ringing_call),
//            description = stringResource(R.string.takeover_ringing_call_desc),
//            checked = takeoverWhenRingingCall,
//            onCheckedChange = onTakeoverWhenRingingCallChanged,
//            enabled = isPremium
//        )
//        StyledToggle(
//            label = stringResource(R.string.takeover_media_start),
//            description = stringResource(R.string.takeover_media_start_desc),
//            checked = takeoverWhenMediaStart,
//            onCheckedChange = onTakeoverWhenMediaStartChanged,
//            enabled = isPremium
//        )
//    }
}
