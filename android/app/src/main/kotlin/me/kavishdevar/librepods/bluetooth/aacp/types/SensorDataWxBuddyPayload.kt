package me.kavishdevar.librepods.bluetooth.aacp.types

import me.kavishdevar.librepods.bluetooth.aacp.rtbuddy.proto.SensorDataWX

data class SensorDataWxBuddyPayload(
    val data: SensorDataWX,
): RTBuddyPayload {
    override val descriptor: RTBuddyDescriptor = RTBuddyDescriptor.SENSOR_DATA_WX
}
