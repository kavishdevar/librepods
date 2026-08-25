/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.
*/

package me.kavishdevar.librepods.utils

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.Spatializer
import android.provider.Settings

data class SpatialAudioStatus(
    val supported: Boolean = false,
    val enabled: Boolean = false,
    val available: Boolean = false,
    val headTrackerAvailable: Boolean = false,
)

/**
 * Read-only bridge to Android's system Spatializer.
 *
 * Android only exposes availability to regular apps. Enabling head tracking and supplying
 * motion data are system responsibilities, so LibrePods must not present an app toggle that
 * implies it can drive OxygenOS' audio renderer.
 */
object SpatialAudioController {
    fun getStatus(context: Context): SpatialAudioStatus {
        return try {
            val spatializer = context.getSystemService(AudioManager::class.java).spatializer
            SpatialAudioStatus(
                supported = spatializer.immersiveAudioLevel !=
                    Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_NONE,
                enabled = spatializer.isEnabled,
                available = spatializer.isAvailable,
                headTrackerAvailable = spatializer.isHeadTrackerAvailable,
            )
        } catch (_: RuntimeException) {
            SpatialAudioStatus()
        }
    }

    fun openSystemSettings(context: Context) {
        val intents = listOf(
            Intent(Settings.ACTION_SOUND_SETTINGS),
            Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
        )

        intents.firstOrNull { it.resolveActivity(context.packageManager) != null }?.let {
            context.startActivity(it)
        }
    }
}
