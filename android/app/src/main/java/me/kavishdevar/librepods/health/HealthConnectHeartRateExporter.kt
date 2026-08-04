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
 * Buffers validated AirPods heart-rate samples and writes them to Health Connect.
 *
 * Each batch is assigned a stable client record ID derived from its ordered sample contents and
 * device metadata. Retrying a failed batch therefore remains idempotent even if Health Connect
 * accepted the record before returning an error.
 */
class HealthConnectHeartRateExporter(
    context: Context,
    private val sharedPreferences: SharedPreferences,
    private val scope: CoroutineScope
) {
    private enum class BatchDetail {
        MINUTE_AVERAGE,
        DETAILED
    }

    private data class PendingSample(
        val id: String,
        val sample: HeartRateSample,
        val deviceModel: String
    )

    private data class PendingBatch(
        val samples: List<PendingSample>,
        val clientRecordId: String,
        val detail: BatchDetail,
        val startTimeMillis: Long,
        val endTimeMillis: Long,
        val partialMinute: Boolean = false
    )

    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private val pendingSamples = linkedMapOf<String, PendingSample>()
    private var pendingBatch: PendingBatch? = null
    private var minuteWindowStartMillis: Long? = null
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
                    } catch (error: Exception) {
                        Log.w(TAG, "Unable to query Health Connect permissions", error)
                        _enabled.value = false
                        _status.value = HealthConnectExportStatus.ERROR
                        return@withLock
                    }

                    val requested = sharedPreferences.getBoolean(EXPORT_PREFERENCE, false)
                    _enabled.value = requested && granted
                    _status.value = when {
                        !granted -> HealthConnectExportStatus.PERMISSION_REQUIRED
                        _enabled.value -> HealthConnectExportStatus.ENABLED
                        else -> HealthConnectExportStatus.READY
                    }

                    if (_enabled.value && hasPendingSamplesLocked()) {
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
                flushLocked(forcePartialMinute = true)
                sharedPreferences.edit { putBoolean(EXPORT_PREFERENCE, false) }
                _enabled.value = false
                _status.value = disabledStatus()
                return@withLock
            }

            when (HealthConnectClient.getSdkStatus(appContext)) {
                HealthConnectClient.SDK_AVAILABLE -> {
                    val granted = try {
                        hasWritePermission(getClient())
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
                if (hasPendingSamplesLocked()) {
                    if (!_enabled.value || !flushLocked(forcePartialMinute = true)) {
                        return@withLock
                    }
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
            val flushNow = mutex.withLock {
                if (!_enabled.value) return@withLock false

                val id = clientRecordId(sample)
                pendingSamples.putIfAbsent(
                    id,
                    PendingSample(
                        id = id,
                        sample = sample,
                        deviceModel = deviceModel.ifBlank { "AirPods" }
                    )
                )
                if (!_detailedSamples.value && minuteWindowStartMillis == null) {
                    minuteWindowStartMillis = sample.receivedAtMillis
                }
                trimBufferLocked()

                if (pendingBatch != null) {
                    false
                } else if (_detailedSamples.value) {
                    if (bufferedSampleCountLocked() >= MAX_BATCH_SIZE) {
                        scheduledFlush?.cancel()
                        scheduledFlush = null
                        true
                    } else {
                        scheduleFlushLocked(FLUSH_INTERVAL_MILLIS)
                        false
                    }
                } else if (hasCompletedMinuteWindowLocked()) {
                    scheduledFlush?.cancel()
                    scheduledFlush = null
                    true
                } else {
                    scheduleMinuteFlushLocked()
                    false
                }
            }

            if (flushNow) flush()
        }
    }

    fun flushAsync() {
        scope.launch { flush(forcePartialMinute = true) }
    }

    suspend fun flush(forcePartialMinute: Boolean = false) {
        mutex.withLock {
            scheduledFlush?.cancel()
            scheduledFlush = null
            flushLocked(forcePartialMinute)
        }
    }

    suspend fun closeAndFlush() {
        flush(forcePartialMinute = true)
    }

    private suspend fun flushLocked(forcePartialMinute: Boolean = false): Boolean {
        if (!hasPendingSamplesLocked()) {
            applyRequestedDetailLocked()
            return true
        }
        if (!_enabled.value) return false

        while (_enabled.value && hasPendingSamplesLocked()) {
            val batch = getOrCreatePendingBatchLocked(
                forcePartialMinute || requestedDetailedSamples != null
            )
            if (batch == null) {
                scheduleNextFlushLocked()
                return false
            }

            try {
                getClient().insertRecords(listOf(toRecord(batch)))
                completePendingBatchLocked(batch)
                _status.value = HealthConnectExportStatus.ENABLED
            } catch (error: SecurityException) {
                Log.w(TAG, "Health Connect permission was revoked", error)
                sharedPreferences.edit { putBoolean(EXPORT_PREFERENCE, false) }
                _enabled.value = false
                _status.value = HealthConnectExportStatus.PERMISSION_REQUIRED
                return false
            } catch (error: IOException) {
                handleRetryableWriteFailureLocked(
                    "Health Connect write failed; keeping batch for retry",
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

        minuteWindowStartMillis = null
        sharedPreferences.edit {
            putBoolean(DETAILED_SAMPLES_PREFERENCE, detailed)
        }
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
        if (pendingBatch != null || pendingSamples.isEmpty()) return
        if (_detailedSamples.value) {
            scheduleFlushLocked(FLUSH_INTERVAL_MILLIS)
        } else {
            scheduleMinuteFlushLocked()
        }
    }

    private fun scheduleMinuteFlushLocked() {
        val windowStart = ensureMinuteWindowStartLocked() ?: return
        val windowEnd = windowStart + MINUTE_WINDOW_MILLIS
        val delayMillis = (windowEnd - System.currentTimeMillis()).coerceAtLeast(0L)
        scheduleFlushLocked(delayMillis)
    }

    private fun getOrCreatePendingBatchLocked(forcePartialMinute: Boolean): PendingBatch? {
        pendingBatch?.let { return it }

        return if (_detailedSamples.value) {
            createDetailedBatchLocked()
        } else {
            createMinuteAverageBatchLocked(forcePartialMinute)
        }
    }

    private fun createDetailedBatchLocked(): PendingBatch? {
        val selectedSamples = pendingSamples.values.take(MAX_BATCH_SIZE)
        if (selectedSamples.isEmpty()) return null

        selectedSamples.forEach { pendingSamples.remove(it.id) }
        val orderedSamples = selectedSamples.sortedWith(PENDING_SAMPLE_COMPARATOR)
        val firstSample = orderedSamples.first()
        val lastSample = orderedSamples.last()

        return PendingBatch(
            samples = orderedSamples,
            clientRecordId = batchClientRecordId(orderedSamples),
            detail = BatchDetail.DETAILED,
            startTimeMillis = firstSample.sample.receivedAtMillis,
            endTimeMillis = lastSample.sample.receivedAtMillis + 1L
        ).also { pendingBatch = it }
    }

    private fun createMinuteAverageBatchLocked(forcePartialMinute: Boolean): PendingBatch? {
        val orderedSamples = pendingSamples.values.sortedWith(PENDING_SAMPLE_COMPARATOR)
        if (orderedSamples.isEmpty()) return null

        var windowStart = ensureMinuteWindowStartLocked() ?: return null
        val earliestTimestamp = orderedSamples.first().sample.receivedAtMillis
        var windowEnd = windowStart + MINUTE_WINDOW_MILLIS
        while (earliestTimestamp >= windowEnd) {
            windowStart = windowEnd
            windowEnd = windowStart + MINUTE_WINDOW_MILLIS
            minuteWindowStartMillis = windowStart
        }

        val hasSampleAfterWindow = orderedSamples.any {
            it.sample.receivedAtMillis >= windowEnd
        }
        val completedWindow = hasSampleAfterWindow || System.currentTimeMillis() >= windowEnd
        if (!forcePartialMinute && !completedWindow) return null

        val selectedSamples = orderedSamples.takeWhile {
            it.sample.receivedAtMillis < windowEnd
        }
        if (selectedSamples.isEmpty()) return null

        selectedSamples.forEach { pendingSamples.remove(it.id) }
        val firstSampleTime = selectedSamples.first().sample.receivedAtMillis
        val lastSampleTime = selectedSamples.last().sample.receivedAtMillis
        val partialMinute = !completedWindow
        val recordStartTime = maxOf(windowStart, firstSampleTime)
        val recordEndTime = if (partialMinute) {
            maxOf(recordStartTime + 1L, lastSampleTime + 1L)
        } else {
            maxOf(recordStartTime + 1L, windowEnd)
        }

        return PendingBatch(
            samples = selectedSamples,
            clientRecordId = minuteAverageClientRecordId(
                samples = selectedSamples,
                startTimeMillis = recordStartTime,
                endTimeMillis = recordEndTime
            ),
            detail = BatchDetail.MINUTE_AVERAGE,
            startTimeMillis = recordStartTime,
            endTimeMillis = recordEndTime,
            partialMinute = partialMinute
        ).also { pendingBatch = it }
    }

    private fun completePendingBatchLocked(batch: PendingBatch) {
        pendingBatch = null
        if (batch.detail == BatchDetail.MINUTE_AVERAGE) {
            minuteWindowStartMillis = if (batch.partialMinute) {
                null
            } else {
                batch.endTimeMillis
            }
        }
    }

    private fun toRecord(batch: PendingBatch): HeartRateRecord {
        val firstSample = batch.samples.first()
        val startTimestamp = Instant.ofEpochMilli(batch.startTimeMillis)
        val endTimestamp = Instant.ofEpochMilli(batch.endTimeMillis)
        val zoneRules = ZoneId.systemDefault().rules
        val samples = when (batch.detail) {
            BatchDetail.DETAILED -> batch.samples.map { pending ->
                HeartRateRecord.Sample(
                    time = Instant.ofEpochMilli(pending.sample.receivedAtMillis),
                    beatsPerMinute = pending.sample.bpm.toLong()
                )
            }

            BatchDetail.MINUTE_AVERAGE -> listOf(
                HeartRateRecord.Sample(
                    time = Instant.ofEpochMilli(
                        batch.startTimeMillis +
                            (batch.endTimeMillis - batch.startTimeMillis) / 2L
                    ),
                    beatsPerMinute = averageBpm(batch.samples)
                )
            )
        }

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
                clientRecordId = batch.clientRecordId,
                clientRecordVersion = 0L
            )
        )
    }

    private fun hasPendingSamplesLocked(): Boolean =
        pendingBatch != null || pendingSamples.isNotEmpty()

    private fun bufferedSampleCountLocked(): Int =
        pendingSamples.size + (pendingBatch?.samples?.size ?: 0)

    private fun hasCompletedMinuteWindowLocked(): Boolean {
        val windowStart = ensureMinuteWindowStartLocked() ?: return false
        val windowEnd = windowStart + MINUTE_WINDOW_MILLIS
        return System.currentTimeMillis() >= windowEnd || pendingSamples.values.any {
            it.sample.receivedAtMillis >= windowEnd
        }
    }

    private fun ensureMinuteWindowStartLocked(): Long? {
        minuteWindowStartMillis?.let { return it }
        return pendingSamples.values.minOfOrNull { it.sample.receivedAtMillis }?.also {
            minuteWindowStartMillis = it
        }
    }

    private fun trimBufferLocked() {
        while (bufferedSampleCountLocked() > MAX_BUFFERED_SAMPLES) {
            val oldestId = pendingSamples.keys.firstOrNull() ?: break
            pendingSamples.remove(oldestId)
        }
    }

    private fun averageBpm(samples: List<PendingSample>): Long {
        val total = samples.fold(0L) { sum, pending ->
            sum + pending.sample.bpm.toLong()
        }
        return (total + samples.size / 2L) / samples.size
    }

    private fun batchClientRecordId(samples: List<PendingSample>): String {
        val stableBatchDescription = buildString {
            append(samples.first().deviceModel)
            samples.forEach { pending ->
                append('\u0000')
                append(pending.id)
            }
        }
        return "$BATCH_CLIENT_RECORD_ID_PREFIX${sha256(stableBatchDescription)}"
    }

    private fun minuteAverageClientRecordId(
        samples: List<PendingSample>,
        startTimeMillis: Long,
        endTimeMillis: Long
    ): String {
        val stableBatchDescription = buildString {
            append(startTimeMillis)
            append('\u0000')
            append(endTimeMillis)
            samples.forEach { pending ->
                append('\u0000')
                append(pending.deviceModel)
                append('\u0000')
                append(pending.id)
            }
        }
        return "$MINUTE_AVERAGE_CLIENT_RECORD_ID_PREFIX${sha256(stableBatchDescription)}"
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
        private const val BATCH_CLIENT_RECORD_ID_PREFIX = "librepods-heart-rate-batch-v1-"
        private const val MINUTE_AVERAGE_CLIENT_RECORD_ID_PREFIX =
            "librepods-heart-rate-minute-average-v1-"
        private const val MAX_BATCH_SIZE = 15
        private const val MAX_BUFFERED_SAMPLES = 300
        private const val FLUSH_INTERVAL_MILLIS = 15_000L
        private const val MINUTE_WINDOW_MILLIS = 60_000L
        private const val RETRY_INTERVAL_MILLIS = 30_000L

        val WRITE_HEART_RATE_PERMISSION: String =
            HealthPermission.getWritePermission(HeartRateRecord::class)
        val REQUIRED_PERMISSIONS: Set<String> = setOf(WRITE_HEART_RATE_PERMISSION)
    }
}
