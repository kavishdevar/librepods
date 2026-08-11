package me.kavishdevar.librepods.bluetooth.aacp.packet

import android.util.Log
import me.kavishdevar.librepods.bluetooth.aacp.types.MessageOpcode
import me.kavishdevar.librepods.devices.ComponentStatus
import me.kavishdevar.librepods.devices.DeviceComponent
import me.kavishdevar.librepods.devices.DeviceComponentState
import me.kavishdevar.librepods.devices.PacketDestination

private const val TAG = "EarDetectionResponsePacket"

data class EarDetectionResponsePacket(
    val componentStates: Set<DeviceComponentState>,
    val isLeftPrimary: Boolean
): AACPPacket {
    override val destination: PacketDestination = PacketDestination.HOST

    override val type: AACPPacketType = AACPPacketType.MESSAGE
    override val service: Byte = 0x04

    override val opcode = MessageOpcode.EAR_DETECTION

    override val payload: ByteArray = byteArrayOf(
        componentStates.first { it.component == DeviceComponent.LEFT }.status.toAirPodsByte(),
        componentStates.first { it.component == DeviceComponent.RIGHT }.status.toAirPodsByte()
    ).let {
        if (isLeftPrimary) it else it.reversedArray()
    }

    companion object {
        fun parse(
            packet: ByteArray,
            isLeftPrimary: Boolean
        ): EarDetectionResponsePacket {
            val payload = packet.copyOfRange(6, packet.size)
            Log.d(
                TAG,
                "parsing Ear Detection Response: ${packet.joinToString(" ") { "%02X".format(it) }}"
            )

            val primaryStatus = payload[0]
            val secondaryStatus = payload[1]

            val componentStates = mutableSetOf<DeviceComponentState>()

            if (isLeftPrimary) {
                componentStates.addAll(
                    listOf(
                        DeviceComponentState(
                            DeviceComponent.LEFT,
                            ComponentStatus.fromAirPodsByte(primaryStatus)
                        ),
                        DeviceComponentState(
                            DeviceComponent.RIGHT,
                            ComponentStatus.fromAirPodsByte(secondaryStatus)
                        )
                    )
                )
            } else {
                componentStates.addAll(
                    listOf(
                        DeviceComponentState(
                            DeviceComponent.LEFT,
                            ComponentStatus.fromAirPodsByte(secondaryStatus)
                        ),
                        DeviceComponentState(
                            DeviceComponent.RIGHT,
                            ComponentStatus.fromAirPodsByte(primaryStatus)
                        )
                    )
                )
            }

            Log.i(TAG, "parsed Ear Detection Response: $componentStates")

            return EarDetectionResponsePacket(componentStates, isLeftPrimary)
        }
    }
}
