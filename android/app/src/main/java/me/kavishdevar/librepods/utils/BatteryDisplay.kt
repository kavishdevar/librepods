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

import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryComponent
import me.kavishdevar.librepods.data.BatteryStatus

enum class BatteryDisplaySource {
    EARBUDS,
    CASE,
}

data class BatteryDisplaySelection(
    val level: Int,
    val source: BatteryDisplaySource,
)

/** Keeps the compact battery value and its artwork source as one decision. */
object BatteryDisplay {
    fun select(batteries: List<Battery>): BatteryDisplaySelection? {
        val selected = batteries.asSequence()
            .filter {
                it.component == BatteryComponent.LEFT ||
                    it.component == BatteryComponent.RIGHT ||
                    it.component == BatteryComponent.CASE
            }
            .filter { it.status != BatteryStatus.DISCONNECTED }
            .filter { BatteryLevels.isKnown(it.level) }
            .minWithOrNull(
                compareBy<Battery> { it.level }
                    // Prefer an earbud when levels are equal so a 100/100/100 reading
                    // keeps the familiar earbuds visual.
                    .thenBy { if (it.component == BatteryComponent.CASE) 1 else 0 }
            ) ?: return null

        return BatteryDisplaySelection(
            level = selected.level,
            source = if (selected.component == BatteryComponent.CASE) {
                BatteryDisplaySource.CASE
            } else {
                BatteryDisplaySource.EARBUDS
            },
        )
    }
}
