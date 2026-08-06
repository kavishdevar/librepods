/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.
*/

package me.kavishdevar.librepods.bluetooth

import android.os.SystemClock

/** A validated heart-rate sample decoded from an RTBuddy SensorDataWX frame. */
data class HeartRateSample(
    val bpm: Int,
    val sequence: Int,
    val receivedAtMillis: Long,
    val receivedAtElapsedRealtime: Long = SystemClock.elapsedRealtime()
)

internal enum class HeartRateRejectionReason {
    MALFORMED_SENSOR_DATA,
    UNSUPPORTED_LOG_TYPE,
    MISSING_HEART_RATE_PAYLOAD,
    UNRECOGNIZED_HEART_RATE_PAYLOAD
}

internal data class HeartRateDecodeResult(
    val samples: List<HeartRateSample> = emptyList(),
    val relatedFrameCount: Int = 0,
    val rejectedFrameCount: Int = 0,
    val rejectionReasons: Map<HeartRateRejectionReason, Int> = emptyMap(),
    val structuralDiagnostics: Map<String, Int> = emptyMap(),
    val suppressRawLogging: Boolean = false,
    val passthroughPackets: List<ByteArray> = emptyList()
)

/**
 * Stateful decoder for the verified RTBuddy HEARTRATE SensorDataWX stream.
 *
 * Socket reads are arbitrary chunks. A possible partial 0x17/0x00100000 frame is retained until
 * its declared payload is complete. Other 0x17 packets are reconstructed and passed to the normal
 * AACP parser so head tracking keeps its existing behavior.
 *
 * The known heart-rate value is accepted only from a live SensorDataWX record: an exact 18-byte
 * HEARTRATE(19) command payload with one of the observed status trailers and a BPM in the validated
 * physiological range. Firmware may use either observed live log type and may repeat command payload
 * field 3 or place that exact payload inside one or more protobuf length-delimited wrappers; those
 * structural variants are traversed without reading arbitrary offsets.
 */
internal class RtBuddyHeartRateDecoder {
    private var carry = ByteArray(0)

    @Synchronized
    fun reset() {
        carry = ByteArray(0)
    }

