package me.kavishdevar.librepods.repository

import android.util.Log
import me.kavishdevar.librepods.bluetooth.MacAddress
import me.kavishdevar.librepods.data.apple.AppleCache
import me.kavishdevar.librepods.database.apple.AppleDao
import me.kavishdevar.librepods.database.apple.AppleEntity
import me.kavishdevar.librepods.devices.AppleMetadata
import me.kavishdevar.librepods.devices.AppleSettings
import me.kavishdevar.librepods.devices.AppleState

private const val TAG = "AppleRepository"
class AppleRepository(
    private val dao: AppleDao,
) {
    suspend fun load(macAddress: MacAddress): AppleEntity? {
        Log.d(TAG, "Loading AppleEntity for $macAddress")
        val entity = dao.get(macAddress)
        Log.i(TAG, "Loaded AppleEntity for $macAddress: $entity")
        return entity
    }

    suspend fun saveSettings(macAddress: MacAddress, settings: AppleSettings) {
        Log.d(TAG, "Saving AppleSettings for $macAddress: $settings")
        dao.updateSettings(macAddress, settings)
        Log.i(TAG, "Saved AppleSettings for $macAddress: $settings")
    }

    suspend fun saveMetadata(macAddress: MacAddress, metadata: AppleMetadata) {
        Log.d(TAG, "Saving AppleMetadata for $macAddress: $metadata")
        dao.updateMetadata(macAddress, metadata)
        Log.i(TAG, "Saved AppleMetadata for $macAddress: $metadata")
    }

    suspend fun saveCache(macAddress: MacAddress, cache: AppleCache) {
        Log.d(TAG, "Saving AppleCache for $macAddress: $cache")
        dao.saveCache(macAddress, cache)
        Log.i(TAG, "Saved AppleCache for $macAddress: $cache")
    }

    suspend fun saveCacheFromState(macAddress: MacAddress, state: AppleState) {
        Log.d(TAG, "Saving AppleCache from AppleState for ${macAddress.toRedactedString()}: $state")
        val cache = try {
            AppleCache(
                capabilities = state.capabilities,
                magicKeys = state.magicKeys,
                controlStates = state.controlStates,
                customEq = state.customEq
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create AppleCache from AppleState for ${macAddress.toRedactedString()}: $state", e)
            return
        }

        saveCache(macAddress, cache)
        Log.i(TAG, "Saved AppleCache from AppleState for ${macAddress.toRedactedString()}: $cache")
    }
}
