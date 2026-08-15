package me.kavishdevar.librepods.data.heartrate

import kotlin.time.Instant

data class HeartRateSample (
    val bpm: Int,
    val timestamp: Instant
)
// TODO: implement records (don't have a better place to put this)