    @Synchronized
    fun feed(chunk: ByteArray): HeartRateDecodeResult {
        if (chunk.isEmpty()) return HeartRateDecodeResult()

        val hadCarry = carry.isNotEmpty()
        val carryWasSensitive = carry.size >= MIN_SENSITIVE_PREFIX_LENGTH
        val combined = if (carry.isEmpty()) chunk else carry + chunk
        carry = ByteArray(0)

        val samples = mutableListOf<HeartRateSample>()
        val passthroughPackets = mutableListOf<ByteArray>()
        var relatedFrameCount = 0
        var rejectedFrameCount = 0
        val rejectionReasons = mutableMapOf<HeartRateRejectionReason, Int>()
        val structuralDiagnostics = linkedMapOf<String, Int>()
        var suppressRawLogging = carryWasSensitive
        var cursor = 0

        while (cursor < combined.size) {
            val candidateOffset = combined.indexOfPrefix(RTBUDDY_FRAME_PREFIX, cursor)
            if (candidateOffset < 0) {
                val suffixLength = combined.longestSuffixMatchingPrefix(
                    prefix = RTBUDDY_FRAME_PREFIX,
                    startIndex = cursor
                )
                val passthroughEnd = combined.size - suffixLength
                if (passthroughEnd > cursor) {
                    passthroughPackets += combined.copyOfRange(cursor, passthroughEnd)
                }
                if (suffixLength > 0) {
                    carry = combined.copyOfRange(passthroughEnd, combined.size)
                    if (suffixLength >= MIN_SENSITIVE_PREFIX_LENGTH) {
                        suppressRawLogging = true
                    }
                }
                break
            }

            if (candidateOffset > cursor) {
                passthroughPackets += combined.copyOfRange(cursor, candidateOffset)
            }

            if (combined.size - candidateOffset < AACP_RTBUDDY_HEADER_LENGTH) {
                carry = combined.copyOfRange(candidateOffset, combined.size)
                suppressRawLogging = true
                break
            }

            val declaredLength = combined.readLe16(candidateOffset + 10)
            if (declaredLength > MAX_RTBUDDY_PAYLOAD_LENGTH) {
                // The exact SensorDataWX prefix is sensitive, but the length is untrusted. Drop the
                // remainder rather than exposing it to generic packet logs or interpreting it as
                // head tracking.
                suppressRawLogging = true
                break
            }

            val frameLength = AACP_RTBUDDY_HEADER_LENGTH + declaredLength
            if (combined.size - candidateOffset < frameLength) {
                carry = combined.copyOfRange(candidateOffset, combined.size)
                suppressRawLogging = true
                break
            }

            val frame = combined.copyOfRange(candidateOffset, candidateOffset + frameLength)
            val classification = classifyFrame(frame)
            if (classification.isHeartRateRelated) {
                relatedFrameCount++
                if (classification.sample == null) {
                    rejectedFrameCount++
                    classification.rejectionReason?.let { rejectionReasons.increment(it) }
                }
                classification.structuralDiagnostic?.let { structuralDiagnostics.increment(it) }
                suppressRawLogging = true
                classification.sample?.let(samples::add)
            } else {
                passthroughPackets += frame
                if (hadCarry && candidateOffset == 0) suppressRawLogging = true
            }
            cursor = candidateOffset + frameLength
        }

        return HeartRateDecodeResult(
            samples = samples,
            relatedFrameCount = relatedFrameCount,
            rejectedFrameCount = rejectedFrameCount,
            rejectionReasons = rejectionReasons,
            structuralDiagnostics = structuralDiagnostics,
            suppressRawLogging = suppressRawLogging,
            passthroughPackets = passthroughPackets
        )
    }

    private fun classifyFrame(frame: ByteArray): FrameClassification {
        val hasHeartRateReference = hasHeartRateServiceReference(
            frame,
            AACP_RTBUDDY_HEADER_LENGTH,
            frame.size
        )
        val sensorData = parseSensorDataWx(frame, AACP_RTBUDDY_HEADER_LENGTH, frame.size)
            ?: return FrameClassification(
                isHeartRateRelated = hasHeartRateReference,
                rejectionReason = HeartRateRejectionReason.MALFORMED_SENSOR_DATA
                    .takeIf { hasHeartRateReference },
                structuralDiagnostic = MALFORMED_STRUCTURE_DIAGNOSTIC
                    .takeIf { hasHeartRateReference }
            )
        val heartRateRelated = hasHeartRateReference ||
            HEART_RATE_SERVICE in sensorData.referencedServices
        if (!heartRateRelated) return FrameClassification()
        if (sensorData.logType !in LIVE_SENSOR_DATA_LOG_TYPES) {
            return FrameClassification(
                isHeartRateRelated = true,
                rejectionReason = HeartRateRejectionReason.UNSUPPORTED_LOG_TYPE,
                structuralDiagnostic = buildStructuralDiagnostic(sensorData, emptyList())
            )
        }

        val heartRateCommands = sensorData.commands.filter { it.service == HEART_RATE_SERVICE }
        val analyses = heartRateCommands.flatMap { command ->
            command.payloadCandidates.map { candidate ->
                PayloadAnalysis(
                    command = command,
                    candidate = candidate,
                    failures = validateHeartRatePayload(candidate.bytes)
                )
            }
        }
        val accepted = analyses.firstOrNull { it.failures.isEmpty() }
        if (accepted == null) {
            val hasPayloadCandidate = heartRateCommands.any {
                it.directPayloadCount > 0 || it.payloadCandidates.isNotEmpty()
            }
            return FrameClassification(
                isHeartRateRelated = true,
                rejectionReason = if (hasPayloadCandidate) {
                    HeartRateRejectionReason.UNRECOGNIZED_HEART_RATE_PAYLOAD
                } else {
                    HeartRateRejectionReason.MISSING_HEART_RATE_PAYLOAD
                },
                structuralDiagnostic = buildStructuralDiagnostic(sensorData, analyses)
            )
        }

        val payload = accepted.candidate.bytes
        return FrameClassification(
            isHeartRateRelated = true,
            sample = HeartRateSample(
                bpm = payload.unsignedByteAt(HEART_RATE_BPM_OFFSET),
                sequence = sensorData.sequence,
                receivedAtMillis = System.currentTimeMillis(),
                receivedAtElapsedRealtime = SystemClock.elapsedRealtime()
            )
        )
    }

