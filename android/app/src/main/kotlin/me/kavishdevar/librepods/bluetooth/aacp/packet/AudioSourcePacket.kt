package me.kavishdevar.librepods.bluetooth.aacp.packet

import me.kavishdevar.librepods.bluetooth.aacp.types.AudioSource
import me.kavishdevar.librepods.bluetooth.aacp.types.AudioSourceType
import me.kavishdevar.librepods.bluetooth.aacp.types.MessageOpcode
import me.kavishdevar.librepods.bluetooth.MacAddress
import me.kavishdevar.librepods.devices.PacketDestination

data class AudioSourcePacket(
    val audioSource: AudioSource,
    override val payload: ByteArray,
): AACPPacket {
    override val destination: PacketDestination = PacketDestination.HOST

    override val type: AACPPacketType = AACPPacketType.MESSAGE
    override val service: Byte = 0x04

    override val opcode = MessageOpcode.STEM_PRESS

    companion object {
        fun parse(
            packet: ByteArray,
        ): AudioSourcePacket {
            if (packet.size < 9) {
                throw IllegalArgumentException("Data array too short to parse Audio Source Response")
            }

            val payload = packet.copyOfRange(6, packet.size)
            val macAddress = MacAddress(payload.sliceArray(0..5).toHexString(HexFormat{ upperCase = true }).chunked(2).joinToString(":"))

            val typeByte = payload[6]
            val type = AudioSourceType.fromByte(typeByte)

            val audioSource = AudioSource(macAddress, type)

            return AudioSourcePacket(audioSource, payload)
        }
    }
}
