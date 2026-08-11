package me.kavishdevar.librepods.data.audio

data class MicrophoneState(
    val isActive: Boolean = false,

    val packetsReceived: Long = 0,
    val decodeErrors: Long = 0,

    val durationMs: Long = 0,

    val level: Float = 0f,
)
