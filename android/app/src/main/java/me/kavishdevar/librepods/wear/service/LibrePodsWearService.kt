package me.kavishdevar.librepods.wear.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.MainActivity
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.bluetooth.BLEManager
import me.kavishdevar.librepods.wear.bluetooth.AirPodsConnectionSession
import me.kavishdevar.librepods.wear.bluetooth.WearBluetoothConnection
import me.kavishdevar.librepods.wear.bluetooth.WearBluetoothScanner
import me.kavishdevar.librepods.wear.core.AirPodsController
import me.kavishdevar.librepods.wear.core.AirPodsState

/**
 * Owner of the autonomous AirPods stack on the watch.
 *
 * The service holds the only [AirPodsController] instance so the protocol
 * session survives UI recreation, and keeps the connection alive in the
 * background as a connected-device foreground service.
 */
class LibrePodsWearService : Service() {
    inner class LocalBinder : Binder() {
        val service: LibrePodsWearService get() = this@LibrePodsWearService
    }

    lateinit var controller: AirPodsController
        private set
    lateinit var scanner: WearBluetoothScanner
        private set

    private lateinit var transport: WearBluetoothConnection
    private lateinit var session: AirPodsConnectionSession
    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var foregroundStarted = false

    override fun onCreate() {
        super.onCreate()

        val adapter = getSystemService(BluetoothManager::class.java)?.adapter
            ?: error("Bluetooth adapter is unavailable")

        session = AirPodsConnectionSession(adapter)
        transport = WearBluetoothConnection(this)
        transport.attachSession(session)

        scanner = WearBluetoothScanner(this)
        controller = AirPodsController(this, transport)
        controller.initialize(
            aacpManager = AACPManager(),
            bleManager = BLEManager(this),
        )

        createNotificationChannel()
        scope.launch { controller.state.collectLatest(::onStateChanged) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundIfPossible(controller.state.value)
        when (intent?.action) {
            ACTION_CONNECT -> {
                val address = intent.getStringExtra(EXTRA_ADDRESS)
                val name = intent.getStringExtra(EXTRA_NAME) ?: "AirPods"
                if (address != null) controller.connectToDevice(address, name) else controller.connectToBondedAirPods()
            }
            ACTION_DISCONNECT -> controller.disconnect()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        scope.cancel()
        controller.shutdown()
        scanner.stopScan()
        transport.close()
        super.onDestroy()
    }

    private fun onStateChanged(state: AirPodsState) {
        if (!foregroundStarted) return
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun startForegroundIfPossible(state: AirPodsState) {
        if (foregroundStarted) return
        if (!hasConnectPermission()) return
        runCatching { startForeground(NOTIFICATION_ID, buildNotification(state)) }
            .onSuccess { foregroundStarted = true }
    }

    private fun buildNotification(state: AirPodsState): Notification {
        val content = when {
            state.connecting -> "Connecting…"
            state.connected -> listOfNotNull(
                state.leftBattery?.takeIf { it in 0..100 }?.let { "L $it%" },
                state.rightBattery?.takeIf { it in 0..100 }?.let { "R $it%" },
                state.caseBattery?.takeIf { it in 0..100 }?.let { "C $it%" },
            ).joinToString(" · ").ifEmpty { "Connected" }
            else -> state.lastError ?: "Disconnected"
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle(state.deviceName)
            .setContentText(content)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "AirPods connection", NotificationManager.IMPORTANCE_LOW)
        channel.setShowBadge(false)
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun hasConnectPermission(): Boolean =
        android.os.Build.VERSION.SDK_INT < 31 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val CHANNEL_ID = "librepods_wear_connection"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_CONNECT = "me.kavishdevar.librepods.wear.CONNECT"
        const val ACTION_DISCONNECT = "me.kavishdevar.librepods.wear.DISCONNECT"
        const val EXTRA_ADDRESS = "address"
        const val EXTRA_NAME = "name"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, LibrePodsWearService::class.java))
        }
    }
}
