package me.kavishdevar.librepods.bluetooth.aacp.packet

import me.kavishdevar.librepods.bluetooth.aacp.rtbuddy.proto.SensorDataWX
import me.kavishdevar.librepods.bluetooth.aacp.types.MessageOpcode
import me.kavishdevar.librepods.bluetooth.aacp.types.Opcode
import me.kavishdevar.librepods.bluetooth.aacp.types.RTBuddyDescriptor
import me.kavishdevar.librepods.bluetooth.aacp.types.RTBuddyPayload
import me.kavishdevar.librepods.bluetooth.aacp.types.SensorDataWxBuddyPayload
import me.kavishdevar.librepods.bluetooth.aacp.types.UnknownBuddyPayload
import me.kavishdevar.librepods.devices.PacketDestination

data class RTBuddyPacket(
    val rtBuddyPayload: RTBuddyPayload,

    override val payload: ByteArray,
    override val destination: PacketDestination,
) : AACPPacket {

    override val type: AACPPacketType = AACPPacketType.MESSAGE
    override val service: Byte = 0x04
    override val opcode: Opcode = MessageOpcode.BUDDY_COMMAND

    companion object {
        fun parse(
            data: ByteArray,
            destination: PacketDestination = PacketDestination.HOST,
        ): RTBuddyPacket {
            val payload = if (
                data.size >= 6 &&
                data[0] == AACPPacketType.MESSAGE.value &&
                data[4] == MessageOpcode.BUDDY_COMMAND.value
            ) {
                data.copyOfRange(6, data.size)
            } else {
                data
            }

            require(payload.size >= 6) {
                "RTBuddy packet is too short: ${payload.size}"
            }

            val descriptorValue =
                (payload[0].toUInt() and 0xFFu) or
                    ((payload[1].toUInt() and 0xFFu) shl 8) or
                    ((payload[2].toUInt() and 0xFFu) shl 16) or
                    ((payload[3].toUInt() and 0xFFu) shl 24)

            val length =
                (payload[4].toInt() and 0xFF) or
                    ((payload[5].toInt() and 0xFF) shl 8)

            require(payload.size >= 6 + length) {
                "RTBuddy payload truncated: expected $length bytes, got ${payload.size - 6}"
            }

            val descriptor = RTBuddyDescriptor.fromValue(descriptorValue)
            val data = payload.copyOfRange(6, 6 + length)

            val rtBuddyPayload: RTBuddyPayload = when (descriptor) {
                RTBuddyDescriptor.SENSOR_DATA_WX -> {
                    SensorDataWxBuddyPayload(
                        data = SensorDataWX.parseFrom(data),
                    )
                }

                else -> {
                    UnknownBuddyPayload(
                        descriptor = descriptor,
                        descriptorValue = descriptorValue,
                        data = data,
                    )
                }
            }

            return RTBuddyPacket(
                rtBuddyPayload = rtBuddyPayload,
                payload = payload,
                destination = destination,
            )
        }
        fun create(
            rtBuddyPayload: RTBuddyPayload,
            destination: PacketDestination = PacketDestination.DEVICE,
        ): RTBuddyPacket {
            val data = when (rtBuddyPayload) {
                is SensorDataWxBuddyPayload ->
                    rtBuddyPayload.data.toByteArray()

                is UnknownBuddyPayload ->
                    rtBuddyPayload.data
            }

            val descriptorValue = when (rtBuddyPayload) {
                is SensorDataWxBuddyPayload ->
                    rtBuddyPayload.descriptor.value

                is UnknownBuddyPayload ->
                    rtBuddyPayload.descriptorValue
            }

            require(data.size <= 0xFFFF) {
                "RTBuddy payload too large: ${data.size}"
            }

            val payload = byteArrayOf(
                (descriptorValue and 0xFFu).toByte(),
                ((descriptorValue shr 8) and 0xFFu).toByte(),
                ((descriptorValue shr 16) and 0xFFu).toByte(),
                ((descriptorValue shr 24) and 0xFFu).toByte(),
                (data.size and 0xFF).toByte(),
                ((data.size shr 8) and 0xFF).toByte(),
            ) + data

            return RTBuddyPacket(
                rtBuddyPayload = rtBuddyPayload,
                payload = payload,
                destination = destination,
            )
        }
    }
}
