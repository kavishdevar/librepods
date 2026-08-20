package me.kavishdevar.librepods.wear.bluetooth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream

/**
 * Framed packet channel used by the Wear protocol adapters.
 *
 * AACP packets carry their payload length in the first two bytes after the
 * fixed four-byte transport header. The channel keeps stream framing out of
 * the protocol parser and guarantees serialized writes.
 */
class AirPodsPacketChannel(
    private val input: InputStream,
    private val output: OutputStream,
) {
    private val writeLock = Any()

    fun write(packet: ByteArray) {
        synchronized(writeLock) {
            output.write(packet)
            output.flush()
        }
    }

    fun readPacket(): ByteArray {
        val header = readFully(4)
        val lengthBytes = readFully(2)
        val payloadLength = (lengthBytes[0].toInt() and 0xFF) or
            ((lengthBytes[1].toInt() and 0xFF) shl 8)
        if (payloadLength < 0 || payloadLength > 64 * 1024) {
            throw IllegalArgumentException("Invalid AirPods packet length: $payloadLength")
        }
        return header + lengthBytes + readFully(payloadLength)
    }

    fun startReader(
        scope: CoroutineScope,
        onPacket: (ByteArray) -> Unit,
        onError: (Throwable) -> Unit,
    ): Job = scope.launch(Dispatchers.IO) {
        try {
            while (isActive) {
                onPacket(readPacket())
            }
        } catch (error: Throwable) {
            if (isActive) onError(error)
        }
    }

    private fun readFully(size: Int): ByteArray {
        val result = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val read = input.read(result, offset, size - offset)
            if (read < 0) throw EOFException("AirPods transport closed while reading packet")
            offset += read
        }
        return result
    }
}
