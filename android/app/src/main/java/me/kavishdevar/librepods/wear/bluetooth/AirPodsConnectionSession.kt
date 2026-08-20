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

/**
 * Owns the direct AirPods L2CAP transport for the Wear application.
 *
 * The session deliberately separates transport establishment from protocol
 * handshake. A CONNECTED transport means both sockets are open; protocol code
 * must still perform its own AACP initialization before exposing features.
 */
class AirPodsConnectionSession(
    private val adapter: BluetoothAdapter,
) : AirPodsProtocolTransport {
    enum class State { IDLE, CONNECTING, CONNECTED, DISCONNECTING, FAILED }

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

    /**
     * Reconnect is transport-only. The caller owns protocol re-initialization.
     */
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
        val type = 3 // L2CAP
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
                Log.d("AirPodsConnection", "Using L2CAP socket constructor #${index + 1}")
                return constructor.newInstance(*params) as BluetoothSocket
            } catch (error: Exception) {
                lastException = error
                Log.d("AirPodsConnection", "L2CAP constructor #${index + 1} unavailable: ${error.message}")
            }
        }
        throw lastException ?: IllegalStateException("No compatible L2CAP BluetoothSocket constructor")
    }
}
