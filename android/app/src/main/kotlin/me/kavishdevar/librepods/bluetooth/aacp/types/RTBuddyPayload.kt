package me.kavishdevar.librepods.bluetooth.aacp.types

sealed interface RTBuddyPayload {
    val descriptor: RTBuddyDescriptor
}

data class UnknownBuddyPayload(
    override val descriptor: RTBuddyDescriptor,
    val descriptorValue: UInt,
    val data: ByteArray,
) : RTBuddyPayload
