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

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * OxygenOS / ColorOS ships a Bluetooth overlay that also speaks AACP on L2CAP PSM 0x1001.
 * Only one process can own that channel. Detect the overlay so we can warn and back off
 * instead of connect/disconnect looping.
 *
 * The overlay package remains installed after `pm disable-user`, so presence on disk is not
 * a conflict. Warn only when the AACP-stealing overlay is actually enabled.
 *
 * Do not treat `com.oplus.framework_bluetooth.overlay` as a thief: it targets `android`
 * (framework resources), not `com.android.bluetooth`. Generic HeyTap/Oplus accessory
 * frameworks also stay installed and do not own L2CAP PSM 0x1001.
 *
 * Do not disable vendor packages automatically. The documented workaround (user-initiated) is:
 *   adb shell pm disable-user --user 0 com.android.bluetooth.oplus.overlay
 * or Settings → Apps → show system → Bluetooth overlay → disable.
 */
object VendorAacpConflict {
    val conflictingPackages = listOf(
        "com.android.bluetooth.oplus.overlay",
        "com.oplus.bluetooth.overlay",
        "com.oplus.airpods",
        "com.oneplus.airpods",
    )

    fun isOplusFamily(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return manufacturer in listOf("oneplus", "oppo", "realme") ||
            brand in listOf("oneplus", "oppo", "realme", "oxygen")
    }

    fun installedConflictingPackages(context: Context): List<String> =
        enabledConflictingPackages(context)

    fun enabledConflictingPackages(context: Context): List<String> {
        val pm = context.packageManager
        return conflictingPackages.filter { pkg -> isPackageEffectivelyEnabled(pm, pkg) }
    }

    fun hasConflictingOwner(context: Context): Boolean {
        return isOplusFamily() && enabledConflictingPackages(context).isNotEmpty()
    }

    /**
     * True only when the package is installed and the user-facing enabled setting is on.
     * `disabled` / `disabled-user` / `disabled-until-used` hide the banner even though
     * the APK is still on disk.
     */
    fun isPackageEffectivelyEnabled(pm: PackageManager, packageName: String): Boolean {
        val setting = try {
            pm.getApplicationEnabledSetting(packageName)
        } catch (_: IllegalArgumentException) {
            return false
        }

        if (setting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ||
            setting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER ||
            setting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED
        ) {
            return false
        }

        if (setting != PackageManager.COMPONENT_ENABLED_STATE_ENABLED &&
            setting != PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        ) {
            return false
        }

        return try {
            pm.getApplicationInfo(packageName, PackageManager.MATCH_DISABLED_COMPONENTS).enabled
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}
