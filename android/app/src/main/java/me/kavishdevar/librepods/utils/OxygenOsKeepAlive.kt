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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import me.kavishdevar.librepods.receivers.ServiceWatchdogWorker
import java.util.concurrent.TimeUnit

object OxygenOsKeepAlive {
    private const val TAG = "OxygenOsKeepAlive"

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(PowerManager::class.java) ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (isIgnoringBatteryOptimizations(context)) return
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            if (context !is android.app.Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "REQUEST_IGNORE_BATTERY_OPTIMIZATIONS failed, opening settings", e)
            openBatteryOptimizationSettings(context)
        }
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val intents = listOf(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        )
        startFirstAvailable(context, intents)
    }

    /**
     * Deep-link to OnePlus / OxygenOS / ColorOS auto-start and battery pages.
     * These activity names change across OS versions; we try several and fall back.
     */
    fun openAutoStartSettings(context: Context): Boolean {
        val candidates = listOf(
            component("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
            component("com.oplus.safecenter", "com.oplus.safecenter.startupapp.view.StartupAppListActivity"),
            component("com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity"),
            component("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            component("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            component("com.oplus.battery", "com.oplus.powermanager.fuelgaue.PowerControlActivity"),
            component("com.oneplus.security", "com.oneplus.security.startup.StartupManagerActivity")
        )
        for (intent in candidates) {
            if (intent.resolveActivity(context.packageManager) != null) {
                try {
                    context.startActivity(intent)
                    return true
                } catch (e: Exception) {
                    Log.d(TAG, "Auto-start intent failed: ${intent.component}", e)
                }
            }
        }
        openBatteryOptimizationSettings(context)
        return false
    }

    fun scheduleWatchdog(context: Context) {
        val appContext = context.applicationContext
        try {
            val request = PeriodicWorkRequestBuilder<ServiceWatchdogWorker>(
                15, TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                ServiceWatchdogWorker.UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to enqueue WorkManager watchdog", e)
        }
    }

    fun cancelWatchdog(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(ServiceWatchdogWorker.UNIQUE_NAME)
    }

    private fun component(pkg: String, cls: String): Intent {
        return Intent().apply {
            component = ComponentName(pkg, cls)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun startFirstAvailable(context: Context, intents: List<Intent>) {
        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return
                }
            } catch (e: Exception) {
                Log.d(TAG, "Keep-alive intent failed: ${intent.action}", e)
            }
        }
    }
}
