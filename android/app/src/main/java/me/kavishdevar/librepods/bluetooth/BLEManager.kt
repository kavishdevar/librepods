/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package me.kavishdevar.librepods.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.edit
import me.kavishdevar.librepods.utils.BluetoothCryptography
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.collections.iterator
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Manager for Bluetooth Low Energy scanning operations specifically for AirPods
 */
@OptIn(ExperimentalEncodingApi::class)
class BLEManager(private val context: Context) {

    data class AirPodsStatus(
        val address: String,
        val lastSeen: Long = System.currentTimeMillis(),
        val paired: Boolean = false,
        val model: String = "Unknown",
        val leftBattery: Int? = null,
        val rightBattery: Int? = null,
        val caseBattery: Int? = null,
        /** Wall-clock time when the case value itself was seen, not merely replayed from cache. */
        val caseBatteryObservedAt: Long? = null,
        val caseBatteryIsCached: Boolean = false,
        val isLeftInEar: Boolean = false,
        val isRightInEar: Boolean = false,
        val isLeftCharging: Boolean = false,
        val isRightCharging: Boolean = false,
        val isCaseCharging: Boolean = false,
        /** True when levels came from the decrypted, one-percent-resolution payload. */
        val hasExactBatteryData: Boolean = false,
        val lidOpen: Boolean = false,
        val color: String = "Unknown",
        val connectionState: String = "Unknown"
    )

    fun getMostRecentStatus(): AirPodsStatus? {
        return deviceStatusMap.values.maxByOrNull { it.lastSeen }
    }

    interface AirPodsStatusListener {
        fun onDeviceStatusChanged(device: AirPodsStatus, previousStatus: AirPodsStatus?)
        fun onBroadcastFromNewAddress(device: AirPodsStatus)
        fun onLidStateChanged(lidOpen: Boolean)
        fun onEarStateChanged(device: AirPodsStatus, leftInEar: Boolean, rightInEar: Boolean)
        fun onBatteryChanged(device: AirPodsStatus)
        /** Refreshes battery confidence without forcing downstream UI publication. */
        fun onBatteryObserved(device: AirPodsStatus) = Unit
        fun onDeviceDisappeared()
    }

    private var mBluetoothLeScanner: BluetoothLeScanner? = null
    private var mScanCallback: ScanCallback? = null
    private var airPodsStatusListener: AirPodsStatusListener? = null
    private val deviceStatusMap = mutableMapOf<String, AirPodsStatus>()
    private val verifiedAddresses = mutableSetOf<String>()
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private var currentGlobalLidState: Boolean? = null
    private var lastBroadcastTime: Long = 0

    private data class RememberedCaseBattery(val level: Int, val observedAt: Long)

    private val lastValidCaseBatteryMap = mutableMapOf<String, RememberedCaseBattery>()
    private val loadedCaseBatteryAddresses = mutableSetOf<String>()
    private val lastAdvertisementMap = mutableMapOf<String, CachedAdvertisement>()

    private var cachedEncryptionKeyBase64: String? = null
    private var cachedEncryptionKey: ByteArray? = null
    private var decryptionCipher: Cipher? = null
    private var cachedIrkBase64: String? = null
    private var cachedIrk: ByteArray? = null

    private data class CachedAdvertisement(
        val manufacturerData: ByteArray,
        val encryptionKeyBase64: String?
    )
    private val modelNames = mapOf(
        0x0E20 to "AirPods Pro",
        0x1420 to "AirPods Pro 2",
        0x2420 to "AirPods Pro 2 (USB-C)",
        0x2520 to "AirPods Pro 3",
        0x2620 to "AirPods Pro 3 (USB-C)",
        0x2720 to "AirPods Pro 3",
        0x0220 to "AirPods 1",
        0x0F20 to "AirPods 2",
        0x1320 to "AirPods 3",
        0x1920 to "AirPods 4",
        0x1B20 to "AirPods 4 (ANC)",
        0x0A20 to "AirPods Max",
        0x1F20 to "AirPods Max (USB-C)"
    )

    val colorNames = mapOf(
        0x00 to "White", 0x01 to "Black", 0x02 to "Red", 0x03 to "Blue",
        0x04 to "Pink", 0x05 to "Gray", 0x06 to "Silver", 0x07 to "Gold",
        0x08 to "Rose Gold", 0x09 to "Space Gray", 0x0A to "Dark Blue",
        0x0B to "Light Blue", 0x0C to "Yellow"
    )

