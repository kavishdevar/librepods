package me.kavishdevar.librepods.bluetooth.aacp

import android.util.Log
import com.google.protobuf.ByteString
import me.kavishdevar.librepods.bluetooth.aacp.packet.AACPPacket
import me.kavishdevar.librepods.bluetooth.aacp.packet.RTBuddyPacket
import me.kavishdevar.librepods.bluetooth.aacp.rtbuddy.proto.SensorDataWX
import me.kavishdevar.librepods.bluetooth.aacp.rtbuddy.proto.SensorServiceSetting
import me.kavishdevar.librepods.bluetooth.aacp.rtbuddy.proto.SensorServiceType
import me.kavishdevar.librepods.bluetooth.aacp.types.SensorDataWxBuddyPayload
import me.kavishdevar.librepods.devices.PacketDestination
import kotlin.time.Duration

class RTBuddyManager(
    private val sendPacket: (AACPPacket) -> Boolean,
) {
    companion object {
        private const val TAG = "RTBuddyManager"
    }

    private var sequence = 0

    private fun nextSequence(): Int {
        val next = sequence
        sequence = (sequence + 1) and 0x7FFFFFFF
        return next
    }

    private fun observeSequence(data: SensorDataWX) {
        Log.d(TAG, "Received SensorDataWX seq=${data.seq}")
    }

    fun handlePacket(packet: RTBuddyPacket) {
        when (val payload = packet.rtBuddyPayload) {
            is SensorDataWxBuddyPayload -> {
                val data = payload.data
                observeSequence(data)
            }

            else -> {
                Log.d(
                    TAG,
                    "Unhandled RTBuddy payload: ${payload.descriptor}"
                )
            }
        }
    }

    fun sendSensorData(
        data: SensorDataWX,
    ): Boolean {
        val dataWithSequence = data.toBuilder()
            .setSeq(nextSequence())
            .build()

        val packet = RTBuddyPacket.create(
            rtBuddyPayload = SensorDataWxBuddyPayload(
                data = dataWithSequence,
            ),
            destination = PacketDestination.DEVICE,
        )

        return sendPacket(packet)
    }

    private fun sendSensorServiceSetting(
        service: SensorServiceType,
        configuration: ByteArray,
    ): Boolean {
        val data = SensorDataWX.newBuilder()
            .setServiceSettings(
                SensorServiceSetting.newBuilder()
                    .setService(service)
                    .setSetting(2)
                    .setConfiguration(
                        ByteString.copyFrom(configuration)
                    )
            )
            .build()

        return sendSensorData(data)
    }

    fun setSensorServiceReportInterval(sensorServiceType: SensorServiceType, interval: Duration): Boolean = sendSensorServiceSetting(
        service = sensorServiceType,
        configuration = byteArrayOf(0x01) + interval.toMicrosUINT32LE()
    )
}

private fun Duration.toMicrosUINT32LE(): ByteArray {
    val microseconds = this.inWholeMicroseconds
    return byteArrayOf(
        microseconds.toByte(),
        (microseconds shr 8).toByte(),
        (microseconds shr 16).toByte(),
        (microseconds shr 24).toByte()
    )
}
