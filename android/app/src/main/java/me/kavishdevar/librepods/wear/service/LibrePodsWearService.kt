package me.kavishdevar.librepods.wear.service

import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.IBinder
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.bluetooth.BLEManager
import me.kavishdevar.librepods.bluetooth.BluetoothConnectionManager
import me.kavishdevar.librepods.wear.bluetooth.AirPodsConnectionSession
import me.kavishdevar.librepods.wear.bluetooth.WearBluetoothConnection
import me.kavishdevar.librepods.wear.core.AirPodsController

/**
 * Background lifecycle boundary for direct AirPods control on Wear OS.
 *
 * The session owns the sockets. The legacy BluetoothConnectionManager is only
 * bound as a temporary compatibility facade for the inherited AACP code.
 */
class LibrePodsWearService : Service() {
    private lateinit var controller: AirPodsController
    private lateinit var transport: WearBluetoothConnection
    private lateinit var session: AirPodsConnectionSession

    override fun onCreate() {
        super.onCreate()

        val adapter = getSystemService(BluetoothManager::class.java)?.adapter
            ?: error("Bluetooth adapter is unavailable")

        session = AirPodsConnectionSession(adapter)
        BluetoothConnectionManager.bind(session)

        transport = WearBluetoothConnection(this)
        transport.attachSession(session)

        controller = AirPodsController(transport)
        controller.initialize(
            aacpManager = AACPManager(),
            bleManager = BLEManager(this),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        controller.shutdown()
        BluetoothConnectionManager.unbind(session)
        transport.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