    val connStates = mapOf(
        0x00 to "Disconnected", 0x04 to "Idle", 0x05 to "Music",
        0x06 to "Call", 0x07 to "Ringing", 0x09 to "Hanging Up", 0xFF to "Unknown"
    )

    private val cleanupHandler = Handler(Looper.getMainLooper())
    private val cleanupRunnable = object : Runnable {
        override fun run() {
            cleanupStaleDevices()
            checkLidStateTimeout()
            cleanupHandler.postDelayed(this, CLEANUP_INTERVAL_MS)
        }
    }

    fun setAirPodsStatusListener(listener: AirPodsStatusListener) {
        airPodsStatusListener = listener
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        try {
            Log.d(TAG, "Starting BLE scanner")

            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val btAdapter = btManager.adapter

            if (btAdapter == null) {
                Log.d(TAG, "No Bluetooth adapter available")
                return
            }

            if (mBluetoothLeScanner != null && mScanCallback != null) {
                mBluetoothLeScanner?.stopScan(mScanCallback)
                mScanCallback = null
            }

            if (!btAdapter.isEnabled) {
                Log.d(TAG, "Bluetooth is disabled")
                return
            }

            mBluetoothLeScanner = btAdapter.bluetoothLeScanner

            val scanSettings = ScanSettings.Builder()
                // Balanced scanning dramatically lowers the cost of an always-on scan. Results
                // are delivered immediately so a lid-open advertisement is not held in a batch.
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                .setReportDelay(0L)
                .build()

            val manufacturerData = ByteArray(27)
            val manufacturerDataMask = ByteArray(27)

            manufacturerData[0] = 7
            manufacturerData[1] = 25

            manufacturerDataMask[0] = -1
            manufacturerDataMask[1] = -1

            val scanFilter = ScanFilter.Builder()
                .setManufacturerData(76, manufacturerData, manufacturerDataMask)
                .build()

            mScanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    processScanResult(result)
                }

                override fun onBatchScanResults(results: List<ScanResult>) {
                    for (result in results) {
                        processScanResult(result)
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    Log.e(TAG, "BLE scan failed with error code: $errorCode")
                }
            }

            mBluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, mScanCallback)
            Log.d(TAG, "BLE scanner started successfully")

