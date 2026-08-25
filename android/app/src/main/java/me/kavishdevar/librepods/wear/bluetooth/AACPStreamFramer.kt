package me.kavishdevar.librepods.wear.bluetooth

/**
 * Conservative AACP stream framer.
 *
 * Bluetooth reads are not packet boundaries. We only emit frames whose
 * lengths are verified by the currently implemented protocol layouts.
 * Unknown frames stay buffered until a verified frame can be extracted;
 * their bytes are never assigned a guessed length.
 */
class AACPStreamFramer {
    private val header = byteArrayOf(0x04, 0x00, 0x04, 0x00)
    private val handshakeAck = byteArrayOf(0x01, 0x00, 0x04, 0x00)
    private var buffer = ByteArray(0)

    fun reset() {
        buffer = ByteArray(0)
    }

    fun append(bytes: ByteArray, onFrame: (ByteArray) -> Unit) {
        if (bytes.isEmpty()) return
        buffer += bytes
        extract(onFrame)
    }

    private fun extract(onFrame: (ByteArray) -> Unit) {
        while (buffer.isNotEmpty()) {
            val length = verifiedLength(buffer) ?: return
            if (length <= 0 || length > buffer.size) return
            onFrame(buffer.copyOfRange(0, length))
            buffer = buffer.copyOfRange(length, buffer.size)
        }
    }

    private fun verifiedLength(data: ByteArray): Int? {
        if (data.size >= 4 && data.copyOfRange(0, 4).contentEquals(handshakeAck)) return 4
        if (data.size < 5) return null
        if (!data.copyOfRange(0, 4).contentEquals(header)) {
            // Do not invent a boundary for an unknown prefix. Keep it for
            // diagnostics instead of feeding a partial frame to AACPManager.
            return null
        }

        return when (data[4].toInt() and 0xFF) {
            0x04 -> {
                if (data.size < 7) return null
                val count = data[6].toInt() and 0xFF
                if (count > 3) return null
                val length = 7 + count * 5
                if (data.size >= length) length else null
            }
            0x06 -> if (data.size >= 8) 8 else null
            else -> null
        }
    }

    /** Bytes that are still buffered and intentionally not framed. */
    fun bufferedBytes(): ByteArray = buffer.copyOf()
}
