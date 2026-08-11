package me.kavishdevar.librepods.bluetooth.aacp.packet

import me.kavishdevar.librepods.bluetooth.aacp.types.ControlCommand
import me.kavishdevar.librepods.bluetooth.aacp.types.MessageOpcode
import me.kavishdevar.librepods.devices.PacketDestination

data class ControlCommandPacket(
    val controlCommand: ControlCommand,
    override val destination: PacketDestination,
): AACPPacket {
    override val type: AACPPacketType = AACPPacketType.MESSAGE
    override val service: Byte = 0x04

    override val opcode = MessageOpcode.CONTROL_COMMAND

    override val payload: ByteArray = controlCommand.toAACPPayload()

    companion object {
        fun parse(
            data: ByteArray,
            destination: PacketDestination = PacketDestination.HOST
        ): ControlCommandPacket {
            val payload = if (data.toHexString().startsWith("040004000900")) {
                data.copyOfRange(6, data.size)
            } else data

            val controlCommand = ControlCommand.fromAACPPayload(payload)

            return ControlCommandPacket(controlCommand, destination)
        }

        fun create(
            controlCommand: ControlCommand,
            destination: PacketDestination = PacketDestination.DEVICE
        ): ControlCommandPacket {
            return ControlCommandPacket(
                controlCommand = controlCommand,
                destination = destination,
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ControlCommandPacket

        if (controlCommand != other.controlCommand) return false
        if (destination != other.destination) return false
        if (!rawPacket.contentEquals(other.rawPacket)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = controlCommand.hashCode()
        result = 31 * result + destination.hashCode()
        result = 31 * result + rawPacket.contentHashCode()
        return result
    }
}
