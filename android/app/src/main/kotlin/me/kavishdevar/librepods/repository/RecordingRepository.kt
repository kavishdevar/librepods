package me.kavishdevar.librepods.repository

import android.content.Context
import android.os.Environment
import me.kavishdevar.librepods.data.recording.Recording
import java.io.File
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

class RecordingRepository(
    context: Context
) {
    private val recordingsDir = context.getExternalFilesDir(Environment.DIRECTORY_RECORDINGS)?: File(
        context.getExternalFilesDir(null),
        "Recordings"
    )

    fun createRecording(): Recording {
        val now = Clock.System.now()

        val uuid = Uuid.random()

        val file = File(
            recordingsDir,
            "${uuid}_${now.toEpochMilliseconds()}.wav"
        )

        return Recording(
            uuid = uuid,
            file = file,
            createdAt = now
        )
    }

    fun recordings(): List<Recording> =
        recordingsDir
            .listFiles { f -> f.extension == "wav" }
            ?.sortedByDescending(File::lastModified)
            ?.map {
                Recording(
                    uuid = Uuid.parse(it.name.split("_")[0]),
                    file = it,
                    createdAt = Instant.fromEpochMilliseconds(it.lastModified())
                )
            }
            ?: emptyList()
}
