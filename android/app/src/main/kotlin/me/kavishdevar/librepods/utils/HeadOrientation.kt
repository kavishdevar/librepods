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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Acceleration(val vertical: Float = 0f, val horizontal: Float = 0f)

// TODO: remove
object HeadTracking {
    private val _acceleration = MutableStateFlow(Acceleration())
    val acceleration = _acceleration.asStateFlow()

    fun addAccel(vert: Float, horiz: Float) {
        _acceleration.value = Acceleration(vert, horiz)
    }

    fun reset() {
        _acceleration.value = Acceleration()
    }
}
