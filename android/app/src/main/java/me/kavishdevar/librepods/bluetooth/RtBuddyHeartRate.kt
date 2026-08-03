/*
    LibrePods - AirPods liberated from Appleâ€™s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.
*/

package me.kavishdevar.librepods.bluetooth

/** A validated heart-rate sample decoded from an RTBuddy SensorDataWX frame. */
data class HeartRateSample(
    val bpm: Int,
    val sequence: Int,
    val receivedAtMillis: Long
)

internal data class HeartRateDecodeResult(
    val samples: List<HeartRateSample> = emptyList(),
    val relatedFrameCount: Int = 0,
    val rejectedFrameCount: Int = 0,
    val suppressRawLogging: Boolean = false,
    val passthroughPackets: List<ByteArray> = emptyList()
)

/**
 * Stateful decoder for the verified RTBuddy HEARTRATE SensorDataWX stream.
 *
 * Socket reads are arbitrary chunks. A possible partial 0x17/0x00100000 frame is retained until
 * its declared payload is complete. Other 0x17 packets are reconstructed and passed to the normal
 * AACP parser so head tracking keeps its existing behavior.
 */
internal class RtBuddyHeartRateDecoder {
    private var carry = ByteArray(0)

    fun reset() {
        carry = ByteArray(0)
    }

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
                if (classification.sample == null) rejectedFrameCount++
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
            suppressRawLogging = suppressRawLogging,
            passthroughPackets = passthroughPackets
        )
    }

    private fun classifyFrame(frame: ByteArray): FrameClassification {
        if (frame.size < AACP_RTBUDDY_HEADER_LENGTH) return FrameClassification()
        if (!frame.startsWithPrefix(RTBUDDY_FRAME_PREFIX)) return FrameClassification()

        val declaredLength = frame.readLe16(10)
        if (frame.size != AACP_RTBUDDY_HEADER_LENGTH + declaredLength) {
            return FrameClassification()
        }

        val hasHeartRateReference = hasHeartRateServiceReference(
            frame,
            AACP_RTBUDDY_HEADER_LENGTH,
            frame.size
        )
        val sensorData = parseSensorDataWx(frame, AACP_RTBUDDY_HEADER_LENGTH, frame.size)
            ?: return FrameClassification(isHeartRateRelated = hasHeartRateReference)
        val heartRateRelated = hasHeartRateReference ||
            HEART_RATE_SERVICE in sensorData.referencedServices
        if (!heartRateRelated || sensorData.logType !in SENSOR_DATA_LOG_STATES) {
            return FrameClassification(isHeartRateRelated = heartRateRelated)
        }

        val command = sensorData.commands.firstOrNull { command ->
            val payload = command.payload ?: return@firstOrNull false
            command.service == HEART_RATE_SERVICE &&
                payload.size == HEART_RATE_PAYLOAD_LENGTH &&
                payload[15] == 0x10.toByte() &&
                payload[16] == 0x00.toByte() &&
                payload[17] == 0x00.toByte() &&
                payload[1].toInt().and(0xFF) in MIN_BPM..MAX_BPM
        } ?: return FrameClassification(isHeartRateRelated = true)
        val payload = command.payload ?: return FrameClassification(isHeartRateRelated = true)

        return FrameClassification(
            isHeartRateRelated = true,
            sample = HeartRateSample(
                bpm = payload[1].toInt().and(0xFF),
                sequence = sensorData.sequence,
                receivedAtMillis = System.currentTimeMillis()
            )
        )
    }


    private fun hasHeartRateServiceReference(data: ByteArray, start: Int, end: Int): Boolean {
        var index = start
        while (index < end) {
            val key = readVarint(data, index, end) ?: return false
            index = key.nextIndex
            val field = (key.value ushr 3).toInt()
            val wireType = (key.value and 0x07).toInt()

            when (wireType) {
                WIRE_VARINT -> {
                    val value = readVarint(data, index, end) ?: return false
                    index = value.nextIndex
                }

                WIRE_LENGTH_DELIMITED -> {
                    val length = readVarint(data, index, end) ?: return false
                    if (length.value > Int.MAX_VALUE) return false
                    index = length.nextIndex
                    val subEnd = index + length.value.toInt()
                    if (subEnd < index || subEnd > end) return false
                    if (field in HEART_RATE_SERVICE_REFERENCE_FIELDS &&
                        parseReferencedService(data, index, subEnd) == HEART_RATE_SERVICE
                    ) {
                        return true
                    }
                    index = subEnd
                }

                WIRE_FIXED64 -> {
                    if (end - index < 8) return false
                    index += 8
                }

                WIRE_FIXED32 -> {
                    if (end - index < 4) return false
                    index += 4
                }

                else -> return false
            }
        }
        return false
    }

    private fun parseSensorDataWx(data: ByteArray, start: Int, end: Int): SensorDataWx? {
        var index = start
        var sequence = -1
        var logType = -1
        val commands = mutableListOf<RtBuddyCommand>()
        val referencedServices = mutableSetOf<Int>()

        while (index < end) {
            val key = readVarint(data, index, end) ?: return null
            index = key.nextIndex
            val field = (key.value ushr 3).toInt()
            val wireType = (key.value and 0x07).toInt()

            when (wireType) {
                WIRE_VARINT -> {
                    val value = readVarint(data, index, end) ?: return null
                    index = value.nextIndex
                    when (field) {
                        1 -> sequence = value.value.toInt()
                        2 -> logType = value.value.toInt()
                    }
                }

                WIRE_LENGTH_DELIMITED -> {
                    val length = readVarint(data, index, end) ?: return null
                    index = length.nextIndex
                    if (length.value > Int.MAX_VALUE) return null
                    val subEnd = index + length.value.toInt()
                    if (subEnd < index || subEnd > end) return null

                    when (field) {
                        5, 8, 9, 12 -> parseReferencedService(data, index, subEnd)
                            ?.let(referencedServices::add)

                        7 -> {
                            val command = parseCommand(data, index, subEnd)
                            if (command != null) {
                                commands += command
                                if (command.service >= 0) referencedServices += command.service
                            } else {
                                parseReferencedService(data, index, subEnd)
                                    ?.let(referencedServices::add)
                            }
                        }
                    }
                    index = subEnd
                }

                WIRE_FIXED64 -> {
                    if (end - index < 8) return null
                    index += 8
                }

                WIRE_FIXED32 -> {
                    if (end - index < 4) return null
                    index += 4
                }

                else -> return null
            }
        }

        return SensorDataWx(
            sequence = sequence,
            logType = logType,
            commands = commands,
            referencedServices = referencedServices
        )
    }

    private fun parseCommand(data: ByteArray, start: Int, end: Int): RtBuddyCommand? {
        var index = start
        var service = -1
        var payload: ByteArray? = null
        var duplicatePayload = false

        while (index < end) {
            val key = readVarint(data, index, end) ?: return null
            index = key.nextIndex
            val field = (key.value ushr 3).toInt()
            val wireType = (key.value and 0x07).toInt()

            when (wireType) {
                WIRE_VARINT -> {
                    val value = readVarint(data, index, end) ?: return null
                    index = value.nextIndex
                    if (field == 1) service = value.value.toInt()
                }

                WIRE_LENGTH_DELIMITED -> {
                    val length = readVarint(data, index, end) ?: return null
                    index = length.nextIndex
                    if (length.value > Int.MAX_VALUE) return null
                    val subEnd = index + length.value.toInt()
                    if (subEnd < index || subEnd > end) return null
                    if (field == 3) {
                        if (payload != null) {
                            duplicatePayload = true
                        } else {
                            payload = data.copyOfRange(index, subEnd)
                        }
                    }
                    index = subEnd
                }

                WIRE_FIXED64 -> {
                    if (end - index < 8) return null
                    index += 8
                }

                WIRE_FIXED32 -> {
                    if (end - index < 4) return null
                    index += 4
                }

                else -> return null
            }
        }

        return RtBuddyCommand(
            service = service,
            payload = if (duplicatePayload) null else payload
        )
    }


    private fun parseReferencedService(data: ByteArray, start: Int, end: Int): Int? {
        var index = start
        while (index < end) {
            val key = readVarint(data, index, end) ?: return null
            index = key.nextIndex
            val field = (key.value ushr 3).toInt()
            val wireType = (key.value and 0x07).toInt()

            when (wireType) {
                WIRE_VARINT -> {
                    val value = readVarint(data, index, end) ?: return null
                    index = value.nextIndex
                    if (field == 1) return value.value.toInt()
                }

                WIRE_LENGTH_DELIMITED -> {
                    val length = readVarint(data, index, end) ?: return null
                    if (length.value > Int.MAX_VALUE) return null
                    val nextIndex = length.nextIndex + length.value.toInt()
                    if (nextIndex < length.nextIndex || nextIndex > end) return null
                    index = nextIndex
                }

                WIRE_FIXED64 -> {
                    if (end - index < 8) return null
                    index += 8
                }

                WIRE_FIXED32 -> {
                    if (end - index < 4) return null
                    index += 4
                }

                else -> return null
            }
        }
        return null
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

    private data class SensorDataWx(
        val sequence: Int,
        val logType: Int,
        val commands: List<RtBuddyCommand>,
        val referencedServices: Set<Int>
    )

    private data class RtBuddyCommand(
        val service: Int,
        val payload: ByteArray?
    )

    private data class FrameClassification(
        val isHeartRateRelated: Boolean = false,
        val sample: HeartRateSample? = null
    )

    private data class VarintRead(
        val value: Long,
        val nextIndex: Int
    )

    private companion object {
        const val AACP_RTBUDDY_HEADER_LENGTH = 12
        const val MAX_RTBUDDY_PAYLOAD_LENGTH = 16 * 1024
        const val MIN_SENSITIVE_PREFIX_LENGTH = 5

        // AirPods firmware has been observed using both 1 and 3 for live SensorDataWX records.
        val SENSOR_DATA_LOG_STATES = setOf(1, 3)
        const val HEART_RATE_SERVICE = 19
        const val HEART_RATE_PAYLOAD_LENGTH = 18
        const val MIN_BPM = 30
        const val MAX_BPM = 220

        val HEART_RATE_SERVICE_REFERENCE_FIELDS = setOf(5, 7, 8, 9, 12)


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

private fun ByteArray.startsWithPrefix(prefix: ByteArray): Boolean {
    if (size < prefix.size) return false
    for (index in prefix.indices) {
        if (this[index] != prefix[index]) return false
    }
    return true
}

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


