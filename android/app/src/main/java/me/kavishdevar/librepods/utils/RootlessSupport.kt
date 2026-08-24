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

import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.content.edit

private const val TAG = "RootlessSupport"

fun isSupported(sharedPreferences: SharedPreferences): Boolean {
    if (deviceHasFixedL2capStack()) return true

    val isBypassFlagActive = sharedPreferences.getBoolean("bypass_device_check.v2", false)
    return isBypassFlagActive
}

fun bypassDeviceCheck(sharedPreferences: SharedPreferences) {
    sharedPreferences.edit { putBoolean("bypass_device_check.v2", true) }
}

fun isSamsungDevice(
    manufacturer: String = Build.MANUFACTURER,
    brand: String = Build.BRAND
): Boolean = isSamsungManufacturer(manufacturer, brand)

internal fun deviceHasFixedL2capStack(): Boolean {
    val oneUiVersion = readOneUiVersion()
    val semPlatformInt = readSemPlatformInt()
    val supported = hasFixedL2capStack(
        sdkInt = Build.VERSION.SDK_INT,
        manufacturer = Build.MANUFACTURER,
        androidRelease = Build.VERSION.RELEASE ?: "",
        buildId = Build.ID ?: "",
        oneUiVersion = oneUiVersion,
        semPlatformInt = semPlatformInt
    )
    if (supported && isSamsungDevice()) {
        Log.i(
            TAG,
            "Samsung One UI 9+ detected (oneui=$oneUiVersion sem=$semPlatformInt sdk=${Build.VERSION.SDK_INT})"
        )
    }
    return supported
}

fun readOneUiVersion(): Int? {
    val raw = readSystemProperty("ro.build.version.oneui") ?: return null
    return raw.filter { it.isDigit() }.toIntOrNull()
}

fun readSemPlatformInt(): Int? {
    return try {
        Build.VERSION::class.java.getField("SEM_PLATFORM_INT").getInt(null)
    } catch (_: Throwable) {
        readSystemProperty("ro.build.version.sep")?.toIntOrNull()
    }
}

fun oneUiVersionLabel(): String? {
    val oneUi = readOneUiVersion()
    if (oneUi != null) {
        val major = oneUi / 10000
        val minor = (oneUi / 100) % 100
        return if (minor == 0) "$major.0" else "$major.$minor"
    }
    val sem = readSemPlatformInt() ?: return null
    val major = sem / 10000 - 9
    val minor = (sem / 100) % 100
    return if (major >= 1) {
        if (minor == 0) "$major.0" else "$major.$minor"
    } else {
        sem.toString()
    }
}

private fun readSystemProperty(key: String): String? {
    return try {
        val clazz = Class.forName("android.os.SystemProperties")
        val get = clazz.getMethod("get", String::class.java)
        (get.invoke(null, key) as? String)?.takeIf { it.isNotBlank() }
    } catch (_: Throwable) {
        null
    }
}
