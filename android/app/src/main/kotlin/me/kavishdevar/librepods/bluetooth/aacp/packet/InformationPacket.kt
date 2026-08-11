package me.kavishdevar.librepods.bluetooth.aacp.packet

import android.util.Log
import me.kavishdevar.librepods.bluetooth.aacp.types.MessageOpcode
import me.kavishdevar.librepods.devices.AirPodsModel
import me.kavishdevar.librepods.devices.AirPodsSpecs
import me.kavishdevar.librepods.devices.AppleMetadata
import me.kavishdevar.librepods.devices.PacketDestination

private const val TAG = "InformationPacket"

data class InformationPacket(
    val metadata: AppleMetadata,
    override val payload: ByteArray
): AACPPacket {
    override val destination: PacketDestination = PacketDestination.HOST

    override val type: AACPPacketType = AACPPacketType.MESSAGE
    override val service: Byte = 0x04

    override val opcode = MessageOpcode.INFORMATION

    companion object {
        fun parse(
            packet: ByteArray,
        ): InformationPacket {
            val payload = packet.copyOfRange(6, packet.size)

            var index = 0
            while (index < payload.size && payload[index] != 0x00.toByte()) index++

            val strings = mutableListOf<String>()
            while (index < payload.size) {
                // skip 0x00 bytes
                while (index < payload.size && payload[index] == 0x00.toByte()) index++
                if (index >= payload.size) break
                val start = index
                // find next 0x00 byte
                while (index < payload.size && payload[index] != 0x00.toByte()) index++
                val str = payload.sliceArray(start until index).decodeToString()
                strings.add(str)
            }

            Log.i(TAG, "parse: strings: $strings")

            strings.removeAt(0)

            val model = AirPodsModel.fromModelNumber(strings.getOrNull(1)?: "A3063")

            return InformationPacket(
                metadata = AppleMetadata(
                    name = strings.getOrNull(0) ?: "",
                    model = model,
                    modelName = AirPodsSpecs.getSpec(model).displayName,
                    modelNumber = strings.getOrNull(1) ?: "",
                    manufacturer = strings.getOrNull(2) ?: "",
                    serialNumber = strings.getOrNull(3) ?: "",
                    leftSerialNumber = strings.getOrNull(8) ?: "",
                    rightSerialNumber = strings.getOrNull(9) ?: "",
                    version1 = strings.getOrNull(4) ?: "",
                    version2 = strings.getOrNull(5) ?: "",
                    version3 = strings.getOrNull(10) ?: "",
                    hardwareRevision = strings.getOrNull(6) ?: "",
                    updaterIdentifier = strings.getOrNull(7) ?: ""
                ),
                payload = payload
            )
        }
    }
}

