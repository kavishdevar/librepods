package me.kavishdevar.librepods.database.apple

import android.util.Log
import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import me.kavishdevar.librepods.data.apple.AppleCache
import me.kavishdevar.librepods.devices.AppleMetadata
import me.kavishdevar.librepods.devices.AppleSettings
import me.kavishdevar.librepods.bluetooth.MacAddress

private const val TAG = "AppleDao"

@Dao
interface AppleDao {

    @Query("SELECT * FROM AppleEntity WHERE macAddress = :macAddress")
    suspend fun get(macAddress: MacAddress): AppleEntity?

    @Query("SELECT * FROM AppleEntity")
    suspend fun getAll(): List<AppleEntity>

    @Upsert
    suspend fun upsert(device: AppleEntity)

    suspend fun updateSettings(macAddress: MacAddress, settings: AppleSettings) {
        Log.d(TAG, "Updating settings for $macAddress: $settings")
        val device = get(macAddress)?: AppleEntity(macAddress = macAddress, settings = settings, metadata = AppleMetadata(), cache = AppleCache())
            upsert(device.copy(settings = settings))
    }

    suspend fun updateMetadata(macAddress: MacAddress, metadata: AppleMetadata) {
        Log.d(TAG, "Updating metadata for $macAddress: $metadata")
        val device = get(macAddress)?: AppleEntity(macAddress = macAddress, settings = AppleSettings(), metadata = metadata, cache = AppleCache())
            upsert(device.copy(metadata = metadata))
    }

    suspend fun saveCache(macAddress: MacAddress, cache: AppleCache) {
        Log.d(TAG, "Saving cache for $macAddress: $cache")
        val device = get(macAddress)?: AppleEntity(macAddress = macAddress, settings = AppleSettings(), metadata = AppleMetadata(), cache = cache)
            upsert(device.copy(cache = cache))
    }
}