            cleanupHandler.removeCallbacks(cleanupRunnable)
            cleanupHandler.postDelayed(cleanupRunnable, CLEANUP_INTERVAL_MS)
        } catch (t: Throwable) {
            Log.e(TAG, "Error starting BLE scanner", t)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        try {
            if (mBluetoothLeScanner != null && mScanCallback != null) {
                Log.d(TAG, "Stopping BLE scanner")
                mBluetoothLeScanner?.stopScan(mScanCallback)
                mScanCallback = null
            }

            cleanupHandler.removeCallbacks(cleanupRunnable)
        } catch (t: Throwable) {
            Log.e(TAG, "Error stopping BLE scanner", t)
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    @SuppressLint("GetInstance") // AirPods advertisements use protocol-defined AES-ECB blocks.
    private fun getEncryptionKeyFromPreferences(): ByteArray? {
        val keyBase64 = sharedPreferences.getString(AACPManager.Companion.ProximityKeyType.ENC_KEY.name, null)
        if (keyBase64 == cachedEncryptionKeyBase64) {
            return cachedEncryptionKey
        }

        cachedEncryptionKeyBase64 = keyBase64
        cachedEncryptionKey = null
        decryptionCipher = null

        if (keyBase64 == null) return null

        return try {
            val decodedKey = Base64.decode(keyBase64)
            val cipher = Cipher.getInstance("AES/ECB/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(decodedKey, "AES"))
            cachedEncryptionKey = decodedKey
            decryptionCipher = cipher
            decodedKey
        } catch (e: Exception) {
            // A bad stored key is logged once, when its value changes, rather than for every scan.
            Log.e(TAG, "Failed to initialize the AirPods advertisement key", e)
            null
        }
    }

    @SuppressLint("GetInstance")
    private fun decryptLastBytes(data: ByteArray, key: ByteArray): ByteArray? {
        return try {
            if (data.size < 16) {
                return null
            }

            var cipher = decryptionCipher
            if (cipher == null || cachedEncryptionKey !== key) {
                cipher = Cipher.getInstance("AES/ECB/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
                cachedEncryptionKey = key
                decryptionCipher = cipher
            }
            cipher.doFinal(data, data.size - 16, 16)
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting data", e)
            decryptionCipher = null
            null
        }
    }

    private fun processScanResult(result: ScanResult) {
        try {
            val scanRecord = result.scanRecord ?: return
            val address = result.device.address

            val manufacturerData = scanRecord.getManufacturerSpecificData(76) ?: return
            if (manufacturerData.size <= 20) return

            val irk = getIrkFromPreferences()
            if (!verifiedAddresses.contains(address)) {
                if (irk == null || !BluetoothCryptography.verifyRPA(address, irk)) {
                    return
                }
                verifiedAddresses.add(address)
                Log.d(TAG, "RPA verified and added to trusted list: $address")
            }

            val observedAt = System.currentTimeMillis()
            lastBroadcastTime = observedAt

            val encryptionKey = getEncryptionKeyFromPreferences()
            val previousStatus = deviceStatusMap[address]
            val previousAdvertisement = lastAdvertisementMap[address]
            val rememberedCaseExpired = previousStatus?.caseBattery != null &&
                !isRememberedCaseBatteryFresh(address, observedAt)
            if (previousStatus != null &&
                previousAdvertisement != null &&
                previousAdvertisement.encryptionKeyBase64 == cachedEncryptionKeyBase64 &&
                previousAdvertisement.manufacturerData.contentEquals(manufacturerData) &&
                !rememberedCaseExpired
            ) {
                // Duplicate advertisements are common. Keep liveness fresh without decrypting,
                // reparsing, writing preferences, or waking every downstream listener.
                val refreshed = previousStatus.copy(
                    lastSeen = observedAt,
                    caseBatteryObservedAt = if (previousStatus.caseBatteryIsCached) {
                        previousStatus.caseBatteryObservedAt
                    } else {
                        observedAt
                    },
                )
                deviceStatusMap[address] = refreshed
                airPodsStatusListener?.onBatteryObserved(refreshed)
                return
            }

            lastAdvertisementMap[address] = CachedAdvertisement(
                manufacturerData = manufacturerData.copyOf(),
                encryptionKeyBase64 = cachedEncryptionKeyBase64
            )

            val decryptedData = if (encryptionKey != null) decryptLastBytes(manufacturerData, encryptionKey) else null
            val parsedStatus = if (decryptedData != null && decryptedData.size == 16) {
                parseProximityMessageWithDecryption(address, manufacturerData, decryptedData, observedAt)
            } else {
                parseProximityMessage(address, manufacturerData, observedAt)
            }

            deviceStatusMap[address] = parsedStatus
            airPodsStatusListener?.onBatteryObserved(parsedStatus)

            airPodsStatusListener?.let { listener ->
                if (previousStatus == null) {
                    listener.onBroadcastFromNewAddress(parsedStatus)
                    Log.d(TAG, "New AirPods device detected: $address")

                    if (parsedStatus.leftBattery != null ||
                        parsedStatus.rightBattery != null ||
                        parsedStatus.caseBattery != null
                    ) {
                        listener.onBatteryChanged(parsedStatus)
                    }

                    if (currentGlobalLidState == null || currentGlobalLidState != parsedStatus.lidOpen) {
                        currentGlobalLidState = parsedStatus.lidOpen
                        listener.onLidStateChanged(parsedStatus.lidOpen)
                        Log.d(TAG, "Lid state ${if (parsedStatus.lidOpen) "opened" else "closed"} (detected from new device)")
                    }
                } else {
                    if (!parsedStatus.hasSameStateAs(previousStatus)) {
                        listener.onDeviceStatusChanged(parsedStatus, previousStatus)
                    }

                    if (parsedStatus.lidOpen != previousStatus.lidOpen) {
                        val previousGlobalState = currentGlobalLidState
                        currentGlobalLidState = parsedStatus.lidOpen

                        if (previousGlobalState != parsedStatus.lidOpen) {
                            listener.onLidStateChanged(parsedStatus.lidOpen)
                            Log.d(TAG, "Lid state changed from $previousGlobalState to ${parsedStatus.lidOpen}")
                        }
                    }

                    if (parsedStatus.isLeftInEar != previousStatus.isLeftInEar ||
                        parsedStatus.isRightInEar != previousStatus.isRightInEar) {
                        listener.onEarStateChanged(
                            parsedStatus,
                            parsedStatus.isLeftInEar,
                            parsedStatus.isRightInEar
                        )
                        Log.d(TAG, "Ear state changed - Left: ${parsedStatus.isLeftInEar}, Right: ${parsedStatus.isRightInEar}")
                    }

                    if (parsedStatus.leftBattery != previousStatus.leftBattery ||
                        parsedStatus.rightBattery != previousStatus.rightBattery ||
                        parsedStatus.caseBattery != previousStatus.caseBattery ||
                        parsedStatus.isLeftCharging != previousStatus.isLeftCharging ||
                        parsedStatus.isRightCharging != previousStatus.isRightCharging ||
                        parsedStatus.isCaseCharging != previousStatus.isCaseCharging ||
                        parsedStatus.hasExactBatteryData != previousStatus.hasExactBatteryData) {
                        listener.onBatteryChanged(parsedStatus)
                        Log.d(TAG, "Battery changed - Left: ${parsedStatus.leftBattery}, Right: ${parsedStatus.rightBattery}, Case: ${parsedStatus.caseBattery}")
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error processing scan result", t)
        }
    }

    private fun parseProximityMessageWithDecryption(
        address: String,
        data: ByteArray,
        decrypted: ByteArray,
        observedAt: Long
    ): AirPodsStatus {
        val paired = data[2].toInt() == 1
        val modelId = ((data[3].toInt() and 0xFF) shl 8) or (data[4].toInt() and 0xFF)
        val model = modelNames[modelId] ?: "Unknown ($modelId)"

        val status = data[5].toInt() and 0xFF
//        val flagsCase = data[7].toInt() and 0xFF
        val lid = data[8].toInt() and 0xFF
        val color = colorNames[data[9].toInt()] ?: "Unknown"
        val conn = connStates[data[10].toInt()] ?: "Unknown (${data[10].toInt()})"

        val primaryLeft = ((status shr 5) and 0x01) == 1
        val thisInCase = ((status shr 6) and 0x01) == 1
        val xorFactor = primaryLeft xor thisInCase

        val isLeftInEar = if (xorFactor) (status and 0x08) != 0 else (status and 0x02) != 0
        val isRightInEar = if (xorFactor) (status and 0x02) != 0 else (status and 0x08) != 0

        val isFlipped = !primaryLeft

        val leftByteIndex = if (isFlipped) 2 else 1
        val rightByteIndex = if (isFlipped) 1 else 2

        val rawLeftBatteryByte = decrypted[leftByteIndex].toInt() and 0xFF
        val rawRightBatteryByte = decrypted[rightByteIndex].toInt() and 0xFF
        val rawLeftBattery = rawLeftBatteryByte and 0x7F
        val rawRightBattery = rawRightBatteryByte and 0x7F
        val leftBattery = if (rawLeftBattery in 0..100) rawLeftBattery else null
        val rightBattery = if (rawRightBattery in 0..100) rawRightBattery else null
        val isLeftCharging = leftBattery != null && (rawLeftBatteryByte and 0x80) != 0
        val isRightCharging = rightBattery != null && (rawRightBatteryByte and 0x80) != 0

        val rawCaseBatteryByte = decrypted[3].toInt() and 0xFF
        val rawCaseBattery = rawCaseBatteryByte and 0x7F

        val isValidCase = rawCaseBatteryByte != 0xFF && rawCaseBattery in 0..100
        val isCaseCharging = isValidCase && (rawCaseBatteryByte and 0x80) != 0
        val rememberedCase = if (isValidCase) {
            rememberCaseBattery(address, rawCaseBattery, observedAt)
            RememberedCaseBattery(rawCaseBattery, observedAt)
        } else {
            getRememberedCaseBatteryObservation(address, observedAt)
        }

        val lidOpen = ((lid shr 3) and 0x01) == 0

        return AirPodsStatus(
            address = address,
            lastSeen = observedAt,
            paired = paired,
            model = model,
            leftBattery = leftBattery,
            rightBattery = rightBattery,
            caseBattery = rememberedCase?.level,
            caseBatteryObservedAt = rememberedCase?.observedAt,
            caseBatteryIsCached = !isValidCase && rememberedCase != null,
            isLeftInEar = isLeftInEar,
            isRightInEar = isRightInEar,
            isLeftCharging = isLeftCharging,
            isRightCharging = isRightCharging,
            isCaseCharging = isCaseCharging,
            hasExactBatteryData = true,
            lidOpen = lidOpen,
            color = color,
            connectionState = conn
        )
    }

    private fun cleanupStaleDevices() {
        val now = System.currentTimeMillis()
        val staleCutoff = now - STALE_DEVICE_TIMEOUT_MS
        val hadDevices = deviceStatusMap.isNotEmpty()

        val staleDevices = deviceStatusMap.filter { it.value.lastSeen < staleCutoff }

        for (device in staleDevices) {
            deviceStatusMap.remove(device.key)
            verifiedAddresses.remove(device.key)
            lastAdvertisementMap.remove(device.key)
            lastValidCaseBatteryMap.remove(device.key)
            loadedCaseBatteryAddresses.remove(device.key)
            Log.d(TAG, "Removed stale device from tracking: ${device.key}")
        }

        if (hadDevices && deviceStatusMap.isEmpty()) {
            airPodsStatusListener?.onDeviceDisappeared()
        }
    }

    private fun checkLidStateTimeout() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBroadcastTime > LID_CLOSE_TIMEOUT_MS && currentGlobalLidState == true) {
            Log.d(TAG, "No broadcasts for ${LID_CLOSE_TIMEOUT_MS}ms, forcing lid state to closed")
            currentGlobalLidState = false
            airPodsStatusListener?.onLidStateChanged(false)
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun getIrkFromPreferences(): ByteArray? {
        val irkBase64 = sharedPreferences.getString(AACPManager.Companion.ProximityKeyType.IRK.name, null)
        if (irkBase64 == cachedIrkBase64) {
            return cachedIrk
        }

        cachedIrkBase64 = irkBase64
        cachedIrk = null
        verifiedAddresses.clear()

        if (irkBase64 == null) return null

        return try {
            Base64.decode(irkBase64).also { cachedIrk = it }
        } catch (e: Exception) {
            // A bad stored IRK is logged once, when its value changes.
            Log.e(TAG, "Failed to decode IRK", e)
            null
        }
    }

    private fun parseProximityMessage(address: String, data: ByteArray, observedAt: Long): AirPodsStatus {
        val paired = data[2].toInt() == 1
        val modelId = ((data[3].toInt() and 0xFF) shl 8) or (data[4].toInt() and 0xFF)
        val model = modelNames[modelId] ?: "Unknown ($modelId)"

        val status = data[5].toInt() and 0xFF
        val podsBattery = data[6].toInt() and 0xFF
        val flagsCase = data[7].toInt() and 0xFF
        val lid = data[8].toInt() and 0xFF
        val color = colorNames[data[9].toInt()] ?: "Unknown"
        val conn = connStates[data[10].toInt()] ?: "Unknown (${data[10].toInt()})"

        val primaryLeft = ((status shr 5) and 0x01) == 1
        val thisInCase = ((status shr 6) and 0x01) == 1
        val xorFactor = primaryLeft xor thisInCase

        val isLeftInEar = if (xorFactor) (status and 0x08) != 0 else (status and 0x02) != 0
        val isRightInEar = if (xorFactor) (status and 0x02) != 0 else (status and 0x08) != 0

        val isFlipped = !primaryLeft

        val leftBatteryNibble = if (isFlipped) (podsBattery shr 4) and 0x0F else podsBattery and 0x0F
        val rightBatteryNibble = if (isFlipped) podsBattery and 0x0F else (podsBattery shr 4) and 0x0F

        val caseBattery = flagsCase and 0x0F
        val flags = (flagsCase shr 4) and 0x0F

        val isLeftCharging = if (isFlipped) (flags and 0x02) != 0 else (flags and 0x01) != 0
        val isRightCharging = if (isFlipped) (flags and 0x01) != 0 else (flags and 0x02) != 0
        val isCaseCharging = (flags and 0x04) != 0

        val lidOpen = ((lid shr 3) and 0x01) == 0

        fun decodeBattery(n: Int): Int? = when (n) {
            in 0x0..0x9 -> n * 10
            in 0xA..0xE -> 100
            0xF -> null
            else -> null
        }

        val decodedCase = decodeBattery(caseBattery)
        val rememberedCase = if (decodedCase != null) {
            rememberCaseBattery(address, decodedCase, observedAt)
            RememberedCaseBattery(decodedCase, observedAt)
        } else {
            getRememberedCaseBatteryObservation(address, observedAt)
        }

        return AirPodsStatus(
            address = address,
            lastSeen = observedAt,
            paired = paired,
            model = model,
            leftBattery = decodeBattery(leftBatteryNibble),
            rightBattery = decodeBattery(rightBatteryNibble),
            caseBattery = rememberedCase?.level,
            caseBatteryObservedAt = rememberedCase?.observedAt,
            caseBatteryIsCached = decodedCase == null && rememberedCase != null,
            isLeftInEar = isLeftInEar,
            isRightInEar = isRightInEar,
            isLeftCharging = isLeftCharging,
            isRightCharging = isRightCharging,
            isCaseCharging = isCaseCharging,
            hasExactBatteryData = false,
            lidOpen = lidOpen,
            color = color,
            connectionState = conn
        )
    }

    private fun AirPodsStatus.hasSameStateAs(other: AirPodsStatus): Boolean {
        return address == other.address &&
            paired == other.paired &&
            model == other.model &&
            leftBattery == other.leftBattery &&
            rightBattery == other.rightBattery &&
            caseBattery == other.caseBattery &&
            isLeftInEar == other.isLeftInEar &&
            isRightInEar == other.isRightInEar &&
            isLeftCharging == other.isLeftCharging &&
            isRightCharging == other.isRightCharging &&
            isCaseCharging == other.isCaseCharging &&
            hasExactBatteryData == other.hasExactBatteryData &&
            lidOpen == other.lidOpen &&
            color == other.color &&
            connectionState == other.connectionState
    }

    private fun getRememberedCaseBattery(
        address: String,
        now: Long = System.currentTimeMillis()
    ): Int? = getRememberedCaseBatteryObservation(address, now)?.level

    private fun getRememberedCaseBatteryObservation(
        address: String,
        now: Long = System.currentTimeMillis()
    ): RememberedCaseBattery? {
        if (loadedCaseBatteryAddresses.add(address)) {
            val stableLevel = sharedPreferences.getInt(LAST_CASE_BATTERY_KEY, -1)
            val stableObservedAt = sharedPreferences.getLong(LAST_CASE_BATTERY_AT_KEY, -1L)
            val legacyKey = "last_case_battery_$address"
            val legacyLevel = sharedPreferences.getInt(legacyKey, -1)
            val remembered = stableLevel.takeIf { it in 0..100 }
                ?: legacyLevel.takeIf { it in 0..100 }
            remembered?.let { level ->
                if (stableObservedAt > 0L && now - stableObservedAt in 0..CASE_BATTERY_CACHE_TTL_MS) {
                    lastValidCaseBatteryMap[address] = RememberedCaseBattery(level, stableObservedAt)
                }
                if (stableLevel !in 0..100 && stableObservedAt > 0L) {
                    sharedPreferences.edit {
                        putInt(LAST_CASE_BATTERY_KEY, level)
                        remove(legacyKey)
                    }
                }
            }
        }
        val remembered = lastValidCaseBatteryMap[address] ?: return null
        return if (now - remembered.observedAt in 0..CASE_BATTERY_CACHE_TTL_MS) {
            remembered
        } else {
            lastValidCaseBatteryMap.remove(address)
            null
        }
    }

    private fun rememberCaseBattery(address: String, level: Int, observedAt: Long): Int {
        val previous = lastValidCaseBatteryMap[address]
        lastValidCaseBatteryMap[address] = RememberedCaseBattery(level, observedAt)
        if (previous?.level != level ||
            observedAt - previous.observedAt >= CASE_BATTERY_PERSIST_INTERVAL_MS
        ) {
            sharedPreferences.edit {
                putInt(LAST_CASE_BATTERY_KEY, level)
                putLong(LAST_CASE_BATTERY_AT_KEY, observedAt)
            }
        }
        return level
    }

    private fun isRememberedCaseBatteryFresh(address: String, now: Long): Boolean {
        return getRememberedCaseBattery(address, now) != null
    }

    companion object {
        private const val TAG = "AirPodsBLE"
        private const val CLEANUP_INTERVAL_MS = 10000L
        private const val STALE_DEVICE_TIMEOUT_MS = 15000L
        private const val LAST_CASE_BATTERY_KEY = "last_case_battery"
        private const val LAST_CASE_BATTERY_AT_KEY = "last_case_battery_observed_at"
        private const val CASE_BATTERY_CACHE_TTL_MS = 15 * 60 * 1000L
        private const val CASE_BATTERY_PERSIST_INTERVAL_MS = 60 * 1000L
        // Balanced scans can have short radio gaps; allow one complete gap before inferring close.
        private const val LID_CLOSE_TIMEOUT_MS = 6000L
    }
}
