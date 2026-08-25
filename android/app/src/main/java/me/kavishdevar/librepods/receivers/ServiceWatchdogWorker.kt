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

package me.kavishdevar.librepods.receivers

import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import me.kavishdevar.librepods.services.AirPodsService
import me.kavishdevar.librepods.services.ServiceManager

class ServiceWatchdogWorker(
    context: android.content.Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Log.d(TAG, "WorkManager watchdog fired")
        val setupComplete = applicationContext.getSharedPreferences(
            "settings",
            android.content.Context.MODE_PRIVATE
        ).getBoolean("onboarding_complete", false)
        if (!setupComplete) return Result.success()

        try {
            if (ServiceManager.getService() == null) {
                applicationContext.startForegroundService(
                    Intent(applicationContext, AirPodsService::class.java)
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Watchdog could not start AirPodsService", e)
        }
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "librepods_service_watchdog"
        private const val TAG = "ServiceWatchdogWorker"
    }
}
