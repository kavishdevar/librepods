/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.
*/

package me.kavishdevar.librepods.bluetooth

import android.util.Log
import me.kavishdevar.librepods.wear.bluetooth.AirPodsProtocolTransport
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "ATTManager"

enum class ATTHandles(val value: Int) {
    TRANSPARENCY(0x18),
    LOUD_SOUND_REDUCTION(0x1B),
    HEARING_AID(0x2A)
}

enum class ATTCCCDHandles(val value: Int) {
    TRANSPARENCY(ATTHandles.TRANSPARENCY.value + 1),
    HEARING_AID(ATTHandles.HEARING_AID.value + 1)
}

/**
 * ATT protocol implementation with no dependency on global Bluetooth state.
 * The transport is supplied by the Wear connection session.
 */
class ATTManager(private val transport: AirPodsProtocolTransport) {
    val characteristicList = mutableMapOf<ATTHandles, ByteArray>()
    private val responseQueues = ConcurrentHashMap<Byte, LinkedBlockingQueue<ByteArray>>()
    private val readerRunning = AtomicBoolean(false)
    private var readerThread: Thread? = null
    private var onNotificationReceived: ((handle: Byte, value: ByteArray) -> Unit)? = null

    fun startReader() {
        if (readerRunning.getAndSet(true)) return
        readerThread = Thread {
            try {
                runReaderLoop()
            } catch (t: Throwable) {
                Log.e(TAG, "reader thread crashed: ${t.message}", t)
            } finally {
                readerRunning.set(false)
            }
        }.also { it.name = "ATT-Reader"; it.isDaemon = true; it.start() }
    }

    fun stopReader() {
        readerRunning.set(false)
        readerThread?.interrupt()
        readerThread = null
    }

    fun setOnNotificationReceived(listener: ((handle: Byte, value: ByteArray) -> Unit)?) {
        onNotificationReceived = listener
    }

    fun enableNotification(handle: ATTCCCDHandles) {
        writeCharacteristic(handle.value.toByte(), byteArrayOf(0x01))
    }

    fun getCharacteristic(handle: ATTHandles): ByteArray? {
        val stored = characteristicList[handle]
        return if (stored?.isNotEmpty() != true) readCharacteristic(handle) else stored
    }

    fun readCharacteristic(handle: ATTHandles, timeoutMillis: Long = 2000): ByteArray? {
        return try {
            val output = transport.attOutput
            val pdu = byteArrayOf(0x0A, handle.value.toByte(), 0x00)
            synchronized(output) {
                output.write(pdu)
                output.flush()
            }
            val response = waitForResponse(0x0B, timeoutMillis) ?: return null
            val value = response.copyOfRange(1, response.size)
            characteristicList[handle] = value
            value
        } catch (error: Exception) {
            Log.e(TAG, "error reading characteristic: ${error.message}", error)
            null
        }
    }

    fun writeCharacteristic(handle: ATTHandles, data: ByteArray, timeoutMillis: Long = 2000) {
        characteristicList[handle] = data
        writeCharacteristic(handle.value.toByte(), data, timeoutMillis)
    }

    fun writeCharacteristic(handle: Byte, data: ByteArray, timeoutMillis: Long = 2000) {
        try {
            val output = transport.attOutput
            val pdu = byteArrayOf(0x12, handle, 0x00) + data
            synchronized(output) {
                output.write(pdu)
                output.flush()
            }
            waitForResponse(0x13, timeoutMillis)
        } catch (error: Exception) {
            Log.e(TAG, "error writing characteristic: ${error.message}", error)
        }
    }

    fun disconnected() {
        characteristicList.clear()
        stopReader()
        responseQueues.clear()
        Log.d(TAG, "ATT disconnected")
    }

    private fun runReaderLoop() {
        val input = transport.attInput
        val buffer = ByteArray(512)
        while (readerRunning.get()) {
            try {
                val len = input.read(buffer)
                if (len == -1) break
                if (len == 0) continue
                val data = buffer.copyOfRange(0, len)
                val opcode = data[0]
                responseQueues.computeIfAbsent(opcode) { LinkedBlockingQueue() }.offer(data)

                if (opcode == 0x1B.toByte() && data.size >= 3) {
                    val handle = data[1]
                    val value = if (data.size > 3) data.copyOfRange(3, data.size) else ByteArray(0)
                    onNotificationReceived?.invoke(handle, value)
                }
            } catch (error: Exception) {
                if (readerRunning.get()) Log.e(TAG, "error in reader loop", error)
                break
            }
        }
        readerRunning.set(false)
    }

    private fun waitForResponse(opcode: Byte, timeoutMillis: Long): ByteArray? = try {
        responseQueues.computeIfAbsent(opcode) { LinkedBlockingQueue() }
            .poll(timeoutMillis, TimeUnit.MILLISECONDS)
    } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
        null
    }
}
