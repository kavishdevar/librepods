package me.kavishdevar.librepods.database.heartrate

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlin.time.Instant

@Dao
interface HeartRateDao {
    @Insert
    suspend fun insert(sample: HeartRateSampleEntity)

    @Query("""
        SELECT * FROM HeartRateSampleEntity
        WHERE timestamp >= :start
        AND timestamp < :end
        ORDER BY timestamp ASC
    """)
    suspend fun get(
        start: Instant,
        end: Instant,
    ): List<HeartRateSampleEntity>
}
