package me.kavishdevar.librepods.bluetooth.aacp.types

import kotlinx.serialization.Serializable

// TODO: use a map instead, this is inefficient

@Serializable
data class CapabilityEntry(
    val capability: Capability,
    val value: ByteArray
)

@Serializable
enum class Capability(
    val value: Byte,
    val valueSize: Int = 1
) {
    UNKNOWN_01(0x01),
    UNKNOWN_02(0x02),
    UNKNOWN_03(0x03, 4),
    SELECTIVE_SPEECH_LISTENING(0x04, 4),
    ENHANCED_TRANSPARENCY_VERSION(0x06, 4),
    UNKNOWN_07(0x07, 4),
    UNKNOWN_09(0x09),
    UNKNOWN_0A(0x0A),
    UNKNOWN_0B(0x0B),
    UNKNOWN_0F(0x0F),
    UNKNOWN_10(0x10),
    PERSONAL_MEDICAL_EQUIPMENT(0x11),
    CASE_SOUND(0x12),
    HIDE_OFF_LISTENING_MODE(0x13),
    UNKNOWN_14(0x14),
    SIRI_MULTITONE(0x15),
    HIDE_EAR_DETECTION(0x16),
    EAR_TIP_FIT_TEST(0x17),
    AUTO_ANC(0x18),
    UNKNOWN_PAUSE_MEDIA_ON_SLEEP(0x19),
    WIRED_LOSSLESS_AUDIO(0x20),
    SLEEP_DETECTION(0x21),
    HEARING_AID(0x22),
    CAMERA_CONTROL(0x23),
    OVAD_STREAMING(0x24),
    FAR_FIELD_UPLINK(0x25),
    HEART_RATE_MONITOR(0x26),
    HEARING_PROTECTION_PPE(0x28),
    DYNAMIC_END_OF_CHARGE(0x29),
    HEARING_PROTECTION(0x30, 4),
    HEARING_AID_V2(0x31),
    UNKNOWN_WIRED_LOSSLESS_AUDIO_2(0x34),
    SMART_ROUTING_VERSION(0x35),
    FAR_FIELD_UPLINK_MODERN(0x36),
    PREFERENCE_EQ(0x37),
    EXTENDED_CLICK_HOLD(0x38),
    UNKNOWN_SPATIAL_AUDIO_SUPPORT(0x40),
    UNKNOWN_CALL_MANAGEMENT(0x50),
    UNKNOWN_60(0x60),
    UNKNOWN_ADAPTIVE_VOLUME(0x90.toByte()),
    UNKNOWN_A0(0xA0.toByte()),
    UNKNOWN_AUTO_ANC_2(0xB0.toByte()),
    UNKNOWN_HEARING_AID_2(0xC0.toByte()),
    HEARING_TEST(0xD0.toByte()),
    UNKNOWN_SENSOR_DATA_2(0xE0.toByte()),
    BOBBLE(0xF0.toByte());

    companion object {
        private val map = entries.associateBy(Capability::value)

        fun fromByte(value: Byte): Capability? = map[value]
    }
}
