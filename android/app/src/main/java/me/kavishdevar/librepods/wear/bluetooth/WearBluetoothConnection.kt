package me.kavishdevar.librepods.wear.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.flow.StateFlow
import java.io.InputStream
import java.io.OutputStream

/**
 * Wear Bluetooth facade. L2CAP ownership is delegated to
 * [AirPodsConnectionSession]; this class exposes the active protocol streams.
 */
class WearBluetoothConnection(context: Context) : AirPodsProtocolTransport {
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

    override val aacpInput: InputStream
        get() = session?.aacpInput
            ?: error("AirPods AACP transport is not connected")

    override val aacpOutput: OutputStream
        get() = session?.aacpOutput
            ?: error("AirPods AACP transport is not connected")

    override val attInput: InputStream
        get() = session?.attInput
            ?: error("AirPods ATT transport is not connected")

    override val attOutput: OutputStream
        get() = session?.attOutput
            ?: error("AirPods ATT transport is not connected")

    fun aacpInput() = aacpInput

    fun close() {
        session?.close()
    }
}
