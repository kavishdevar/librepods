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

package me.kavishdevar.librepods.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.ParcelUuid
import android.util.Log
import kotlinx.serialization.Serializable
import me.kavishdevar.librepods.utils.redactMac
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

fun createBluetoothSocket(
    adapter: BluetoothAdapter, device: BluetoothDevice, uuid: ParcelUuid, psm: Int
): BluetoothSocket {
    val type = 3 // L2CAP
    val constructorSpecs = listOf(
        arrayOf(adapter, device, type, true, true, psm, uuid), // A16QPR3
        arrayOf(device, type, true, true, psm, uuid),
        arrayOf(device, type, 1, true, true, psm, uuid),
        arrayOf(type, 1, true, true, device, psm, uuid),
        arrayOf(type, true, true, device, psm, uuid)
    )

    val constructors = BluetoothSocket::class.java.declaredConstructors
    Log.d("createSocket<$psm>", "BluetoothSocket has ${constructors.size} constructors:")

    constructors.forEachIndexed { index, constructor ->
        val params = constructor.parameterTypes.joinToString(", ") { it.simpleName }
        Log.d("createSocket<$psm>", "Constructor $index: ($params)")
    }

    var lastException: Exception? = null
    var attemptedConstructors = 0

    for ((index, params) in constructorSpecs.withIndex()) {
        try {
            Log.d("createSocket<$psm>", "Trying constructor signature #${index + 1}")
            attemptedConstructors++

            val paramTypes =
                params.map { it::class.javaPrimitiveType ?: it::class.java }.toTypedArray()
            val constructor = BluetoothSocket::class.java.getDeclaredConstructor(*paramTypes)
            constructor.isAccessible = true
            return constructor.newInstance(*params) as BluetoothSocket

        } catch (e: Exception) {
            Log.e("createSocket<$psm>", "Constructor signature #${index + 1} failed: ${e.message}")
            lastException = e
        }
    }

    val errorMessage =
        "Failed to create BluetoothSocket after trying $attemptedConstructors constructor signatures"
    Log.e("createSocket<$psm>", errorMessage)
    throw lastException ?: IllegalStateException(errorMessage)
}

/**
 * Verifies if the provided Bluetooth address is an RPA that matches the given Identity Resolving Key (IRK)
 *
 * @param addr The Bluetooth address to verify
 * @param irk The Identity Resolving Key to use for verification
 * @return true if the address is verified as an RPA matching the IRK
 */
fun verifyRPA(addr: String, irk: ByteArray): Boolean {
    val rpa = addr.split(":").map { it.toInt(16).toByte() }.reversed().toByteArray()
    val prand = rpa.copyOfRange(3, 6)
    val hash = rpa.copyOfRange(0, 3)
    val computedHash = ah(irk, prand)
    return hash.contentEquals(computedHash)
}

/**
 * Performs E function (AES-128) as specified in Bluetooth Core Specification
 *
 * @param key The key for encryption
 * @param data The data to encrypt
 * @return The encrypted data
 */
@SuppressLint("GetInstance")
fun e(key: ByteArray, data: ByteArray): ByteArray {
    val swappedKey = key.reversedArray()
    val swappedData = data.reversedArray()
    val cipher = Cipher.getInstance("AES/ECB/NoPadding")
    val secretKey = SecretKeySpec(swappedKey, "AES")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    return cipher.doFinal(swappedData).reversedArray()
}

/**
 * Performs the ah function as specified in Bluetooth Core Specification
 *
 * @param k The IRK key
 * @param r The random part of the address
 * @return The hash part of the address
 */
fun ah(k: ByteArray, r: ByteArray): ByteArray {
    val rPadded = ByteArray(16)
    r.copyInto(rPadded, 0, 0, 3)
    val encrypted = e(k, rPadded)
    return encrypted.copyOfRange(0, 3)
}

@JvmInline
@Serializable
value class MacAddress(val value: String) {
    init {
        require(value.matches(Regex("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}"))) {
            "Invalid MAC address format: $value"
        }
    }

    override fun toString(): String {
        return value
    }

    fun toRedactedString(): String {
        return value.redactMac()
    }

    fun toNotificationId(): Int {
        return value
            .replace(":", "")
            .takeLast(8)
            .toUInt(16)
            .toInt()

    }
}
