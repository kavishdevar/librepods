package me.kavishdevar.librepods.bluetooth.aacp.packet

import me.kavishdevar.librepods.bluetooth.aacp.types.Opcode
import me.kavishdevar.librepods.devices.PacketDestination

data class AACPPacketType(val value: Byte) {
    companion object {
        val CONNECT = AACPPacketType(0x00)
        val CONNECT_RESPONSE = AACPPacketType(0x01)
        val DISCONNECT = AACPPacketType(0x02)
        val DISCONNECT_RESPONSE = AACPPacketType(0x03)
        val MESSAGE = AACPPacketType(0x04)

        fun fromByte(value: Byte): AACPPacketType {
            return when (value) {
                0x00.toByte() -> CONNECT
                0x01.toByte() -> CONNECT_RESPONSE
                0x02.toByte() -> DISCONNECT
                0x03.toByte() -> DISCONNECT_RESPONSE
                0x04.toByte() -> MESSAGE
                else -> AACPPacketType(value)
            }
        }
    }
}

sealed interface AACPPacket {
    /**
    * The entire packet; contains the opcode and header
    */
    val rawPacket: ByteArray
        get() = byteArrayOf(type.value, 0x00, service, 0x00, opcode.value, 0x00) + payload

    val destination: PacketDestination

    val type: AACPPacketType

    val service: Byte

    val opcode: Opcode

    val payload: ByteArray

    companion object {
        fun createUnknownPacket(
            opcode: Opcode,
            payload: ByteArray,
            destination: PacketDestination = PacketDestination.DEVICE,
            type: AACPPacketType = AACPPacketType.MESSAGE,
            service: Byte = 0x04,
        ): AACPPacket {
            return UnknownAACPPacket(
                service = service,
                type = type,
                opcode = opcode,
                payload = payload,
                destination = destination
            )
        }
    }
}

data class UnknownAACPPacket (
    override val service: Byte = 0x04,
    override val type: AACPPacketType = AACPPacketType.MESSAGE,
    override val opcode: Opcode,
    override val payload: ByteArray,
    override val destination: PacketDestination
): AACPPacket
