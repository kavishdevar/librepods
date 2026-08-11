package me.kavishdevar.librepods.audio

import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaCodecInfo
import android.media.MediaFormat
import java.nio.ByteBuffer

class EldDecoder {

    companion object {
        private val ASC = byteArrayOf(
            0xF8.toByte(),
            0xE6.toByte(),
            0x30,
            0x00
        )
    }

    private val codec = MediaCodec.createDecoderByType(
        MediaFormat.MIMETYPE_AUDIO_AAC
    )

    private val info = BufferInfo()

    init {
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            64_000,
            1
        )

        format.setInteger(
            MediaFormat.KEY_AAC_PROFILE,
            MediaCodecInfo.CodecProfileLevel.AACObjectELD
        )

        format.setByteBuffer(
            "csd-0",
            ByteBuffer.wrap(ASC)
        )

        codec.configure(format, null, null, 0)
        codec.start()
    }

    fun decode(
        accessUnit: ByteArray,
        onPcm: (ByteArray) -> Unit
    ) {
        val input = codec.dequeueInputBuffer(10_000)

        if (input >= 0) {
            codec.getInputBuffer(input)?.apply {
                clear()
                put(accessUnit)
            }

            codec.queueInputBuffer(
                input,
                0,
                accessUnit.size,
                0,
                0
            )
        }

        while (true) {
            val output = codec.dequeueOutputBuffer(info, 0)

            when {
                output >= 0 -> {
                    val buffer = codec.getOutputBuffer(output)!!

                    val pcm = ByteArray(info.size)

                    buffer.position(info.offset)
                    buffer.limit(info.offset + info.size)
                    buffer.get(pcm)

                    codec.releaseOutputBuffer(output, false)

                    onPcm(pcm)
                }

                output == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    // ignore
                }

                output == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    break
                }

                else -> {
                    break
                }
            }
        }
    }

    fun close() {
        codec.stop()
        codec.release()
    }
}
