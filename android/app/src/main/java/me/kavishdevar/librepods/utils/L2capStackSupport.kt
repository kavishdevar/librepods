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

/** One UI encodes 9.0 as 90000, 8.5 as 85000, 8.0 as 80000. */
internal const val ONE_UI_9 = 90_000

/** Samsung SEM_PLATFORM_INT for One UI 9.0 (One UI 8.0 is 170000). */
internal const val SEM_PLATFORM_ONE_UI_9 = 180_000

fun isSamsungManufacturer(manufacturer: String, brand: String = ""): Boolean {
    return manufacturer.equals("samsung", ignoreCase = true) ||
        brand.equals("samsung", ignoreCase = true)
}

/**
 * Pure allowlist for the AOSP/OEM L2CAP fix. Android 17 (SDK 37) has it in AOSP.
 * Samsung ships it as One UI 9. One UI 8 / 8.5 does not, even if SEM looks high.
 *
 * When [oneUiVersion] is present it wins: a known 8.5 (85000) must not be rescued
 * by [semPlatformInt].
 */
internal fun hasFixedL2capStack(
    sdkInt: Int,
    manufacturer: String,
    androidRelease: String,
    buildId: String,
    oneUiVersion: Int?,
    semPlatformInt: Int?
): Boolean {
    if (sdkInt >= 37) return true
    if (androidRelease.startsWith("17")) return true

    val mfr = manufacturer.lowercase()
    if (mfr == "google" && sdkInt == 36) {
        return buildId.startsWith("CP1A")
    }
    if (mfr in listOf("oneplus", "oppo", "realme") && sdkInt >= 36) return true
    return isSamsungManufacturer(manufacturer) &&
        isSamsungOneUi9OrNewer(oneUiVersion, semPlatformInt)
}

internal fun isSamsungOneUi9OrNewer(
    oneUiVersion: Int?,
    semPlatformInt: Int?
): Boolean {
    if (oneUiVersion != null) return oneUiVersion >= ONE_UI_9
    if (semPlatformInt != null) return semPlatformInt >= SEM_PLATFORM_ONE_UI_9
    return false
}
