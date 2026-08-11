package me.kavishdevar.librepods.data.apple

import kotlinx.serialization.Serializable
import me.kavishdevar.librepods.bluetooth.aacp.types.CapabilityEntry
import me.kavishdevar.librepods.bluetooth.aacp.types.ControlCommandIdentifier
import me.kavishdevar.librepods.bluetooth.aacp.types.CustomEq
import me.kavishdevar.librepods.bluetooth.aacp.types.MagicKeyType

@Serializable
data class AppleCache(
    val capabilities: Set<CapabilityEntry> = emptySet(),
    val magicKeys: Map<MagicKeyType, ByteArray> = emptyMap(),
    val controlStates: Map<ControlCommandIdentifier, ByteArray> = emptyMap(),
    val customEq: CustomEq = CustomEq(1, 50, 50, 50),
)
