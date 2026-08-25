/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.
*/

package me.kavishdevar.librepods.presentation.overlays

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings

/** How connection and battery updates are presented outside the app. */
enum class ConnectionAlertStyle(val preferenceValue: String) {
    SYSTEM_LIVE_ALERT("system_live_alert"),
    CAMERA_CUTOUT("camera_cutout"),
    BOTTOM_SHEET("bottom_sheet"),
    OFF("off");

    companion object {
        const val PREFERENCE_KEY = "connection_alert_style"

        fun fromPreferences(preferences: SharedPreferences): ConnectionAlertStyle {
            val storedValue = preferences.getString(PREFERENCE_KEY, null)
            entries.firstOrNull { it.preferenceValue == storedValue }?.let { return it }

            // OxygenOS 16 and other Android 16 implementations can render a native status-bar
            // capsule. New and migrated installs should prefer it over a draw-over-apps window.
            return if (LiveAlertSupport.isSupported) {
                SYSTEM_LIVE_ALERT
            } else if (preferences.getBoolean("show_island_popup", true)) {
                CAMERA_CUTOUT
            } else if (preferences.getBoolean("show_bottom_sheet_popup", true)) {
                BOTTOM_SHEET
            } else {
                OFF
            }
        }
    }
}

object LiveAlertSupport {
    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return false
        return context.getSystemService(NotificationManager::class.java)
            .canPostPromotedNotifications()
    }

    fun settingsIntent(context: Context): Intent = Intent(
        ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS
    ).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    // The public constant is not present in every Android 16 SDK minor revision, while the
    // documented action string is stable and is implemented by OxygenOS 16.1.
    private const val ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS =
        "android.settings.APP_NOTIFICATION_PROMOTION_SETTINGS"
}
