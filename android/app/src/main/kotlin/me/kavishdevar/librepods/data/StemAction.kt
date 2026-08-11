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

package me.kavishdevar.librepods.data

import kotlinx.serialization.Serializable
import me.kavishdevar.librepods.bluetooth.aacp.types.StemPressType

@Serializable
enum class StemAction {
    PLAY_PAUSE,
    PREVIOUS_TRACK,
    NEXT_TRACK,
    DIGITAL_ASSISTANT,
    CYCLE_NOISE_CONTROL_MODES;
    companion object {
        fun fromString(action: String): StemAction? {
            return entries.find { it.name == action }
        }
        val defaultActions: Map<StemPressType, StemAction> = mapOf(
            StemPressType.SINGLE_PRESS to PLAY_PAUSE,
            StemPressType.DOUBLE_PRESS to NEXT_TRACK,
            StemPressType.TRIPLE_PRESS to PREVIOUS_TRACK,
            StemPressType.LONG_PRESS to CYCLE_NOISE_CONTROL_MODES,
        )
    }
}
