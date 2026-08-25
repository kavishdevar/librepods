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

package me.kavishdevar.librepods.utils

import me.kavishdevar.librepods.data.BatteryStatus

/**
 * AirPods report 0x7F (127) as an unknown battery sentinel, not 127%.
 * 0xFF is the same idea in some BLE advertisements.
 */
object BatteryLevels {
    const val UNKNOWN_SENTINEL = 0x7F
    const val UNKNOWN_LEVEL = -1

    fun sanitizePercent(raw: Int): Int {
        val unsigned = raw and 0xFF
        return if (unsigned in 0..100) unsigned else UNKNOWN_LEVEL
    }

    fun isKnown(level: Int): Boolean = level in 0..100

    fun displayPercent(level: Int): String = if (isKnown(level)) "$level%" else "—"

    fun statusFor(level: Int, rawStatus: Int): Int {
        return if (!isKnown(level) && rawStatus != BatteryStatus.DISCONNECTED) {
            BatteryStatus.UNKNOWN
        } else {
            rawStatus
        }
    }
}
