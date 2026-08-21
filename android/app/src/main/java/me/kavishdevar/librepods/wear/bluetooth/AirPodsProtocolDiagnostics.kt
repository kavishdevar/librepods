package me.kavishdevar.librepods.wear.bluetooth

/**
 * Small, dependency-free AACP frame decoder used by the Wear transport.
 * It deliberately does not invent values for unknown packet layouts: unknown
 * frames are surfaced as hex so they can be decoded from real AirPods traffic.
 */
object AirPodsProtocolDiagnostics {
    private val header = byteArrayOf(0x04, 0x00, 0x04, 0x00)

    data class Frame(
        val opcode: Int?,
        val payload: ByteArray,
        val raw: ByteArray,
    )

    fun isHeader(packet: ByteArray): Boolean =
        packet.size >= 4 && packet.copyOfRange(0, 4).contentEquals(header)

    fun decode(packet: ByteArray): Frame? {
        if (!isHeader(packet) || packet.size < 5) return null
        return Frame(packet[4].toInt() and 0xFF, packet.copyOfRange(5, packet.size), packet.copyOf())
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
