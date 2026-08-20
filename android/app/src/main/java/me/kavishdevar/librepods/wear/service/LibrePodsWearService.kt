package me.kavishdevar.librepods.wear.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import me.kavishdevar.librepods.bluetooth.BLEManager
import me.kavishdevar.librepods.wear.core.AirPodsController

/**
 * Minimal foreground-capable service boundary for Wear OS AirPods control.
 * Bluetooth lifecycle will be connected here after the protocol core is
 * separated from the legacy phone service.
 */
class LibrePodsWearService : Service() {
    private val controller = AirPodsController()

    override fun onCreate() {
        super.onCreate()
        // BLEManager currently belongs to the inherited protocol layer.
        // Its Wear-specific construction will be wired in the next refactor.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        controller.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
