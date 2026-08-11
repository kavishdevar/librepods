package me.kavishdevar.librepods.bluetooth.aacp.types

sealed interface Opcode { val value: Byte }

data class UnknownOpcode(override val value: Byte): Opcode

enum class ConnectOpcode(override val value: Byte): Opcode {
    SOMETHING (0x01);

    companion object {
        fun fromByte(value: Byte): Opcode =
            entries.firstOrNull { it.value == value }?: UnknownOpcode(value)
    }
}

enum class MessageOpcode(override val value: Byte): Opcode {
    CAPABILITIES_REQUEST(0x01),
    CAPABILITIES(0x02),
    BATTERY_INFO_REQUEST(0x03),
    BATTERY_INFO(0x04),
    EAR_DETECTION_REQUEST(0x05),
    EAR_DETECTION(0x06),
    BUD_ROLE_REQUEST(0x07),
    BUD_ROLE(0x08),
    CONTROL_COMMAND(0x09),
    DEVICE_LIST(0x0B),
    MAC_ADDRESS(0x0C),
    AUDIO_SOURCE_REQUEST(0x0D),
    AUDIO_SOURCE(0x0E),
    REQUEST_NOTIFICATIONS(0x0F),
    SMART_ROUTING(0x10),
    SMART_ROUTING_RESPONSE(0x11),
    EASY_PAIR_REQUEST(0x12),
    EASY_PAIR(0x13),
    CONNECT_PRIORITY_LIST(0x14),
    TRIANGLE_LINK_STATUS_REQUEST(0x15),
    TRIANGLE_LINK_STATUS(0x16),
    BUDDY_COMMAND(0x17),
    STEM_PRESS(0x19),
    RENAME(0x1A),
    TIMESTAMP(0x1B),
    INFORMATION(0x1D),
    EXTERNAL_ACCESSORY_SESSION(0x1E),
    SESSION_STATE(0x1F),
    REMOTE_FIRMWARE_AUTH(0x20),
    UNKNOWN_21(0x21),
    CASE_INFO_REQUEST(0x22),
    CASE_INFO(0x23),
    DEVICE_INFO(0x24),
    CERTIFICATES_REQUEST(0x26),
    CERTIFICATES(0x27),
    GYRO_INFO(0x28),
    SET_COUNTRY_CODE(0x29),
    STREAM_STATE_INFO(0x2B),
    GAPA_CHALLENGE(0x2C),
    CONNECTED_DEVICES_REQUEST(0x2D),
    CONNECTED_DEVICES(0x2E),
    MAGIC_KEYS_REQUEST(0x30),
    MAGIC_KEYS_RESPONSE(0x31),
    MAGIC_KEYS_2(0x32),
    UNKNOWN_40(0x40),
    SMART_ROUTING_V2_INFO(0x44),
    FAST_CONNECT_COMPLETE(0x45),
    BUD_SWAP_PROCEDURE(0x47),
    SWAP_IMMINENT_CONFIRM(0x48),
    BUD_SWAP_COMPLETE(0x49),
    SWAP_COMPLETE_CONFIRM(0x4A),
    CONVERSATION_AWARENESS(0x4B),
    ADAPTIVE_VOLUME(0x4C),
    SOURCE_FEATURE_CAPABILITIES(0x4D),
    FEATURE_PROXCARD_STATUS(0x4E),
    UARP_DATA(0x4F),
    PERF_STATS(0x50),
    SOURCE_CONTEXT(0x52),
    HEADPHONE_ACCOMMODATION(0x53),
    SET_BAND_EDGES(0x54),
    UNKNOWN_55(0x55),
    USB_SPATIAL_SENSOR_DATA_REQUEST(0x56),
    SLEEP_DETECTION_UPDATE(0x57),
    MICROPHONE_STREAM(0x58),
    DYNAMIC_END_OF_CHARGE(0x59),
    PERSONAL_TRANSLATION(0x60),
    SET_FEATURE_FLAGS(0x62),
    CUSTOM_EQ(0x63),
    APPLECARE(0x64);

    companion object {
        fun fromByte(value: Byte): Opcode =
            entries.firstOrNull { it.value == value }?: UnknownOpcode(value)
    }
}
