package me.kavishdevar.librepods.bluetooth.aacp.types

import me.kavishdevar.librepods.bluetooth.MacAddress

data class ConnectedDevice(
    val macAddress: MacAddress, val info1: Byte, val info2: Byte, var type: String?
)
