package me.kavishdevar.librepods.audio

import java.io.File
import java.io.RandomAccessFile

class WavWriter(
    file: File,
    private val sampleRate: Int = 64_000,
    private val channels: Int = 1,
    private val bitsPerSample: Int = 16
) : AutoCloseable {

    private val raf = RandomAccessFile(file, "rw")
    private var dataSize = 0L

    init {
        writeHeader()
    }

    fun write(pcm: ByteArray) {
        raf.write(pcm)
        dataSize += pcm.size
    }

    override fun close() {
        raf.seek(4)
        raf.writeIntLE((36 + dataSize).toInt())

        raf.seek(40)
        raf.writeIntLE(dataSize.toInt())

        raf.close()
    }

    private fun writeHeader() {
        raf.writeBytes("RIFF")
        raf.writeIntLE(0)

        raf.writeBytes("WAVE")

        raf.writeBytes("fmt ")
        raf.writeIntLE(16)
        raf.writeShortLE(1)

        raf.writeShortLE(channels.toShort())

        raf.writeIntLE(sampleRate)

        val byteRate = sampleRate * channels * bitsPerSample / 8
        raf.writeIntLE(byteRate)

        val blockAlign = channels * bitsPerSample / 8
        raf.writeShortLE(blockAlign.toShort())

        raf.writeShortLE(bitsPerSample.toShort())

        raf.writeBytes("data")
        raf.writeIntLE(0)
    }
}

private fun RandomAccessFile.writeIntLE(value: Int) {
    write(value and 0xff)
    write((value ushr 8) and 0xff)
    write((value ushr 16) and 0xff)
    write((value ushr 24) and 0xff)
}

private fun RandomAccessFile.writeShortLE(value: Short) {
    write(value.toInt() and 0xff)
    write((value.toInt() ushr 8) and 0xff)
}