    private fun validateHeartRatePayload(payload: ByteArray): Set<PayloadValidationFailure> {
        if (payload.size != HEART_RATE_PAYLOAD_LENGTH) {
            return setOf(PayloadValidationFailure.LENGTH)
        }

        val failures = linkedSetOf<PayloadValidationFailure>()
        if (!payload.hasKnownHeartRateStatusTail()) {
            failures += PayloadValidationFailure.STATUS_TAIL
        }
        if (payload.unsignedByteAt(HEART_RATE_BPM_OFFSET) !in MIN_BPM..MAX_BPM) {
            failures += PayloadValidationFailure.BPM_RANGE
        }
        return failures
    }

    private fun ByteArray.hasKnownHeartRateStatusTail(): Boolean {
        if (size != HEART_RATE_PAYLOAD_LENGTH) return false
        return KNOWN_HEART_RATE_STATUS_TAILS.any { tail ->
            tail.indices.all { index ->
                this[HEART_RATE_STATUS_TAIL_OFFSET + index] == tail[index]
            }
        }
    }

    private fun ByteArray.unsignedByteAt(index: Int): Int = this[index].toInt().and(0xFF)

    private fun <K> MutableMap<K, Int>.increment(key: K) {
        this[key] = getOrDefault(key, 0) + 1
    }

    private fun hasHeartRateServiceReference(data: ByteArray, start: Int, end: Int): Boolean {
        val message = parseProtoMessage(data, start, end) ?: return false
        return message.entries.any { entry ->
            entry.wireType == WIRE_LENGTH_DELIMITED &&
                entry.field in SENSOR_DATA_COMMAND_FIELDS &&
                containsServiceReference(
                    data = data,
                    start = entry.valueStart,
                    end = entry.valueEnd,
                    depth = 0
                )
        }
    }

    private fun containsServiceReference(
        data: ByteArray,
        start: Int,
        end: Int,
        depth: Int
    ): Boolean {
        if (depth > MAX_COMMAND_ENVELOPE_DEPTH) return false
        val message = parseProtoMessage(data, start, end) ?: return false
        if (message.entries.any {
                it.field == 1 &&
                    it.wireType == WIRE_VARINT &&
                    it.varintValue == HEART_RATE_SERVICE.toLong()
            }
        ) {
            return true
        }
        if (depth == MAX_COMMAND_ENVELOPE_DEPTH) return false
        return message.entries.any { entry ->
            entry.wireType == WIRE_LENGTH_DELIMITED &&
                containsServiceReference(
                    data,
                    entry.valueStart,
                    entry.valueEnd,
                    depth + 1
                )
        }
    }

