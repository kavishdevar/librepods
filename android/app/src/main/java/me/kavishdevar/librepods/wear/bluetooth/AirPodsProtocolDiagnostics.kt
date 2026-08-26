package me.kavishdevar.librepods.wear.bluetooth

/**
 * Small, dependency-free AACP frame decoder used by the Wear transport.
 * Unknown packet layouts are never guessed; they remain available as hex.
 */
object AirPodsProtocolDiagnostics {
    private val header = byteArrayOf(0x04, 0x00, 0x04, 0x00)

    data class Frame(
        val opcode: Int?,
        val payload: ByteArray,
        val raw: ByteArray,
    )

    data class BatteryComponent(
        val type: Component,
        val level: Int,
        val charging: Boolean,
        val connected: Boolean,
    )

    enum class Component(val wireValue: Int) { HEADSET(0x01), RIGHT(0x02), LEFT(0x04), CASE(0x08), UNKNOWN(-1) }

    fun isHeader(packet: ByteArray): Boolean =
        packet.size >= 4 && packet.copyOfRange(0, 4).contentEquals(header)

    fun decode(packet: ByteArray): Frame? {
        if (!isHeader(packet) || packet.size < 5) return null
        return Frame(packet[4].toInt() and 0xFF, packet.copyOfRange(5, packet.size), packet.copyOf())
    }

    /**
     * AACP battery response: header + opcode + reserved + count + N*5 bytes.
     * Each entry is type, 0x01, level/status, status, 0x01.
     * The layout is validated before returning anything to the UI layer.
     */
    fun parseBattery(packet: ByteArray): List<BatteryComponent>? {
        val frame = decode(packet) ?: return null
        if (frame.opcode != 0x04 || packet.size < 7) return null
        val count = packet[6].toInt() and 0xFF
        if (count > 3 || packet.size != 7 + 5 * count) return null

        val result = ArrayList<BatteryComponent>(count)
        repeat(count) { index ->
            val offset = 7 + index * 5
            if ((packet[offset + 1].toInt() and 0xFF) != 0x01 ||
                (packet[offset + 4].toInt() and 0xFF) != 0x01) return null
            val typeValue = packet[offset].toInt() and 0xFF
            val type = Component.entries.firstOrNull { it.wireValue == typeValue } ?: Component.UNKNOWN
            val level = (packet[offset + 2].toInt() and 0xFF).coerceIn(0, 100)
            val status = packet[offset + 3].toInt() and 0xFF
            result += BatteryComponent(
                type = type,
                level = level,
                charging = status == 0x01,
                connected = status != 0x04,
            )
        }
        return result
    }

    /** Ear-detection packets are fixed 8-byte AACP frames. 0x00 means in-ear. */
    fun parseEarDetection(packet: ByteArray): Pair<Boolean, Boolean>? {
        val frame = decode(packet) ?: return null
        if (frame.opcode != 0x06 || packet.size != 8) return null
        fun inEar(value: Int): Boolean? = when (value) {
            0x00 -> true
            0x01, 0x02 -> false
            else -> null
        }
        val left = inEar(packet[6].toInt() and 0xFF) ?: return null
        val right = inEar(packet[7].toInt() and 0xFF) ?: return null
        return left to right
    }

    fun opcodeName(opcode: Int?): String = when (opcode) {
        0x04 -> "BATTERY_INFO"
        0x06 -> "EAR_DETECTION"
        0x09 -> "CONTROL_COMMAND"
        0x0F -> "REQUEST_NOTIFICATIONS"
        0x17 -> "HEADTRACKING"
        0x19 -> "STEM_PRESS"
        0x1A -> "RENAME"
        0x1D -> "INFORMATION"
        0x2E -> "CONNECTED_DEVICES"
        0x30 -> "PROXIMITY_KEYS_REQ"
        0x31 -> "PROXIMITY_KEYS_RSP"
        0x4B -> "CONVERSATION_AWARENESS"
        0x4D -> "SET_FEATURE_FLAGS"
        0x53 -> "HEADPHONE_ACCOMMODATION"
        0x63 -> "CUSTOM_EQ"
        else -> "UNKNOWN"
    }

    fun hex(bytes: ByteArray, maxBytes: Int = 256): String =
        bytes.take(maxBytes).joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
}
