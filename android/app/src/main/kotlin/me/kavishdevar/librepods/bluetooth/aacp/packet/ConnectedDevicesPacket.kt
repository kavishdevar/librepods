package me.kavishdevar.librepods.bluetooth.aacp.packet

import android.util.Log
import me.kavishdevar.librepods.bluetooth.aacp.types.ConnectedDevice
import me.kavishdevar.librepods.bluetooth.aacp.types.MessageOpcode
import me.kavishdevar.librepods.bluetooth.MacAddress
import me.kavishdevar.librepods.devices.PacketDestination

private const val TAG = "ConnectedDevicesPacket"

data class ConnectedDevicesPacket(
    val connectedDevices: List<ConnectedDevice>,
    override val payload: ByteArray,
): AACPPacket {
    override val destination: PacketDestination = PacketDestination.HOST

    override val type: AACPPacketType = AACPPacketType.MESSAGE
    override val service: Byte = 0x04

    override val opcode = MessageOpcode.CONNECTED_DEVICES

    companion object {
        fun parse(
            packet: ByteArray,
        ): ConnectedDevicesPacket {
            if (packet.size < 8) {
                throw IllegalArgumentException("Data array too short to parse Connected Devices Response")
            }

            val payload = packet.copyOfRange(6, packet.size)

            val deviceCount = payload[2].toInt()
            val devices = mutableListOf<ConnectedDevice>()

            var offset = 3

            for (i in 0 until deviceCount) {
                if (offset + 8 > payload.size) {
                    Log.w(
                        TAG,
                        "Data array too short to parse all connected devices, returning what we have"
                    )
                    break
                }

                val macAddress = MacAddress(payload.sliceArray(offset until offset + 6).toHexString(HexFormat{ upperCase = true }).chunked(2).joinToString(":"))

                val info1 = payload[offset + 6]
                val info2 = payload[offset + 7]

                val existingDevice = devices.find { it.macAddress == macAddress }
                devices.add(ConnectedDevice(macAddress, info1, info2, existingDevice?.type))
                offset += 8
            }

            return ConnectedDevicesPacket(devices, payload)
        }
    }
}