    private fun parseSensorDataWx(data: ByteArray, start: Int, end: Int): SensorDataWx? {
        val message = parseProtoMessage(data, start, end) ?: return null
        var sequence = -1
        var logType = -1
        val commands = mutableListOf<RtBuddyCommand>()
        val referencedServices = mutableSetOf<Int>()
        val fieldOccurrences = mutableMapOf<Int, Int>()

        message.entries.forEach { entry ->
            when {
                entry.wireType == WIRE_VARINT && entry.field == 1 -> {
                    sequence = entry.varintValue?.toInt() ?: sequence
                }

                entry.wireType == WIRE_VARINT && entry.field == 2 -> {
                    logType = entry.varintValue?.toInt() ?: logType
                }

                entry.wireType == WIRE_LENGTH_DELIMITED &&
                    entry.field in SENSOR_DATA_COMMAND_FIELDS -> {
                    val occurrence = fieldOccurrences.getOrDefault(entry.field, 0)
                    fieldOccurrences[entry.field] = occurrence + 1
                    inspectCommandEnvelope(
                        data = data,
                        start = entry.valueStart,
                        end = entry.valueEnd,
                        path = "f${entry.field}[$occurrence]",
                        depth = 0,
                        commands = commands,
                        referencedServices = referencedServices
                    )
                }
            }
        }

        return SensorDataWx(
            sequence = sequence,
            logType = logType,
            commands = commands,
            referencedServices = referencedServices,
            topLevelShape = message.shape
        )
    }

    private fun inspectCommandEnvelope(
        data: ByteArray,
        start: Int,
        end: Int,
        path: String,
        depth: Int,
        commands: MutableList<RtBuddyCommand>,
        referencedServices: MutableSet<Int>
    ) {
        if (depth > MAX_COMMAND_ENVELOPE_DEPTH || commands.size >= MAX_COMMANDS_PER_FRAME) return
        val message = parseProtoMessage(data, start, end) ?: return
        val service = message.entries.firstOrNull {
            it.field == 1 && it.wireType == WIRE_VARINT
        }?.varintValue?.toInt()

        if (service != null && service >= 0) {
            referencedServices += service
            val directPayloadEntries = message.entries.filter {
                it.field == 3 && it.wireType == WIRE_LENGTH_DELIMITED
            }
            val payloadCandidates = mutableListOf<PayloadCandidate>()
            directPayloadEntries.forEachIndexed { index, entry ->
                collectPayloadCandidates(
                    data = data,
                    start = entry.valueStart,
                    end = entry.valueEnd,
                    path = "$path.f3[$index]",
                    wrapperDepth = 0,
                    candidates = payloadCandidates
                )
            }
            commands += RtBuddyCommand(
                service = service,
                directPayloadCount = directPayloadEntries.size,
                payloadCandidates = payloadCandidates,
                path = path,
                shape = message.shape
            )
        }

        if (depth == MAX_COMMAND_ENVELOPE_DEPTH || commands.size >= MAX_COMMANDS_PER_FRAME) return
        val fieldOccurrences = mutableMapOf<Int, Int>()
        message.entries.forEach { entry ->
            if (entry.wireType != WIRE_LENGTH_DELIMITED || commands.size >= MAX_COMMANDS_PER_FRAME) {
                return@forEach
            }
            val occurrence = fieldOccurrences.getOrDefault(entry.field, 0)
            fieldOccurrences[entry.field] = occurrence + 1
            inspectCommandEnvelope(
                data = data,
                start = entry.valueStart,
                end = entry.valueEnd,
                path = "$path.f${entry.field}[$occurrence]",
                depth = depth + 1,
                commands = commands,
                referencedServices = referencedServices
            )
        }
    }

