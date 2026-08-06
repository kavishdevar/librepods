/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.
*/

package me.kavishdevar.librepods.health

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.kavishdevar.librepods.bluetooth.HeartRateSample
import java.io.IOException
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId

/** User-visible Health Connect state for the optional heart-rate export. */
enum class HealthConnectExportStatus {
    UNAVAILABLE,
    UPDATE_REQUIRED,
    PERMISSION_REQUIRED,
    PERMISSION_DENIED,
    READY,
    ENABLED,
    ERROR
}

/**
 * Writes validated AirPods heart-rate samples to Health Connect at the selected interval.
 *
 * Each record is assigned a stable client record ID derived from its source sample contents and
 * device metadata. Retrying a failed record therefore remains idempotent even if Health Connect
 * accepted the record before returning an error.
 */
class HealthConnectHeartRateExporter(
    context: Context,
    private val sharedPreferences: SharedPreferences,
    private val scope: CoroutineScope
) {
    private data class PendingSample(
        val id: String,
        val sample: HeartRateSample,
        val deviceModel: String
    )

    private data class PendingRecord(
        val samples: List<PendingSample>,
        val clientRecordId: String,
        val startTimeMillis: Long,
        val endTimeMillis: Long,
        val partialInterval: Boolean
    )

    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private val pendingSamples = linkedMapOf<String, PendingSample>()
    private var pendingRecord: PendingRecord? = null
    private var intervalWindowStartMillis: Long? = null
    private var requestedDetailedSamples: Boolean? = null
    private var healthConnectClient: HealthConnectClient? = null
    private var scheduledFlush: Job? = null

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> get() = _enabled

    private val _detailedSamples = MutableStateFlow(
        sharedPreferences.getBoolean(DETAILED_SAMPLES_PREFERENCE, false)
    )
    val detailedSamples: StateFlow<Boolean> get() = _detailedSamples

    private val _status = MutableStateFlow(statusForSdk())
    val status: StateFlow<HealthConnectExportStatus> get() = _status

    fun refresh() {
        scope.launch {
            refreshInternal()
        }
    }

    suspend fun refreshInternal() {
        mutex.withLock {
            when (HealthConnectClient.getSdkStatus(appContext)) {
                HealthConnectClient.SDK_AVAILABLE -> {
                    val client = getClient()
                    val granted = try {
                        hasWritePermission(client)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        Log.w(TAG, "Unable to query Health Connect permissions", error)
                        _enabled.value = false
                        _status.value = HealthConnectExportStatus.ERROR
                        return@withLock
                    }

                    val requested = sharedPreferences.getBoolean(EXPORT_PREFERENCE, false)
                    val exportEnabled = requested && granted
                    _enabled.value = exportEnabled
                    _status.value = when {
                        !granted -> HealthConnectExportStatus.PERMISSION_REQUIRED
                        exportEnabled -> HealthConnectExportStatus.ENABLED
                        else -> HealthConnectExportStatus.READY
                    }

                    if (exportEnabled && hasPendingSamplesLocked()) {
                        scheduleFlushLocked(0L)
                    }
                }

                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                    healthConnectClient = null
                    _enabled.value = false
                    _status.value = HealthConnectExportStatus.UPDATE_REQUIRED
                }

                else -> {
                    healthConnectClient = null
                    _enabled.value = false
                    _status.value = HealthConnectExportStatus.UNAVAILABLE
                }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        scope.launch {
            setEnabledInternal(enabled)
        }
    }

    private suspend fun setEnabledInternal(enabled: Boolean) {
        mutex.withLock {
            if (!enabled) {
                scheduledFlush?.cancel()
                scheduledFlush = null
                flushLocked(forcePartialInterval = true)
                sharedPreferences.edit { putBoolean(EXPORT_PREFERENCE, false) }
                _enabled.value = false
                _status.value = disabledStatus()
                return@withLock
            }

            when (HealthConnectClient.getSdkStatus(appContext)) {
                HealthConnectClient.SDK_AVAILABLE -> {
                    val granted = try {
                        hasWritePermission(getClient())
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        Log.w(TAG, "Unable to enable Health Connect export", error)
                        _enabled.value = false
                        _status.value = HealthConnectExportStatus.ERROR
                        return@withLock
                    }

                    if (!granted) {
                        sharedPreferences.edit { putBoolean(EXPORT_PREFERENCE, false) }
                        _enabled.value = false
                        _status.value = HealthConnectExportStatus.PERMISSION_REQUIRED
                        return@withLock
                    }

                    sharedPreferences.edit { putBoolean(EXPORT_PREFERENCE, true) }
                    _enabled.value = true
                    _status.value = HealthConnectExportStatus.ENABLED
                    if (hasPendingSamplesLocked()) scheduleFlushLocked(0L)
                }

                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                    _enabled.value = false
                    _status.value = HealthConnectExportStatus.UPDATE_REQUIRED
                }

                else -> {
                    _enabled.value = false
                    _status.value = HealthConnectExportStatus.UNAVAILABLE
                }
            }
        }
    }

    fun setDetailedSamples(detailed: Boolean) {
        scope.launch {
            mutex.withLock {
                if (_detailedSamples.value == detailed) {
                    requestedDetailedSamples = null
                    return@withLock
                }

                requestedDetailedSamples = detailed
                scheduledFlush?.cancel()
                scheduledFlush = null
                if (hasPendingSamplesLocked() &&
                    (!_enabled.value || !flushLocked(forcePartialInterval = true))
                ) {
                    return@withLock
                }
                applyRequestedDetailLocked()
            }
        }
    }

    fun markPermissionDenied() {
        scope.launch {
            mutex.withLock {
                sharedPreferences.edit { putBoolean(EXPORT_PREFERENCE, false) }
                _enabled.value = false
                _status.value = HealthConnectExportStatus.PERMISSION_DENIED
            }
        }
    }

    fun enqueue(sample: HeartRateSample, deviceModel: String) {
        if (!_enabled.value) return

        scope.launch {
            mutex.withLock {
                if (!_enabled.value) return@withLock

                val id = clientRecordId(sample)
                pendingSamples.putIfAbsent(
                    id,
                    PendingSample(
                        id = id,
                        sample = sample,
                        deviceModel = deviceModel.ifBlank { "AirPods" }
                    )
                )
                trimBufferLocked()

                if (pendingRecord != null) {
                    return@withLock
                }
                if (hasCompletedIntervalWindowLocked()) {
                    scheduledFlush?.cancel()
                    scheduledFlush = null
                    flushLocked()
                } else {
                    scheduleNextFlushLocked()
                }
            }
        }
    }

    fun flushAsync() {
        scope.launch { flush(forcePartialInterval = true) }
    }

    suspend fun flush(forcePartialInterval: Boolean = false) {
        mutex.withLock {
            scheduledFlush?.cancel()
            scheduledFlush = null
            flushLocked(forcePartialInterval)
        }
    }

    suspend fun closeAndFlush() {
        flush(forcePartialInterval = true)
    }

    private suspend fun flushLocked(forcePartialInterval: Boolean = false): Boolean {
        if (!hasPendingSamplesLocked()) {
            applyRequestedDetailLocked()
            return true
        }
        if (!_enabled.value) return false

        while (_enabled.value && hasPendingSamplesLocked()) {
            val record = getOrCreatePendingRecordLocked(
                forcePartialInterval || requestedDetailedSamples != null
            )
            if (record == null) {
                scheduleNextFlushLocked()
                return false
            }

            try {
                getClient().insertRecords(listOf(toRecord(record)))
                completePendingRecordLocked(record)
                _status.value = HealthConnectExportStatus.ENABLED
            } catch (error: CancellationException) {
                throw error
            } catch (error: SecurityException) {
                Log.w(TAG, "Health Connect permission was revoked", error)
                sharedPreferences.edit { putBoolean(EXPORT_PREFERENCE, false) }
                _enabled.value = false
                _status.value = HealthConnectExportStatus.PERMISSION_REQUIRED
                return false
            } catch (error: IOException) {
                handleRetryableWriteFailureLocked(
                    "Health Connect write failed; keeping record for retry",
                    error
                )
                return false
            } catch (error: IllegalStateException) {
                handleRetryableWriteFailureLocked(
                    "Health Connect is temporarily unavailable",
                    error
                )
                return false
            } catch (error: RuntimeException) {
                handleRetryableWriteFailureLocked(
                    "Unexpected Health Connect write failure",
                    error
                )
                return false
            }
        }

        applyRequestedDetailLocked()
        return true
    }

    private fun handleRetryableWriteFailureLocked(message: String, error: Exception) {
        Log.w(TAG, message, error)
        _status.value = HealthConnectExportStatus.ERROR
        scheduleFlushLocked(RETRY_INTERVAL_MILLIS)
    }

    private fun applyRequestedDetailLocked() {
        val detailed = requestedDetailedSamples ?: return
        if (hasPendingSamplesLocked()) return

        intervalWindowStartMillis = null
        sharedPreferences.edit { putBoolean(DETAILED_SAMPLES_PREFERENCE, detailed) }
        _detailedSamples.value = detailed
        requestedDetailedSamples = null
    }

    private fun scheduleFlushLocked(delayMillis: Long) {
        if (scheduledFlush?.isActive == true) return
        scheduledFlush = scope.launch {
            delay(delayMillis)
            mutex.withLock {
                scheduledFlush = null
                flushLocked()
            }
        }
    }

    private fun scheduleNextFlushLocked() {
        if (pendingRecord != null || pendingSamples.isEmpty()) return
        val windowStart = ensureIntervalWindowStartLocked() ?: return
        val windowEnd = windowStart + exportIntervalMillis()
        val delayMillis = (windowEnd - System.currentTimeMillis()).coerceAtLeast(0L)
        scheduleFlushLocked(delayMillis)
    }

    private fun getOrCreatePendingRecordLocked(
        forcePartialInterval: Boolean
    ): PendingRecord? {
        pendingRecord?.let { return it }

        val orderedSamples = pendingSamples.values.sortedWith(PENDING_SAMPLE_COMPARATOR)
        if (orderedSamples.isEmpty()) return null

        var windowStart = ensureIntervalWindowStartLocked() ?: return null
        val earliestTimestamp = orderedSamples.first().sample.receivedAtMillis
        val intervalMillis = exportIntervalMillis()
        var windowEnd = windowStart + intervalMillis
        while (earliestTimestamp >= windowEnd) {
            windowStart = windowEnd
            windowEnd = windowStart + intervalMillis
            intervalWindowStartMillis = windowStart
        }

        val completedInterval = isIntervalCompleteLocked(windowEnd)
        if (!forcePartialInterval && !completedInterval) return null

        val selectedSamples = orderedSamples.takeWhile {
            it.sample.receivedAtMillis < windowEnd
        }
        if (selectedSamples.isEmpty()) return null

        selectedSamples.forEach { pendingSamples.remove(it.id) }
        val firstSampleTime = selectedSamples.first().sample.receivedAtMillis
        val lastSampleTime = selectedSamples.last().sample.receivedAtMillis
        val partialInterval = !completedInterval
        val recordStartTime = maxOf(windowStart, firstSampleTime)
        val recordEndTime = if (partialInterval) {
            maxOf(recordStartTime + 1L, lastSampleTime + 1L)
        } else {
            maxOf(recordStartTime + 1L, windowEnd)
        }

        return PendingRecord(
            samples = selectedSamples,
            clientRecordId = recordClientRecordId(
                samples = selectedSamples,
                startTimeMillis = recordStartTime,
                endTimeMillis = recordEndTime
            ),
            startTimeMillis = recordStartTime,
            endTimeMillis = recordEndTime,
            partialInterval = partialInterval
        ).also { pendingRecord = it }
    }

    private fun completePendingRecordLocked(record: PendingRecord) {
        pendingRecord = null
        intervalWindowStartMillis = if (record.partialInterval) null else record.endTimeMillis
    }

    private fun toRecord(record: PendingRecord): HeartRateRecord {
        val firstSample = record.samples.first()
        val startTimestamp = Instant.ofEpochMilli(record.startTimeMillis)
        val endTimestamp = Instant.ofEpochMilli(record.endTimeMillis)
        val zoneRules = ZoneId.systemDefault().rules
        val samples = listOf(
            HeartRateRecord.Sample(
                time = Instant.ofEpochMilli(
                    record.startTimeMillis +
                        (record.endTimeMillis - record.startTimeMillis) / 2L
                ),
                beatsPerMinute = averageBpm(record.samples)
            )
        )

        return HeartRateRecord(
            startTime = startTimestamp,
            startZoneOffset = zoneRules.getOffset(startTimestamp),
            endTime = endTimestamp,
            endZoneOffset = zoneRules.getOffset(endTimestamp),
            samples = samples,
            metadata = Metadata.autoRecorded(
                device = Device(
                    type = Device.TYPE_UNKNOWN,
                    manufacturer = "Apple",
                    model = firstSample.deviceModel
                ),
                clientRecordId = record.clientRecordId,
                clientRecordVersion = 0L
            )
        )
    }

    private fun hasPendingSamplesLocked(): Boolean =
        pendingRecord != null || pendingSamples.isNotEmpty()

    private fun bufferedSampleCountLocked(): Int =
        pendingSamples.size + (pendingRecord?.samples?.size ?: 0)

    private fun hasCompletedIntervalWindowLocked(): Boolean {
        val windowStart = ensureIntervalWindowStartLocked() ?: return false
        return isIntervalCompleteLocked(windowStart + exportIntervalMillis())
    }

    private fun isIntervalCompleteLocked(windowEnd: Long): Boolean =
        System.currentTimeMillis() >= windowEnd || pendingSamples.values.any {
            it.sample.receivedAtMillis >= windowEnd
        }

    private fun ensureIntervalWindowStartLocked(): Long? {
        intervalWindowStartMillis?.let { return it }
        return pendingSamples.values.minOfOrNull { it.sample.receivedAtMillis }?.also {
            intervalWindowStartMillis = it
        }
    }

    private fun exportIntervalMillis(): Long = if (_detailedSamples.value) {
        SECOND_INTERVAL_MILLIS
    } else {
        MINUTE_INTERVAL_MILLIS
    }

    private fun trimBufferLocked() {
        while (bufferedSampleCountLocked() > MAX_BUFFERED_SAMPLES) {
            val oldestId = pendingSamples.keys.firstOrNull() ?: break
            pendingSamples.remove(oldestId)
        }
    }

    private fun averageBpm(samples: List<PendingSample>): Long {
        val total = samples.sumOf { it.sample.bpm.toLong() }
        return (total + samples.size / 2L) / samples.size
    }

    private fun recordClientRecordId(
        samples: List<PendingSample>,
        startTimeMillis: Long,
        endTimeMillis: Long
    ): String {
        val stableRecordDescription = buildString {
            append(startTimeMillis)
            append('\u0000')
            append(endTimeMillis)
            append('\u0000')
            append(samples.first().deviceModel)
            samples.forEach { pending ->
                append('\u0000')
                append(pending.id)
            }
        }
        return "$RECORD_CLIENT_RECORD_ID_PREFIX${sha256(stableRecordDescription)}"
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte ->
                "%02x".format(byte.toInt() and 0xff)
            }

    private fun getClient(): HealthConnectClient = healthConnectClient
        ?: HealthConnectClient.getOrCreate(appContext).also { healthConnectClient = it }

    private suspend fun hasWritePermission(client: HealthConnectClient): Boolean =
        WRITE_HEART_RATE_PERMISSION in client.permissionController.getGrantedPermissions()

    private fun statusForSdk(): HealthConnectExportStatus =
        when (HealthConnectClient.getSdkStatus(appContext)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectExportStatus.PERMISSION_REQUIRED
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectExportStatus.UPDATE_REQUIRED
            else -> HealthConnectExportStatus.UNAVAILABLE
        }

    private suspend fun disabledStatus(): HealthConnectExportStatus {
        return when (HealthConnectClient.getSdkStatus(appContext)) {
            HealthConnectClient.SDK_AVAILABLE -> {
                val permissionGranted = try {
                    hasWritePermission(getClient())
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Log.w(TAG, "Unable to query Health Connect permissions", error)
                    return HealthConnectExportStatus.ERROR
                }
                if (permissionGranted) {
                    HealthConnectExportStatus.READY
                } else {
                    HealthConnectExportStatus.PERMISSION_REQUIRED
                }
            }

            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectExportStatus.UPDATE_REQUIRED

            else -> HealthConnectExportStatus.UNAVAILABLE
        }
    }

    private fun clientRecordId(sample: HeartRateSample): String =
        "librepods-heart-rate-v1-${sample.receivedAtMillis}-${sample.sequence}-${sample.bpm}"

    companion object {
        private val PENDING_SAMPLE_COMPARATOR = compareBy<PendingSample>(
            { it.sample.receivedAtMillis },
            { it.sample.sequence },
            { it.id }
        )

        private const val TAG = "HealthConnectHR"
        private const val EXPORT_PREFERENCE = "heart_rate_health_connect_export_enabled"
        private const val DETAILED_SAMPLES_PREFERENCE =
            "heart_rate_health_connect_detailed_samples"
        private const val RECORD_CLIENT_RECORD_ID_PREFIX = "librepods-heart-rate-record-v1-"
        private const val MAX_BUFFERED_SAMPLES = 300
        private const val SECOND_INTERVAL_MILLIS = 1_000L
        private const val MINUTE_INTERVAL_MILLIS = 60_000L
        private const val RETRY_INTERVAL_MILLIS = 30_000L

        val WRITE_HEART_RATE_PERMISSION: String =
            HealthPermission.getWritePermission(HeartRateRecord::class)
        val REQUIRED_PERMISSIONS: Set<String> = setOf(WRITE_HEART_RATE_PERMISSION)
    }
}
