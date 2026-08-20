package me.kavishdevar.librepods

import android.app.Application

/**
 * Application entry point for the autonomous Wear OS build.
 *
 * Platform-specific initialization is intentionally kept minimal. Bluetooth
 * and AirPods protocol lifecycle belongs to the dedicated service layer.
 */
class LibrePodsApplication : Application()
