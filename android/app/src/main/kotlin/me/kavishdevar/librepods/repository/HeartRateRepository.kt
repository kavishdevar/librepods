package me.kavishdevar.librepods.repository

import me.kavishdevar.librepods.data.heartrate.HeartRateDao
import me.kavishdevar.librepods.data.heartrate.HeartRateSample
import me.kavishdevar.librepods.database.heartrate.HeartRateSampleEntity
import kotlin.time.Instant

class HeartRateRepository(
    private val dao: HeartRateDao,
) {
    suspend fun insert(sample: HeartRateSample) {
        dao.insert(sample.toEntity())
    }

    suspend fun get(
        start: Instant,
        end: Instant,
    ): List<HeartRateSample> = dao.get(start, end).map { it.toSample() }

    private fun HeartRateSample.toEntity() = HeartRateSampleEntity(
        timestamp = timestamp,
        bpm = bpm
    )

    private fun HeartRateSampleEntity.toSample() = HeartRateSample(
        bpm = bpm,
        timestamp = timestamp
    )
}
