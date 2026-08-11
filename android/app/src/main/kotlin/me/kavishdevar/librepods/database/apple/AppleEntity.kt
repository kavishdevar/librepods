package me.kavishdevar.librepods.database.apple

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import me.kavishdevar.librepods.data.apple.AppleCache
import me.kavishdevar.librepods.devices.AppleMetadata
import me.kavishdevar.librepods.devices.AppleSettings
import me.kavishdevar.librepods.bluetooth.MacAddress

@Entity
data class AppleEntity(
    @PrimaryKey
    val macAddress: MacAddress,

    val settings: AppleSettings,
    val metadata: AppleMetadata,
    val cache: AppleCache,
)
