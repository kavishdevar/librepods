/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package me.kavishdevar.librepods.bluetooth.att

import android.bluetooth.BluetoothSocket
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.kavishdevar.librepods.devices.Device

enum class ATTHandle(val value: Int) {
    TRANSPARENCY(0x18),
    LOUD_SOUND_REDUCTION(0x1B),
    HEARING_AID(0x2A)
}

class ATTManager (
    private val device: Device<*, *, *>
) {
    val macParts = device.macAddress.value.split(":")
    private val TAG = "ATTManager[${macParts[0]}:${macParts[1]}:${macParts[2]}]"
    private var socket: BluetoothSocket? = null
    suspend fun readCharacteristic(handle: ATTHandle): ByteArray? {
        // TODO
        return null
        if (socket != null) {
            Log.d(TAG, "Closing existing socket before reading characteristic")
            try {
                socket!!.close()
                Log.d(TAG, "Existing socket closed successfully")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            socket = null
        }

        Log.d(TAG, "Creating new socket for reading characteristic")
        socket = try {
            device.createSocket(ParcelUuid.fromString("00000000-0000-0000-0000-000000000000"), 31)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating socket: ${e.message}")
            return null
        }

        try {
            socket?.connect()
            Log.d(TAG, "Socket connected successfully")
            val output = socket?.outputStream ?: return null
            Log.d(TAG, "Sending read request for handle: ${handle.value}")
            val pdu = byteArrayOf(0x0A, handle.value.toByte(), 0x00)

            withContext(Dispatchers.IO) {
                output.write(pdu)
                Log.d(TAG, "Read request sent: ${pdu.joinToString(" ") { String.format("%02X", it) }}")
                output.flush()
                Log.d(TAG, "Output stream flushed after sending read request")
            }

            val buffer = ByteArray(1024)
            val bytesRead = withContext(Dispatchers.IO) {
                socket?.inputStream?.read(buffer)?: -1
            }
            Log.d(TAG, "Read response received, bytes read: ${buffer.copyOfRange(0, bytesRead).toHexString()}")
            return if (bytesRead > 0) {
                buffer.copyOfRange(1, bytesRead)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Error during read operation: ${e.message}")
        } finally {
            try {
                Log.d(TAG, "Closing socket after read operation")
                socket?.close()
                socket = null
                Log.d(TAG, "Socket closed successfully after read operation")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    suspend fun writeCharacteristic(handle: ATTHandle, data: ByteArray): Boolean {
        return false
        // todo
        if (socket != null) {
            Log.d(TAG, "Closing existing socket before writing characteristic")
            try {
                socket!!.close()
                Log.d(TAG, "Existing socket closed successfully")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            socket = null
        }

        Log.d(TAG, "Creating new socket for writing characteristic")
        socket = try {
            device.createSocket(ParcelUuid.fromString("00000000-0000-0000-0000-000000000000"), 31)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        Log.d(TAG, "Socket created successfully, attempting to connect")

        try {
            socket?.connect()
            Log.d(TAG, "Socket connected successfully")
            val output = socket?.outputStream ?: return false
            Log.d(TAG, "Sending write request for handle: ${handle.value}")
            val pdu = byteArrayOf(0x12, handle.value.toByte(), 0x00) + data

            withContext(Dispatchers.IO) {
                output.write(pdu)
                Log.d(TAG, "Write request sent: ${pdu.joinToString(" ") { String.format("%02X", it) }}")
                output.flush()
                Log.d(TAG, "Output stream flushed after sending write request")
            }

            val buffer = ByteArray(1024)
            val bytesRead = withContext(Dispatchers.IO) {
                socket?.inputStream?.read(buffer)?: -1
            }
            Log.d(TAG, "Write response received, bytes read: ${buffer.copyOfRange(0, bytesRead).toHexString()}")
            return bytesRead > 0
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                Log.d(TAG, "Closing socket after write operation")
                socket?.close()
                socket = null
                Log.d(TAG, "Socket closed successfully after write operation")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return false
    }
}
