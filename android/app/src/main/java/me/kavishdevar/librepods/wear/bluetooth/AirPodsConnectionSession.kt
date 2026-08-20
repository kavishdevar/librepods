package me.kavishdevar.librepods.wear.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.ParcelUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

/**
 * Owns the direct AirPods L2CAP transport for the Wear application.
 *
 * Keeping socket ownership in one session removes the legacy global socket
 * lifecycle from the application-facing layer and gives reconnect logic one
 * place to operate on.
 */
class AirPodsConnectionSession(
    private val adapter: BluetoothAdapter,
) {
    enum class State { IDLE, CONNECTING, CONNECTED, DISCONNECTING, FAILED }

    private val mutableState = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = mutableState.asStateFlow()

    var aacpSocket: BluetoothSocket? = null
        private set
    var attSocket: BluetoothSocket? = null
        private set

    @Synchronized
    fun connect(
        device: BluetoothDevice,
        aacpUuid: ParcelUuid,
        aacpPsm: Int,
        attUuid: ParcelUuid,
        attPsm: Int,
    ) {
        if (mutableState.value == State.CONNECTING || mutableState.value == State.CONNECTED) return

        mutableState.value = State.CONNECTING
        closeSockets()

        try {
            // The existing LibrePods protocol uses fixed L2CAP PSMs. The
            // actual values are supplied by the protocol discovery layer.
            val newAacp = createBluetoothSocket(adapter, device, aacpUuid, aacpPsm)
            val newAtt = createBluetoothSocket(adapter, device, attUuid, attPsm)

            newAacp.connect()
            try {
                newAtt.connect()
            } catch (attError: IOException) {
                newAacp.close()
                throw attError
            }

            aacpSocket = newAacp
            attSocket = newAtt
            mutableState.value = State.CONNECTED
        } catch (error: Throwable) {
            closeSockets()
            mutableState.value = State.FAILED
            throw error
        }
    }

    @Synchronized
    fun disconnect() {
        if (mutableState.value == State.IDLE) return
        mutableState.value = State.DISCONNECTING
        closeSockets()
        mutableState.value = State.IDLE
    }

    @Synchronized
    fun close() = disconnect()

    private fun closeSockets() {
        runCatching { aacpSocket?.close() }
        runCatching { attSocket?.close() }
        aacpSocket = null
        attSocket = null
    }
}
