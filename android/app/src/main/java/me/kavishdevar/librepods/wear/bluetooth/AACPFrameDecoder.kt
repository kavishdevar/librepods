package me.kavishdevar.librepods.wear.bluetooth

/**
 * Reassembles AACP frames from an arbitrary L2CAP read stream.
 *
 * Bluetooth stream reads do not preserve protocol packet boundaries, so the
 * controller must never pass a partial or concatenated frame directly to the
 * AACP packet engine.
 */
class AACPFrameDecoder {
    private val pending = java.io.ByteArrayOutputStream()

    fun reset() {
        pending.reset()
    }

    fun append(data: ByteArray, length: Int = data.size): List<ByteArray> {
        require(length in 0..data.size)
        if (length == 0) return emptyList()
        pending.write(data, 0, length)

        val frames = ArrayList<ByteArray>()
        while (true) {
            val bytes = pending.toByteArray()
            if (bytes.size < HEADER_SIZE) break

            val frameLength = parseFrameLength(bytes)
            if (frameLength <= 0 || frameLength > MAX_FRAME_SIZE) {
                // Drop one byte and resynchronise instead of growing forever on
                // malformed input. Valid AACP frames start with a 4-byte header.
                pending.reset()
                pending.write(bytes, 1, bytes.size - 1)
                continue
            }
            if (bytes.size < frameLength) break

            frames += bytes.copyOf(frameLength)
            pending.reset()
            if (bytes.size > frameLength) {
                pending.write(bytes, frameLength, bytes.size - frameLength)
            }
        }
        return frames
    }

    private fun parseFrameLength(bytes: ByteArray): Int {
        // LibrePods AACP packets use the 4-byte transport header followed by
        // payload. The first two bytes encode payload length in little-endian.
        val payloadLength = (bytes[0].toInt() and 0xFF) or
            ((bytes[1].toInt() and 0xFF) shl 8)
        return HEADER_SIZE + payloadLength
    }

    companion object {
        private const val HEADER_SIZE = 4
        private const val MAX_FRAME_SIZE = 64 * 1024
    }
}
