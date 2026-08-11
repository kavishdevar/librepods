package me.kavishdevar.librepods.utils

fun calculateLevel(pcm: ByteArray): Float {
    if (pcm.isEmpty()) return 0f

    var sum = 0.0

    for (i in pcm.indices step 2) {
        val sample = ((pcm[i + 1].toInt() shl 8) or (pcm[i].toInt() and 0xff)).toShort()
        val normalized = sample / 32768.0
        sum += normalized * normalized
    }

    val rms = kotlin.math.sqrt(sum / (pcm.size / 2))

    return rms.toFloat().coerceIn(0f, 1f)
}
