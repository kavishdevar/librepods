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

/** One UI encodes 9.0 as 90000, 8.5 as 85000, 8.0 as 80000. */
internal const val ONE_UI_9 = 90_000

/** Samsung SEM_PLATFORM_INT for One UI 9.0 (One UI 8.0 is 170000). */
internal const val SEM_PLATFORM_ONE_UI_9 = 180_000

fun isSupported(sharedPreferences: SharedPreferences): Boolean {
    if (hasFixedL2capStack()) return true

    val isBypassFlagActive = sharedPreferences.getBoolean("bypass_device_check.v2", false)
    return isBypassFlagActive
}

fun bypassDeviceCheck(sharedPreferences: SharedPreferences) {
    sharedPreferences.edit { putBoolean("bypass_device_check.v2", true) }
}

fun isSamsungDevice(
    manufacturer: String = Build.MANUFACTURER,
    brand: String = Build.BRAND
): Boolean {
    return manufacturer.equals("samsung", ignoreCase = true) ||
        brand.equals("samsung", ignoreCase = true)
}

/**
 * True when this OS build includes Google's AirPods L2CAP stack fix, so LibrePods
 * can talk to AirPods without root or Xposed.
 *
 * Android 17 (SDK 37) has the fix in AOSP. Samsung ships it as One UI 9; Galaxy S26
 * on One UI 8 / 8.5 does not. One UI 9 betas sometimes still report SDK 36, so we
 * also read Samsung's One UI version properties.
 */
internal fun hasFixedL2capStack(
    sdkInt: Int = Build.VERSION.SDK_INT,
    manufacturer: String = Build.MANUFACTURER,
    androidRelease: String = Build.VERSION.RELEASE ?: "",
    buildId: String = Build.ID ?: "",
    oneUiVersion: Int? = readOneUiVersion(),
    semPlatformInt: Int? = readSemPlatformInt()
): Boolean {
    if (sdkInt >= 37) return true
    if (androidRelease.startsWith("17")) return true

    val mfr = manufacturer.lowercase()
    val isPixel = mfr == "google"
    val isOppoFamily = mfr in listOf("oneplus", "oppo", "realme")

    if (isPixel && sdkInt == 36) {
        return buildId.startsWith("CP1A")
    }
    if (isOppoFamily && sdkInt >= 36) return true
    if (isSamsungDevice(manufacturer) && isSamsungOneUi9OrNewer(oneUiVersion, semPlatformInt)) {
        Log.i(
            TAG,
            "Samsung One UI 9+ detected (oneui=$oneUiVersion sem=$semPlatformInt sdk=$sdkInt)"
        )
        return true
    }
    return false
}

internal fun isSamsungOneUi9OrNewer(
    oneUiVersion: Int? = readOneUiVersion(),
    semPlatformInt: Int? = readSemPlatformInt()
): Boolean {
    if (oneUiVersion != null && oneUiVersion >= ONE_UI_9) return true
    if (semPlatformInt != null && semPlatformInt >= SEM_PLATFORM_ONE_UI_9) return true
    return false
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
