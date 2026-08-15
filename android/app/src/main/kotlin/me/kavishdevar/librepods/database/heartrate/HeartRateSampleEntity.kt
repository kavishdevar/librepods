package me.kavishdevar.librepods.database.heartrate

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlin.time.Instant

@Entity
data class HeartRateSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val timestamp: Instant,
    val bpm: Int,
)
