package me.kavishdevar.librepods.bluetooth.aacp.types

import me.kavishdevar.librepods.bluetooth.MacAddress

enum class AudioSourceType(val value: Byte) {
    NONE(0x00),
    CALL(0x01),
    MEDIA(0x02),
    UNKNOWN_1(0x04),
    UNKNOWN_2(0x06),
    UNKNOWN(-1);

    companion object {
        fun fromByte(byte: Byte): AudioSourceType = entries.find { it.value == byte }?: UNKNOWN
    }
}

data class AudioSource(
    val mac: MacAddress, val type: AudioSourceType
)
