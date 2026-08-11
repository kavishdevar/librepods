package me.kavishdevar.librepods.bluetooth.aacp.types

enum class StemPressType(val value: Byte) {
    SINGLE_PRESS(0x05), DOUBLE_PRESS(0x06), TRIPLE_PRESS(0x07), LONG_PRESS(0x08);

    companion object {
        fun fromByte(byte: Byte): StemPressType? = entries.find { it.value == byte }
    }
}

// TODO: make DeviceComponent, BatteryComponent, and StemPressBud the same with helpers to parse from byte for specific messages
enum class StemPressBud(val value: Byte) {
    LEFT(0x01), RIGHT(0x02);

    companion object {
        fun fromByte(byte: Byte): StemPressBud? = entries.find { it.value == byte }
    }
}
