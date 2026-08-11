package me.kavishdevar.librepods.bluetooth.aacp.types

enum class MagicKeyType(val value: Byte) {
    IRK(0x01), ENC_KEY(0x04);

    companion object {
        fun fromByte(byte: Byte): MagicKeyType = entries.find { it.value == byte }
            ?: throw IllegalArgumentException("Unknown MagicKeyType: $byte")
    }
}
