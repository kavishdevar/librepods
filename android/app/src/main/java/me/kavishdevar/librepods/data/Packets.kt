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

package me.kavishdevar.librepods.data

import android.util.Log

enum class Enums(val value: ByteArray) {
    NOISE_CANCELLATION(byteArrayOf(0x0d)),
    PREFIX(byteArrayOf(0x04, 0x00, 0x04, 0x00)),
    SETTINGS(byteArrayOf(0x09, 0x00)),
    NOISE_CANCELLATION_PREFIX(PREFIX.value + SETTINGS.value + NOISE_CANCELLATION.value),
    CONVERSATION_AWARENESS_RECEIVE_PREFIX(PREFIX.value + byteArrayOf(0x4b, 0x00, 0x02, 0x00)),
}

object BatteryComponent {
    const val LEFT = 4
    const val RIGHT = 2
    const val CASE = 8
}

object BatteryStatus {
    const val CHARGING = 1
    const val NOT_CHARGING = 2
    const val DISCONNECTED = 4
    const val OPTIMIZED_CHARGING = 5
}

/** Plain Wear OS data model; it does not need Android Parcelable overhead. */
data class Battery(val component: Int, val level: Int, val status: Int) {
    fun getComponentName(): String? = when (component) {
        BatteryComponent.LEFT -> "LEFT"
        BatteryComponent.RIGHT -> "RIGHT"
        BatteryComponent.CASE -> "CASE"
        else -> null
    }

    fun getStatusName(): String? = when (status) {
        BatteryStatus.CHARGING -> "CHARGING"
        BatteryStatus.NOT_CHARGING -> "NOT_CHARGING"
        BatteryStatus.DISCONNECTED -> "DISCONNECTED"
        BatteryStatus.OPTIMIZED_CHARGING -> "OPTIMIZED_CHARGING"
        else -> null
    }
}

enum class NoiseControlMode {
    OFF, NOISE_CANCELLATION, TRANSPARENCY, ADAPTIVE
}

class AirPodsNotifications {
    companion object {
        const val AIRPODS_CONNECTED = "me.kavishdevar.librepods.AIRPODS_CONNECTED"
        const val AIRPODS_L2CAP_CONNECTED = "me.kavishdevar.librepods.AIRPODS_CONNECTED"
        const val AIRPODS_DATA = "me.kavishdevar.librepods.AIRPODS_DATA"
        const val EAR_DETECTION_DATA = "me.kavishdevar.librepods.EAR_DETECTION_DATA"
        const val ANC_DATA = "me.kavishdevar.librepods.ANC_DATA"
        const val BATTERY_DATA = "me.kavishdevar.librepods.BATTERY_DATA"
        const val CA_DATA = "me.kavishdevar.librepods.CA_DATA"
        const val AIRPODS_DISCONNECTED = "me.kavishdevar.librepods.AIRPODS_DISCONNECTED"
        const val AIRPODS_CONNECTION_DETECTED = "me.kavishdevar.librepods.AIRPODS_CONNECTION_DETECTED"
        const val DISCONNECT_RECEIVERS = "me.kavishdevar.librepods.DISCONNECT_RECEIVERS"
        const val EQ_DATA = "me.kavishdevar.librepods.HEADPHONE_ACCOMMODATION"
        const val AIRPODS_INFORMATION_UPDATED = "me.kavishdevar.librepods.AIRPODS_INFORMATION_UPDATED"
    }

    class EarDetection {
        private val notificationBit = 6.toByte()
        private val notificationPrefix = Enums.PREFIX.value + notificationBit
        var status: List<Byte> = listOf(0x01, 0x01)

        fun setStatus(data: ByteArray) {
            status = listOf(data[6], data[7])
        }

        fun isEarDetectionData(data: ByteArray): Boolean {
            if (data.size != 8) return false
            return data.startsWith(notificationPrefix)
        }
    }

    class ANC {
        private val notificationPrefix = Enums.NOISE_CANCELLATION_PREFIX.value
        var status: Int = 1
            private set

        fun isANCData(data: ByteArray): Boolean {
            if (data.size != 11) return false
            return data.startsWith(notificationPrefix)
        }

        fun setStatus(data: ByteArray) {
            when (data.size) {
                11 -> status = data[7].toInt()
                1, 4 -> status = data[0].toInt()
                else -> Log.d("ANC", "Invalid ANC data size: ${data.size}")
            }
        }

        val name: String
            get() = when (status) {
                1 -> "OFF"
                2 -> "ON"
                3 -> "TRANSPARENCY"
                4 -> "ADAPTIVE"
                else -> "UNKNOWN"
            }
    }

    class BatteryNotification {
        private var first = Battery(BatteryComponent.LEFT, 0, BatteryStatus.DISCONNECTED)
        private var second = Battery(BatteryComponent.RIGHT, 0, BatteryStatus.DISCONNECTED)
        private var case = Battery(BatteryComponent.CASE, 0, BatteryStatus.DISCONNECTED)

        fun isBatteryData(data: ByteArray): Boolean {
            return data.size == 22 && data.startsWith(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x04, 0x00))
        }

        fun setBatteryDirect(
            leftLevel: Int,
            leftCharging: Boolean,
            rightLevel: Int,
            rightCharging: Boolean,
            caseLevel: Int,
            caseCharging: Boolean,
        ) {
            first = Battery(BatteryComponent.LEFT, leftLevel, if (leftCharging) BatteryStatus.CHARGING else BatteryStatus.NOT_CHARGING)
            second = Battery(BatteryComponent.RIGHT, rightLevel, if (rightCharging) BatteryStatus.CHARGING else BatteryStatus.NOT_CHARGING)
            case = Battery(BatteryComponent.CASE, caseLevel, if (caseCharging) BatteryStatus.CHARGING else BatteryStatus.NOT_CHARGING)
        }

        fun setBattery(data: ByteArray) {
            if (data.size != 22) return
            first = Battery(data[7].toInt(), data[9].toInt(), data[10].toInt())
            second = Battery(data[12].toInt(), data[14].toInt(), data[15].toInt())
            case = Battery(data[17].toInt(), data[19].toInt(), data[20].toInt())
        }

        fun getBattery(): List<Battery> {
            val left = if (first.component == BatteryComponent.LEFT) first else second
            val right = if (first.component == BatteryComponent.LEFT) second else first
            return listOf(left, right, case)
        }
    }

    class ConversationalAwarenessNotification {
        private val notificationPrefix = Enums.CONVERSATION_AWARENESS_RECEIVE_PREFIX.value
        var status: Byte = 0
            private set

        fun isConversationalAwarenessData(data: ByteArray): Boolean {
            return data.size == 10 && data.startsWith(notificationPrefix)
        }

        fun setData(data: ByteArray) {
            status = data[9]
        }
    }
}

private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
    if (size < prefix.size) return false
    return prefix.indices.all { this[it] == prefix[it] }
}

fun isHeadTrackingData(data: ByteArray): Boolean {
    if (data.size <= 60) return false

    val prefixPattern = byteArrayOf(
        0x04, 0x00, 0x04, 0x00, 0x17, 0x00, 0x00, 0x00,
        0x10, 0x00,
    )

    for (i in prefixPattern.indices) {
        if (data[i] != prefixPattern[i]) return false
    }

    if (data[10] != 0x44.toByte() && data[10] != 0x45.toByte()) return false
    return data[11] == 0x00.toByte()
}