    private fun collectPayloadCandidates(
        data: ByteArray,
        start: Int,
        end: Int,
        path: String,
        wrapperDepth: Int,
        candidates: MutableList<PayloadCandidate>
    ) {
        if (candidates.size >= MAX_PAYLOAD_CANDIDATES_PER_COMMAND) return
        val direct = data.copyOfRange(start, end)
        if (candidates.none { it.bytes.contentEquals(direct) }) {
            candidates += PayloadCandidate(bytes = direct, path = path)
        }

        if (wrapperDepth >= MAX_PAYLOAD_WRAPPER_DEPTH ||
            candidates.size >= MAX_PAYLOAD_CANDIDATES_PER_COMMAND
        ) {
            return
        }
        val wrapper = parseProtoMessage(data, start, end) ?: return
        val lengthEntries = wrapper.entries.filter { it.wireType == WIRE_LENGTH_DELIMITED }
        if (lengthEntries.isEmpty()) return

        val fieldOccurrences = mutableMapOf<Int, Int>()
        lengthEntries.forEach { entry ->
            if (candidates.size >= MAX_PAYLOAD_CANDIDATES_PER_COMMAND) return@forEach
            val occurrence = fieldOccurrences.getOrDefault(entry.field, 0)
            fieldOccurrences[entry.field] = occurrence + 1
            collectPayloadCandidates(
                data = data,
                start = entry.valueStart,
                end = entry.valueEnd,
                path = "$path.f${entry.field}[$occurrence]",
                wrapperDepth = wrapperDepth + 1,
                candidates = candidates
            )
        }
    }

    private fun buildStructuralDiagnostic(
        sensorData: SensorDataWx,
        analyses: List<PayloadAnalysis>
    ): String {
        val heartRateCommands = sensorData.commands.filter { it.service == HEART_RATE_SERVICE }
        val commandText = if (heartRateCommands.isEmpty()) {
            "none"
        } else {
            heartRateCommands.take(MAX_DIAGNOSTIC_COMMANDS).joinToString("|") { command ->
                val commandAnalyses = analyses.filter { it.command === command }
                val candidates = if (commandAnalyses.isEmpty()) {
                    "none"
                } else {
                    commandAnalyses.take(MAX_DIAGNOSTIC_PAYLOADS_PER_COMMAND)
                        .joinToString(",") { analysis ->
                            val relativePath = analysis.candidate.path.removePrefix(command.path)
                            val failures = analysis.failures
                                .joinToString("+") { it.diagnosticCode }
                                .ifEmpty { "ok" }
                            "$relativePath:${analysis.candidate.bytes.size}:$failures"
                        }
                }
                "${command.path}{${command.shape};p3x${command.directPayloadCount};c=$candidates}"
            }
        }
        val diagnostic =
            "log=${sensorData.logType};top=${sensorData.topLevelShape};hr=$commandText"
        return diagnostic.take(MAX_DIAGNOSTIC_SIGNATURE_LENGTH)
    }

    private fun parseProtoMessage(data: ByteArray, start: Int, end: Int): ProtoMessage? {
        if (start < 0 || end < start || end > data.size || end - start > MAX_PROTO_MESSAGE_LENGTH) {
            return null
        }
        var index = start
        val entries = mutableListOf<ProtoEntry>()

        while (index < end) {
            if (entries.size >= MAX_PROTO_FIELDS) return null
            val key = readVarint(data, index, end) ?: return null
            index = key.nextIndex
            val fieldLong = key.value ushr 3
            if (fieldLong <= 0 || fieldLong > MAX_PROTO_FIELD_NUMBER) return null
            val field = fieldLong.toInt()
            val wireType = (key.value and 0x07).toInt()

            when (wireType) {
                WIRE_VARINT -> {
                    val value = readVarint(data, index, end) ?: return null
                    entries += ProtoEntry(
                        field = field,
                        wireType = wireType,
                        varintValue = value.value,
                        valueStart = index,
                        valueEnd = value.nextIndex
                    )
                    index = value.nextIndex
                }

                WIRE_LENGTH_DELIMITED -> {
                    val value = readLengthDelimited(data, index, end) ?: return null
                    entries += ProtoEntry(
                        field = field,
                        wireType = wireType,
                        valueStart = value.startIndex,
                        valueEnd = value.endIndex
                    )
                    index = value.endIndex
                }

                WIRE_FIXED64 -> {
                    if (end - index < 8) return null
                    entries += ProtoEntry(
                        field = field,
                        wireType = wireType,
                        valueStart = index,
                        valueEnd = index + 8
                    )
                    index += 8
                }

                WIRE_FIXED32 -> {
                    if (end - index < 4) return null
                    entries += ProtoEntry(
                        field = field,
                        wireType = wireType,
                        valueStart = index,
                        valueEnd = index + 4
                    )
                    index += 4
                }

                else -> return null
            }
        }

        return ProtoMessage(entries = entries, shape = buildProtoShape(entries))
    }

