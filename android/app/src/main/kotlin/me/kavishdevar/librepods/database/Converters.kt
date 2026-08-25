package me.kavishdevar.librepods.database

import androidx.room3.ColumnTypeConverter
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import me.kavishdevar.librepods.bluetooth.MacAddress
import me.kavishdevar.librepods.data.apple.AppleCache
import me.kavishdevar.librepods.devices.AppleMetadata
import me.kavishdevar.librepods.devices.AppleSettings
import kotlin.time.Instant

object Converters {
    val cbor = Cbor {
        ignoreUnknownKeys = true
    }

    @ColumnTypeConverter
    fun macAddressToString(mac: MacAddress): String = mac.value

    @ColumnTypeConverter
    fun stringToMacAddress(value: String): MacAddress = MacAddress(value)

    @ColumnTypeConverter
    fun appleSettingsToBytes(settings: AppleSettings): ByteArray =
        cbor.encodeToByteArray(settings)

    @ColumnTypeConverter
    fun bytesToAppleSettings(bytes: ByteArray): AppleSettings =
        cbor.decodeFromByteArray(bytes)

    @ColumnTypeConverter
    fun appleMetadataToBytes(metadata: AppleMetadata): ByteArray =
        cbor.encodeToByteArray(metadata)

    @ColumnTypeConverter
    fun bytesToAppleMetadata(bytes: ByteArray): AppleMetadata =
        cbor.decodeFromByteArray(bytes)

    @ColumnTypeConverter
    fun appleCacheToBytes(cache: AppleCache): ByteArray =
        cbor.encodeToByteArray(cache)

    @ColumnTypeConverter
    fun bytesToAppleCache(bytes: ByteArray): AppleCache =
        cbor.decodeFromByteArray(bytes)

    @ColumnTypeConverter
    fun kotlinInstantToLong(instant: Instant): Long =
        instant.toEpochMilliseconds()

    @ColumnTypeConverter
    fun longToKotlinInstant(millis: Long): Instant =
        Instant.fromEpochMilliseconds(millis)
}
