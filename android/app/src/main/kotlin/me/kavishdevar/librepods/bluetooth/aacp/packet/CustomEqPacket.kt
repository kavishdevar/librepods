package me.kavishdevar.librepods.bluetooth.aacp.packet

import me.kavishdevar.librepods.bluetooth.aacp.types.CustomEq
import me.kavishdevar.librepods.bluetooth.aacp.types.MessageOpcode
import me.kavishdevar.librepods.devices.PacketDestination


data class CustomEqPacket(
    val customEq: CustomEq,
    override val destination: PacketDestination
): AACPPacket {
    override val type: AACPPacketType = AACPPacketType.MESSAGE
    override val service: Byte = 0x04

    override val opcode = MessageOpcode.CONTROL_COMMAND

    override val payload: ByteArray = customEq.toAACPPayload()

    companion object {
        fun parse(
            packet: ByteArray,
            destination: PacketDestination = PacketDestination.HOST
        ): CustomEqPacket {
            val payload = packet.copyOfRange(6, packet.size)

            val length = payload[0].toInt()
            require(length == 5) { "Invalid length for CustomEqPacket: $length" }

            val state = payload[2].toInt()
            val low = payload[3].toInt()
            val mid = payload[4].toInt()
            val high = payload[5].toInt()

            val customEq = CustomEq(state, low, mid, high)

            return CustomEqPacket(customEq, destination)
        }

        // TODO: not all customeq messages are sending custom eq. there is one to request the current custom eq settings.
        //  this AACPPacket class only supports sending/receiving the custom eq settings, not other messages.
        fun create(
            customEq: CustomEq,
            destination: PacketDestination = PacketDestination.DEVICE
        ): CustomEqPacket {
            return CustomEqPacket(
                customEq = customEq,
                destination = destination,
            )
        }
    }
}
