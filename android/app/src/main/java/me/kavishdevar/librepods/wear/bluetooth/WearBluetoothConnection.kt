package me.kavishdevar.librepods.wear.bluetooth

import android.annotation.SuppressLint
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

    /** Connect the AACP classic L2CAP channel on PSM 0x1001. */
    @SuppressLint("MissingPermission")
    fun connectAacp(device: BluetoothDevice) {
        session?.connectAacp(device)
            ?: error("AirPods connection session is not attached")
    }

    fun aacpInput() = session?.aacpInput
        ?: error("AirPods AACP transport is not connected")

    fun close() {
        session?.close()
    }
}
