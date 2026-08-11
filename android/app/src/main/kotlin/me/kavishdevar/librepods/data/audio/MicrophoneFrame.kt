package me.kavishdevar.librepods.data.audio

import me.kavishdevar.librepods.bluetooth.aacp.types.MessageOpcode
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class MicrophoneFrame(
    val timestamp: UInt,
    val accessUnit: ByteArray
) {
    companion object {
        fun parsePacket(packet: ByteArray): List<MicrophoneFrame> {
            if (packet.size < 22) {
                throw IllegalArgumentException("Microphone packet too short")
            }

            if (packet[4] != MessageOpcode.MICROPHONE_STREAM.value) {
                throw IllegalArgumentException("Not a microphoneState packet")
            }


            if (packet[6] != 0x01.toByte() || packet[7] != 0x00.toByte()) {
                return emptyList()
            }

            val frames = mutableListOf<MicrophoneFrame>()

            var offset = 22

            while (offset + 5 <= packet.size) {
                val timestamp = ByteBuffer
                    .wrap(packet, offset, 4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .int
                    .toUInt()

                val length = packet[offset + 4].toUByte().toInt()

                val start = offset + 5
                val end = start + length

                if (end > packet.size) {
                    break
                }

                frames += MicrophoneFrame(
                    timestamp = timestamp,
                    accessUnit = packet.copyOfRange(start, end)
                )

                offset = end
            }

            return frames
        }
    }
}
