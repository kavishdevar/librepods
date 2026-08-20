package me.kavishdevar.librepods.wear.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * Wear Bluetooth facade. L2CAP ownership is delegated to
 * [AirPodsConnectionSession]; this class remains the platform entry point.
 */
class WearBluetoothConnection(context: Context) {
    @Suppress("UNUSED_PARAMETER")
    private val appContext = context.applicationContext
    private var session: AirPodsConnectionSession? = null

    fun attachSession(connectionSession: AirPodsConnectionSession) {
        session = connectionSession
    }

    fun connectionState(): StateFlow<AirPodsConnectionSession.State>? = session?.state

    fun close() {
        session?.close()
    }
}
