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
    UNSUPPORTED_LOG_TYPE,
    MISSING_HEART_RATE_PAYLOAD,
    UNRECOGNIZED_HEART_RATE_PAYLOAD
}

internal data class HeartRateDecodeResult(
    val samples: List<HeartRateSample> = emptyList(),
    val relatedFrameCount: Int = 0,
    val rejectionReasons: Map<HeartRateRejectionReason, Int> = emptyMap(),
    val suppressRawLogging: Boolean = false,
    val passthroughPackets: List<ByteArray> = emptyList()
) {
    val rejectedFrameCount: Int
        get() = rejectionReasons.values.sum()
}

/**
 * Reassembles RTBuddy frames and extracts the verified HEARTRATE SensorDataWX payload.
 *
 * The parser deliberately keeps the protocol checks that prevent control/startup frames from being
 * interpreted as BPM: live log type, service 19, exact 18-byte payload, known status trailer, and
 * the validated physiological range. Length-delimited wrappers are traversed only to the same
 * bounded depth as the observed firmware variants.
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
        val data = if (carry.isEmpty()) chunk else carry + chunk
        carry = ByteArray(0)

        val samples = mutableListOf<HeartRateSample>()
        val passthroughPackets = mutableListOf<ByteArray>()
        val rejectionReasons = mutableMapOf<HeartRateRejectionReason, Int>()
        var relatedFrameCount = 0
        var suppressRawLogging = carryWasSensitive
        var cursor = 0

        while (cursor < data.size) {
            val frameOffset = data.indexOfPrefix(RTBUDDY_FRAME_PREFIX, cursor)
            if (frameOffset < 0) {
                val suffixLength = data.longestSuffixMatchingPrefix(
                    prefix = RTBUDDY_FRAME_PREFIX,
                    startIndex = cursor
                )
                val passthroughEnd = data.size - suffixLength
                if (passthroughEnd > cursor) {
                    passthroughPackets += data.copyOfRange(cursor, passthroughEnd)
                }
                if (suffixLength > 0) {
                    carry = data.copyOfRange(passthroughEnd, data.size)
                    suppressRawLogging = suppressRawLogging ||
                        suffixLength >= MIN_SENSITIVE_PREFIX_LENGTH
                }
                break
            }

            if (frameOffset > cursor) {
                passthroughPackets += data.copyOfRange(cursor, frameOffset)
            }
            if (data.size - frameOffset < AACP_RTBUDDY_HEADER_LENGTH) {
                carry = data.copyOfRange(frameOffset, data.size)
                suppressRawLogging = true
                break
            }

            val payloadLength = data.readLe16(frameOffset + 10)
            if (payloadLength > MAX_RTBUDDY_PAYLOAD_LENGTH) {
                // The exact SensorDataWX prefix is sensitive, but the declared length is untrusted.
                suppressRawLogging = true
                break
            }

            val frameLength = AACP_RTBUDDY_HEADER_LENGTH + payloadLength
            if (data.size - frameOffset < frameLength) {
                carry = data.copyOfRange(frameOffset, data.size)
                suppressRawLogging = true
                break
            }

            val frame = data.copyOfRange(frameOffset, frameOffset + frameLength)
            val classification = classifyFrame(frame)
            if (classification.related) {
                relatedFrameCount++
                classification.rejectionReason?.let { rejectionReasons.increment(it) }
                classification.sample?.let(samples::add)
                suppressRawLogging = true
            } else {
                passthroughPackets += frame
                if (hadCarry && frameOffset == 0) suppressRawLogging = true
            }
            cursor = frameOffset + frameLength
        }

        return HeartRateDecodeResult(
            samples = samples,
            relatedFrameCount = relatedFrameCount,
            rejectionReasons = rejectionReasons,
            suppressRawLogging = suppressRawLogging,
            passthroughPackets = passthroughPackets
        )
    }

    private fun classifyFrame(frame: ByteArray): FrameClassification {
        val topLevel = parseProtoMessage(
            frame,
            AACP_RTBUDDY_HEADER_LENGTH,
            frame.size
        ) ?: return FrameClassification()

        val sequence = topLevel.firstVarint(FIELD_SEQUENCE)?.toInt() ?: -1
        val logType = topLevel.firstVarint(FIELD_LOG_TYPE)?.toInt() ?: -1
        val commands = mutableListOf<HeartRateCommand>()

        topLevel.fields.forEach { field ->
            if (field.wireType == WIRE_LENGTH_DELIMITED &&
                field.number in SENSOR_DATA_COMMAND_FIELDS &&
                commands.size < MAX_COMMANDS_PER_FRAME
            ) {
                collectHeartRateCommands(
                    data = frame,
                    start = field.valueStart,
                    end = field.valueEnd,
                    depth = 0,
                    commands = commands
                )
            }
        }

        if (commands.isEmpty()) return FrameClassification()
        if (logType !in LIVE_SENSOR_DATA_LOG_TYPES) {
            return FrameClassification(
                related = true,
                rejectionReason = HeartRateRejectionReason.UNSUPPORTED_LOG_TYPE
            )
        }

        val payloads = commands.flatMap { it.payloadCandidates }
        val acceptedPayload = payloads.firstOrNull(::isValidHeartRatePayload)
            ?: return FrameClassification(
                related = true,
                rejectionReason = if (payloads.isEmpty()) {
                    HeartRateRejectionReason.MISSING_HEART_RATE_PAYLOAD
                } else {
                    HeartRateRejectionReason.UNRECOGNIZED_HEART_RATE_PAYLOAD
                }
            )

        return FrameClassification(
            related = true,
            sample = HeartRateSample(
                bpm = acceptedPayload.unsignedByteAt(HEART_RATE_BPM_OFFSET),
                sequence = sequence,
                receivedAtMillis = System.currentTimeMillis(),
                receivedAtElapsedRealtime = SystemClock.elapsedRealtime()
            )
        )
    }

    private fun collectHeartRateCommands(
        data: ByteArray,
        start: Int,
        end: Int,
        depth: Int,
        commands: MutableList<HeartRateCommand>
    ) {
        if (depth > MAX_COMMAND_ENVELOPE_DEPTH || commands.size >= MAX_COMMANDS_PER_FRAME) return
        val message = parseProtoMessage(data, start, end) ?: return
        val service = message.firstVarint(FIELD_SERVICE)?.toInt()

        if (service == HEART_RATE_SERVICE) {
            val payloads = mutableListOf<ByteArray>()
            message.fields.forEach { field ->
                if (field.number == FIELD_COMMAND_PAYLOAD &&
                    field.wireType == WIRE_LENGTH_DELIMITED
                ) {
                    collectPayloadCandidates(
                        data = data,
                        start = field.valueStart,
                        end = field.valueEnd,
                        depth = 0,
                        candidates = payloads
                    )
                }
            }
            commands += HeartRateCommand(payloads)
        }

        if (depth == MAX_COMMAND_ENVELOPE_DEPTH) return
        message.fields.forEach { field ->
            if (field.wireType == WIRE_LENGTH_DELIMITED &&
                commands.size < MAX_COMMANDS_PER_FRAME
            ) {
                collectHeartRateCommands(
                    data = data,
                    start = field.valueStart,
                    end = field.valueEnd,
                    depth = depth + 1,
                    commands = commands
                )
            }
        }
    }

    private fun collectPayloadCandidates(
        data: ByteArray,
        start: Int,
        end: Int,
        depth: Int,
        candidates: MutableList<ByteArray>
    ) {
        if (candidates.size >= MAX_PAYLOAD_CANDIDATES_PER_COMMAND) return

        val direct = data.copyOfRange(start, end)
        if (candidates.none(direct::contentEquals)) candidates += direct
        if (depth >= MAX_PAYLOAD_WRAPPER_DEPTH) return

        val wrapper = parseProtoMessage(data, start, end) ?: return
        wrapper.fields.forEach { field ->
            if (field.wireType == WIRE_LENGTH_DELIMITED &&
                candidates.size < MAX_PAYLOAD_CANDIDATES_PER_COMMAND
            ) {
                collectPayloadCandidates(
                    data = data,
                    start = field.valueStart,
                    end = field.valueEnd,
                    depth = depth + 1,
                    candidates = candidates
                )
            }
        }
    }

    private fun isValidHeartRatePayload(payload: ByteArray): Boolean {
        if (payload.size != HEART_RATE_PAYLOAD_LENGTH) return false
        if (payload.unsignedByteAt(HEART_RATE_BPM_OFFSET) !in MIN_BPM..MAX_BPM) return false

        return KNOWN_HEART_RATE_STATUS_TAILS.any { tail ->
            tail.indices.all { index ->
                payload[HEART_RATE_STATUS_TAIL_OFFSET + index] == tail[index]
            }
        }
    }

    private fun parseProtoMessage(data: ByteArray, start: Int, end: Int): ProtoMessage? {
        if (start < 0 || end < start || end > data.size || end - start > MAX_PROTO_MESSAGE_LENGTH) {
            return null
        }

        val fields = mutableListOf<ProtoField>()
        var index = start
        while (index < end) {
            if (fields.size >= MAX_PROTO_FIELDS) return null
            val key = readVarint(data, index, end) ?: return null
            index = key.nextIndex

            val fieldNumber = key.value ushr 3
            if (fieldNumber <= 0 || fieldNumber > MAX_PROTO_FIELD_NUMBER) return null
            val wireType = (key.value and 0x07).toInt()

            when (wireType) {
                WIRE_VARINT -> {
                    val value = readVarint(data, index, end) ?: return null
                    fields += ProtoField(
                        number = fieldNumber.toInt(),
                        wireType = wireType,
                        varintValue = value.value,
                        valueStart = index,
                        valueEnd = value.nextIndex
                    )
                    index = value.nextIndex
                }

                WIRE_LENGTH_DELIMITED -> {
                    val length = readVarint(data, index, end) ?: return null
                    if (length.value > Int.MAX_VALUE) return null
                    val valueEnd = length.nextIndex + length.value.toInt()
                    if (valueEnd < length.nextIndex || valueEnd > end) return null

                    fields += ProtoField(
                        number = fieldNumber.toInt(),
                        wireType = wireType,
                        valueStart = length.nextIndex,
                        valueEnd = valueEnd
                    )
                    index = valueEnd
                }

                WIRE_FIXED64 -> {
                    if (end - index < 8) return null
                    fields += ProtoField(
                        number = fieldNumber.toInt(),
                        wireType = wireType,
                        valueStart = index,
                        valueEnd = index + 8
                    )
                    index += 8
                }

                WIRE_FIXED32 -> {
                    if (end - index < 4) return null
                    fields += ProtoField(
                        number = fieldNumber.toInt(),
                        wireType = wireType,
                        valueStart = index,
                        valueEnd = index + 4
                    )
                    index += 4
                }

                else -> return null
            }
        }
        return ProtoMessage(fields)
    }

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

    private fun ByteArray.unsignedByteAt(index: Int): Int = this[index].toInt().and(0xFF)

    private fun <K> MutableMap<K, Int>.increment(key: K) {
        this[key] = getOrDefault(key, 0) + 1
    }

    private data class HeartRateCommand(val payloadCandidates: List<ByteArray>)

    private data class ProtoMessage(val fields: List<ProtoField>) {
        fun firstVarint(fieldNumber: Int): Long? = fields.firstOrNull {
            it.number == fieldNumber && it.wireType == WIRE_VARINT
        }?.varintValue
    }

    private data class ProtoField(
        val number: Int,
        val wireType: Int,
        val varintValue: Long? = null,
        val valueStart: Int,
        val valueEnd: Int
    )

    private data class VarintRead(val value: Long, val nextIndex: Int)

    private data class FrameClassification(
        val related: Boolean = false,
        val sample: HeartRateSample? = null,
        val rejectionReason: HeartRateRejectionReason? = null
    )

    private companion object {
        const val AACP_RTBUDDY_HEADER_LENGTH = 12
        const val MAX_RTBUDDY_PAYLOAD_LENGTH = 16 * 1024
        const val MIN_SENSITIVE_PREFIX_LENGTH = 5

        // Firmware has emitted live records with both log types and different exact status trailers
        // depending on whether one or both earbuds participate in the session.
        val LIVE_SENSOR_DATA_LOG_TYPES = setOf(1, 3)
        val KNOWN_HEART_RATE_STATUS_TAILS = arrayOf(
            byteArrayOf(0x10, 0x00, 0x00),
            byteArrayOf(0x20, 0x00, 0x00),
            byteArrayOf(0x20, 0x02, 0x80.toByte()),
            byteArrayOf(0x20, 0x82.toByte(), 0x80.toByte())
        )

        const val FIELD_SEQUENCE = 1
        const val FIELD_LOG_TYPE = 2
        const val FIELD_SERVICE = 1
        const val FIELD_COMMAND_PAYLOAD = 3
        const val HEART_RATE_SERVICE = 19
        const val HEART_RATE_PAYLOAD_LENGTH = 18
        const val HEART_RATE_BPM_OFFSET = 1
        const val HEART_RATE_STATUS_TAIL_OFFSET = 15
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
        if (prefix.indices.all { this[start + it] == prefix[it] }) return start
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
        val start = size - length
        if ((0 until length).all { this[start + it] == prefix[it] }) return length
    }
    return 0
}
