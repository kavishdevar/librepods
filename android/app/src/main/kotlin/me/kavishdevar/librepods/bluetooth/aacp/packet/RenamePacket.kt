package me.kavishdevar.librepods.bluetooth.aacp.packet

import me.kavishdevar.librepods.bluetooth.aacp.types.MessageOpcode
import me.kavishdevar.librepods.devices.PacketDestination

data class RenamePacket(
    val name: String,
    override val destination: PacketDestination = PacketDestination.DEVICE,
): AACPPacket {
    override val type: AACPPacketType = AACPPacketType.MESSAGE
    override val service: Byte = 0x04

    override val opcode = MessageOpcode.RENAME

    override val payload: ByteArray = byteArrayOf(0x01, name.length.toByte()) + name.toByteArray() + byteArrayOf(0x00)

    companion object {
        fun create(
            name: String,
            destination: PacketDestination = PacketDestination.DEVICE
        ): RenamePacket {
            return RenamePacket(
                name = name,
                destination = destination,
            )
        }
    }
}
