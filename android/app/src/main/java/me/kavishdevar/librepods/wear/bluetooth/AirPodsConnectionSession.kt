package me.kavishdevar.librepods.wear.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.util.UUID

/**
 * Owns the direct AirPods L2CAP transport for the Wear application.
 *
 * AACP is the first transport we bring up because battery/ear state and
 * control notifications are carried on the classic L2CAP PSM 0x1001.
 * ATT remains an optional second transport for later features.
 */
class AirPodsConnectionSession(
    private val adapter: BluetoothAdapter,
) : AirPodsProtocolTransport {
    enum class State { IDLE, CONNECTING, CONNECTED, DISCONNECTING, FAILED }

    companion object {
        /** Apple Accessory Communication Protocol L2CAP PSM. */
        const val AACP_PSM = 0x1001
        private val FALLBACK_UUID = ParcelUuid(UUID(0L, 0L))
    }

    private val mutableState = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = mutableState.asStateFlow()

    var aacpSocket: BluetoothSocket? = null
        private set
    var attSocket: BluetoothSocket? = null
        private set

    override val aacpInput get() = requireSocket(aacpSocket).inputStream
    override val aacpOutput get() = requireSocket(aacpSocket).outputStream
    override val attInput get() = requireSocket(attSocket).inputStream
    override val attOutput get() = requireSocket(attSocket).outputStream

    /**
     * Connect only the AACP control channel. This is enough for the first
     * working milestone: handshake, notifications, battery and ear state.
     */
    @Synchronized
    fun connectAacp(device: BluetoothDevice) {
        if (mutableState.value == State.CONNECTING || mutableState.value == State.CONNECTED) return
        mutableState.value = State.CONNECTING
        closeSockets()

        try {
            adapter.cancelDiscovery()
            val socket = createL2capSocket(device, FALLBACK_UUID, AACP_PSM)
            socket.connect()
            aacpSocket = socket
            mutableState.value = State.CONNECTED
            Log.i("AirPodsConnection", "AACP L2CAP connected to ${device.address} on PSM 0x1001")
        } catch (error: Throwable) {
            closeSockets()
            mutableState.value = State.FAILED
            Log.e("AirPodsConnection", "AACP L2CAP connection failed", error)
            throw error
        }
    }

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
            adapter.cancelDiscovery()
            val newAacp = createL2capSocket(device, aacpUuid, aacpPsm)
            newAacp.connect()
            aacpSocket = newAacp

            try {
                val newAtt = createL2capSocket(device, attUuid, attPsm)
                newAtt.connect()
                attSocket = newAtt
            } catch (attError: IOException) {
                closeSockets()
                throw attError
            }

            mutableState.value = State.CONNECTED
        } catch (error: Throwable) {
            closeSockets()
            mutableState.value = State.FAILED
            throw error
        }
    }

    @Synchronized
    fun reconnectAacp(device: BluetoothDevice) {
        disconnect()
        connectAacp(device)
    }

    @Synchronized
    fun reconnect(
        device: BluetoothDevice,
        aacpUuid: ParcelUuid,
        aacpPsm: Int,
        attUuid: ParcelUuid,
        attPsm: Int,
    ) {
        disconnect()
        connect(device, aacpUuid, aacpPsm, attUuid, attPsm)
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

    private fun requireSocket(socket: BluetoothSocket?): BluetoothSocket =
        socket ?: throw IllegalStateException("AirPods transport is not connected")

    private fun closeSockets() {
        runCatching { aacpSocket?.close() }
        runCatching { attSocket?.close() }
        aacpSocket = null
        attSocket = null
    }

    @Suppress("DEPRECATION")
    private fun createL2capSocket(
        device: BluetoothDevice,
        uuid: ParcelUuid,
        psm: Int,
    ): BluetoothSocket {
        val type = 3 // Classic L2CAP
        val constructorSpecs: List<Array<Any>> = listOf(
            arrayOf(adapter, device, type, true, true, psm, uuid),
            arrayOf(device, type, true, true, psm, uuid),
            arrayOf(device, type, 1, true, true, psm, uuid),
            arrayOf(type, 1, true, true, device, psm, uuid),
            arrayOf(type, true, true, device, psm, uuid),
        )
        var lastException: Exception? = null
        for ((index, params) in constructorSpecs.withIndex()) {
            try {
                val parameterTypes = params.map { it::class.javaPrimitiveType ?: it::class.java }.toTypedArray()
                val constructor = BluetoothSocket::class.java.getDeclaredConstructor(*parameterTypes)
                constructor.isAccessible = true
                Log.d("AirPodsConnection", "Using L2CAP socket constructor #${index + 1} for PSM 0x${psm.toString(16)}")
                return constructor.newInstance(*params) as BluetoothSocket
            } catch (error: Exception) {
                lastException = error
                Log.d("AirPodsConnection", "L2CAP constructor #${index + 1} unavailable: ${error.message}")
            }
        }
        throw lastException ?: IllegalStateException("No compatible L2CAP BluetoothSocket constructor")
    }
}
