package me.kavishdevar.librepods.bluetooth.aacp.types

data class ControlCommand(
    val identifier: ControlCommandIdentifier, val value: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ControlCommand

        if (identifier != other.identifier) return false
        if (!value.contentEquals(other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        var result: Int = identifier.value.toInt()
        result = 31 * result + value.contentHashCode()
        return result
    }

    companion object {
        fun fromAACPPayload(data: ByteArray): ControlCommand {
            val identifier = ControlCommandIdentifier.fromByte(data[0])
                ?: throw IllegalArgumentException("Unknown ControlCommandIdentifier: ${data[0].toHexString()}")

            val value = data.copyOfRange(1, data.size)
            val trimmed = value.dropLastWhile { it == 0x00.toByte() }.toByteArray()
            return ControlCommand(identifier, if (trimmed.isEmpty()) byteArrayOf(0x00) else trimmed)
        }
    }

    fun toAACPPayload(): ByteArray {
        val payload = ByteArray(5)
        payload[0] = this.identifier.value
        System.arraycopy(this.value, 0, payload, 1, this.value.size.coerceAtMost(4))
        return payload
    }
}


enum class ControlCommandIdentifier(val value: Byte) {
    MIC_MODE(0x01),
    SCAN(0x02),
    RESET(0x03),
    BASIC_DOUBLE_TAP_MODE(0x04),
    BUTTON_SEND_MODE(0x05),
    OWNS_CONNECTION(0x06),
    TAP_INTERVAL(0x07),
    BUD_ROLE(0x08),
    DEBUG_GET_DATA(0x09),
    EAR_DETECTION_CONFIG(0x0A),
    JITTER_BUFFER(0x0B),
    DOUBLE_TAP_MODE(0x0C),
    LISTENING_MODE(0x0D),
    HEART_RATE_MONITOR_1(0x0E),
    HEART_RATE_MONITOR_2(0x0F),
    UNKNOWN10(0x10),
    SWITCH_CONTROL(0x11),
    VOICE_TRIGGER(0x12),
    DOAP_MODE(0x13),
    SINGLE_CLICK_MODE(0x14),
    DOUBLE_CLICK_MODE(0x15),
    CLICK_HOLD_MODE(0x16),
    DOUBLE_CLICK_INTERVAL(0x17),
    CLICK_HOLD_INTERVAL(0x18),
    UNKNOWN19(0x19),
    LISTENING_MODE_CONFIGS(0x1A),
    ONE_BUD_ANC_MODE(0x1B),
    CROWN_ROTATION_DIRECTION(0x1C),
    UNKNOWN1D(0x1D),
    AUTO_ANSWER_MODE(0x1E),
    CHIME_VOLUME(0x1F),
    SMART_ROUTING_MODE(0x20),
    UNKNOWN21(0x21),
    HFP_UPLINK_MODE(0x22),
    VOLUME_SWIPE_INTERVAL(0x23),
    CALL_MANAGEMENT_CONFIG(0x24),
    VOLUME_SWIPE_MODE(0x25),
    ADAPTIVE_VOLUME_CONFIG(0x26),
    SOFTWARE_MUTE_CONFIG(0x27),
    CONVERSATION_DETECT_CONFIG(0x28),
    SSL(0x29),
    UNKNOWN2A(0x2A),
    UNKNOWN2B(0x2B),
    HEARING_AID(0x2C),
    UNKNOWN2D(0x2D),
    AUTO_ANC_STRENGTH(0x2E),
    HPS_GAIN_SWIPE(0x2F),
    HRM_STATE(0x30),
    IN_CASE_TONE_CONFIG(0x31),
    SIRI_MULTITONE_CONFIG(0x32),
    HEARING_ASSIST_CONFIG(0x33),
    ALLOW_OFF_OPTION(0x34),
    SLEEP_DETECTION_CONFIG(0x35),
    ALLOW_AUTO_CONNECT(0x36),
    PPE_TOGGLE_CONFIG(0x37),
    PPE_CAP_LEVEL_CONFIG(0x38),
    RAW_GESTURES_CONFIG(0x39),
    ALLOW_TEMPORARY_MANAGED_PAIRING(0x3A),
    DYNAMIC_END_OF_CHARGE(0x3B),
    SYSTEM_SIRI_MODE(0x3C),
    HEARING_AID_GENERIC(0x3D),
    UPLINK_EQ_BUD(0x3E),
    UPLINK_EQ_SOURCE(0x3F),
    IN_CASE_TONE_VOLUME(0x40),
    DISABLE_BUTTON_INPUT(0x41),
    EXTENDED_HOLD_AND_RELEASE(0x42);

    companion object {
        fun fromByte(byte: Byte): ControlCommandIdentifier? = entries.find { it.value == byte }
    }
}