    private fun buildProtoShape(entries: List<ProtoEntry>): String =
        entries.take(MAX_DIAGNOSTIC_SHAPE_FIELDS).joinToString(",") { entry ->
            if (entry.wireType == WIRE_LENGTH_DELIMITED) {
                "f${entry.field}/${entry.wireType}:${entry.valueEnd - entry.valueStart}"
            } else {
                "f${entry.field}/${entry.wireType}"
            }
        }.let { shape ->
            if (entries.size > MAX_DIAGNOSTIC_SHAPE_FIELDS) "$shape,..." else shape
        }.ifEmpty { "empty" }

    private fun readVarint(data: ByteArray, start: Int, end: Int): VarintRead? {
        var value = 0L
        var shift = 0
        var index = start

        while (index < end && shift < 64) {
            val byte = data[index++].toInt().and(0xFF)
            value = value or ((byte and 0x7F).toLong() shl shift)
            if (byte and 0x80 == 0) return VarintRead(value, index)
            shift += 7
        }

        return null
    }

    private fun readLengthDelimited(
        data: ByteArray,
        start: Int,
        end: Int
    ): LengthDelimitedRead? {
        val length = readVarint(data, start, end) ?: return null
        if (length.value > Int.MAX_VALUE) return null

        val valueEnd = length.nextIndex + length.value.toInt()
        if (valueEnd < length.nextIndex || valueEnd > end) return null
        return LengthDelimitedRead(
            startIndex = length.nextIndex,
            endIndex = valueEnd
        )
    }

    private enum class PayloadValidationFailure(val diagnosticCode: String) {
        LENGTH("len"),
        STATUS_TAIL("tail"),
        BPM_RANGE("bpm_range")
    }

    private data class SensorDataWx(
        val sequence: Int,
        val logType: Int,
        val commands: List<RtBuddyCommand>,
        val referencedServices: Set<Int>,
        val topLevelShape: String
    )

    private data class RtBuddyCommand(
        val service: Int,
        val directPayloadCount: Int,
        val payloadCandidates: List<PayloadCandidate>,
        val path: String,
        val shape: String
    )

    private data class PayloadCandidate(
        val bytes: ByteArray,
        val path: String
    )

    private data class PayloadAnalysis(
        val command: RtBuddyCommand,
        val candidate: PayloadCandidate,
        val failures: Set<PayloadValidationFailure>
    )

    private data class ProtoMessage(
        val entries: List<ProtoEntry>,
        val shape: String
    )

    private data class ProtoEntry(
        val field: Int,
        val wireType: Int,
        val varintValue: Long? = null,
        val valueStart: Int,
        val valueEnd: Int
    )

    private data class FrameClassification(
        val isHeartRateRelated: Boolean = false,
        val sample: HeartRateSample? = null,
        val rejectionReason: HeartRateRejectionReason? = null,
        val structuralDiagnostic: String? = null
    )

    private data class VarintRead(
        val value: Long,
        val nextIndex: Int
    )

    private data class LengthDelimitedRead(
        val startIndex: Int,
        val endIndex: Int
    )

