package me.kavishdevar.librepods.data.recording

import java.io.File
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class Recording(
    val uuid: Uuid,
    val file: File,
    val createdAt: Instant
)
