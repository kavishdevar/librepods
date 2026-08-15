package me.kavishdevar.librepods.health

import java.time.Instant
import me.kavishdevar.librepods.bluetooth.HeartRateSample
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthConnectHeartRateExporterTest {
    @Test
    fun detailedBatchPreservesEverySampleAndTimestamp() {
        val samples = listOf(
            sample(bpm = 72, sequence = 1, timestamp = 1_000L),
            sample(bpm = 74, sequence = 2, timestamp = 2_000L),
            sample(bpm = 73, sequence = 3, timestamp = 3_000L)
        )

        val recordSamples = buildHeartRateRecordSamples(
            samples = samples,
            preserveSamples = true,
            averageSampleTimeMillis = 2_000L
        )

        assertEquals(listOf(72L, 74L, 73L), recordSamples.map { it.beatsPerMinute })
        assertEquals(
            listOf(
                Instant.ofEpochMilli(1_000L),
                Instant.ofEpochMilli(2_000L),
                Instant.ofEpochMilli(3_000L)
            ),
            recordSamples.map { it.time }
        )
    }

    @Test
    fun existingModesStillCreateOneRoundedAverage() {
        val recordSamples = buildHeartRateRecordSamples(
            samples = listOf(
                sample(bpm = 70, sequence = 1, timestamp = 1_000L),
                sample(bpm = 71, sequence = 2, timestamp = 2_000L)
            ),
            preserveSamples = false,
            averageSampleTimeMillis = 1_500L
        )

        assertEquals(1, recordSamples.size)
        assertEquals(71L, recordSamples.single().beatsPerMinute)
        assertEquals(Instant.ofEpochMilli(1_500L), recordSamples.single().time)
    }

    @Test
    fun batchIntervalIsBoundedAndRoundedToThirtySeconds() {
        assertEquals(30, normalizeHeartRateBatchIntervalSeconds(1))
        assertEquals(60, normalizeHeartRateBatchIntervalSeconds(46))
        assertEquals(330, normalizeHeartRateBatchIntervalSeconds(329))
        assertEquals(15 * 60, normalizeHeartRateBatchIntervalSeconds(60 * 60))
    }

    private fun sample(bpm: Int, sequence: Int, timestamp: Long) = HeartRateSample(
        bpm = bpm,
        sequence = sequence,
        receivedAtMillis = timestamp,
        receivedAtElapsedRealtime = timestamp
    )
}