    private companion object {
        const val AACP_RTBUDDY_HEADER_LENGTH = 12
        const val MAX_RTBUDDY_PAYLOAD_LENGTH = 16 * 1024
        const val MIN_SENSITIVE_PREFIX_LENGTH = 5

        // AirPods firmware has emitted live HEARTRATE records using both log types and different
        // exact status trailers depending on whether one or both earbuds participate in the session.
        // Keep this an exact whitelist: the trailer is the discriminator that prevents startup/control
        // service-19 records becoming BPM.
        val LIVE_SENSOR_DATA_LOG_TYPES = setOf(1, 3)
        val KNOWN_HEART_RATE_STATUS_TAILS = arrayOf(
            byteArrayOf(0x10, 0x00, 0x00),
            byteArrayOf(0x20, 0x00, 0x00),
            byteArrayOf(0x20, 0x02, 0x80.toByte()),
            byteArrayOf(0x20, 0x82.toByte(), 0x80.toByte())
        )
        const val HEART_RATE_SERVICE = 19
        const val HEART_RATE_PAYLOAD_LENGTH = 18
        const val HEART_RATE_BPM_OFFSET = 1
        const val HEART_RATE_STATUS_TAIL_LENGTH = 3
        const val HEART_RATE_STATUS_TAIL_OFFSET =
            HEART_RATE_PAYLOAD_LENGTH - HEART_RATE_STATUS_TAIL_LENGTH
        const val MIN_BPM = 30
        const val MAX_BPM = 220

        val SENSOR_DATA_COMMAND_FIELDS = setOf(5, 7, 8, 9, 12)

        const val MAX_COMMAND_ENVELOPE_DEPTH = 3
        const val MAX_PAYLOAD_WRAPPER_DEPTH = 3
        const val MAX_COMMANDS_PER_FRAME = 16
        const val MAX_PAYLOAD_CANDIDATES_PER_COMMAND = 12
        const val MAX_PROTO_MESSAGE_LENGTH = MAX_RTBUDDY_PAYLOAD_LENGTH
        const val MAX_PROTO_FIELDS = 96
        const val MAX_PROTO_FIELD_NUMBER = 4_096L

        const val MAX_DIAGNOSTIC_SHAPE_FIELDS = 12
        const val MAX_DIAGNOSTIC_COMMANDS = 4
        const val MAX_DIAGNOSTIC_PAYLOADS_PER_COMMAND = 5
        const val MAX_DIAGNOSTIC_SIGNATURE_LENGTH = 480
        const val MALFORMED_STRUCTURE_DIAGNOSTIC = "malformed_sensor_data;raw=suppressed"

        const val WIRE_VARINT = 0
        const val WIRE_FIXED64 = 1
        const val WIRE_LENGTH_DELIMITED = 2
        const val WIRE_FIXED32 = 5

        // type=0x0004, service=0x0004, opcode=0x0017, descriptor=0x00100000
        val RTBUDDY_FRAME_PREFIX = byteArrayOf(
            0x04, 0x00, 0x04, 0x00,
            0x17, 0x00,
            0x00, 0x00, 0x10, 0x00
        )
    }
}

private fun ByteArray.readLe16(offset: Int): Int =
    this[offset].toInt().and(0xFF) or (this[offset + 1].toInt().and(0xFF) shl 8)

private fun ByteArray.indexOfPrefix(prefix: ByteArray, startIndex: Int): Int {
    if (prefix.isEmpty()) return startIndex.coerceAtMost(size)
    val lastStart = size - prefix.size
    if (startIndex > lastStart) return -1

    for (start in startIndex.coerceAtLeast(0)..lastStart) {
        var matches = true
        for (offset in prefix.indices) {
            if (this[start + offset] != prefix[offset]) {
                matches = false
                break
            }
        }
        if (matches) return start
    }
    return -1
}

private fun ByteArray.longestSuffixMatchingPrefix(
    prefix: ByteArray,
    startIndex: Int
): Int {
    val available = size - startIndex.coerceIn(0, size)
    val maxLength = minOf(available, prefix.size - 1)
    for (length in maxLength downTo 1) {
        var matches = true
        val start = size - length
        for (offset in 0 until length) {
            if (this[start + offset] != prefix[offset]) {
                matches = false
                break
            }
        }
        if (matches) return length
    }
    return 0
}
