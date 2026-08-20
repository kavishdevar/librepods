package me.kavishdevar.librepods.wear.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.bluetooth.BLEManager
import me.kavishdevar.librepods.wear.bluetooth.WearBluetoothConnection
import me.kavishdevar.librepods.wear.core.AirPodsController

/**
 * Background lifecycle boundary for direct AirPods control on Wear OS.
 *
 * The service owns long-lived protocol state; the Activity remains a thin UI
 * entry point and does not own Bluetooth resources.
 */
class LibrePodsWearService : Service() {
    private lateinit var controller: AirPodsController

    override fun onCreate() {
        super.onCreate()

        controller = AirPodsController(WearBluetoothConnection(this))
        controller.initialize(
            aacpManager = AACPManager(),
            bleManager = BLEManager(this),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        controller.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
