/*
 * LibrePods - AirPods liberated from Apple’s ecosystem
 *
 * Copyright (C) 2025 LibrePods contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */


@file:Suppress("unused")

package me.kavishdevar.librepods.utils

import android.os.Parcelable
import android.util.Log
import kotlinx.parcelize.Parcelize

enum class Enums(val value: ByteArray) {
    NOISE_CANCELLATION(Capabilities.NOISE_CANCELLATION),
    CONVERSATION_AWARENESS(Capabilities.CONVERSATION_AWARENESS),
    CUSTOMIZABLE_ADAPTIVE_TRANSPARENCY(Capabilities.CUSTOMIZABLE_ADAPTIVE_TRANSPARENCY),
    PREFIX(byteArrayOf(0x04, 0x00, 0x04, 0x00)),
    SETTINGS(byteArrayOf(0x09, 0x00)),
    SUFFIX(byteArrayOf(0x00, 0x00, 0x00)),
    NOTIFICATION_FILTER(byteArrayOf(0x0f)),
    HANDSHAKE(byteArrayOf(0x00, 0x00, 0x04, 0x00, 0x01, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)),
    SPECIFIC_FEATURES(byteArrayOf(0x4d)),
    SET_SPECIFIC_FEATURES(PREFIX.value + SPECIFIC_FEATURES.value + byteArrayOf(0x00,
        0xff.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)),
    REQUEST_NOTIFICATIONS(PREFIX.value + NOTIFICATION_FILTER.value + byteArrayOf(0x00, 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte())),
    NOISE_CANCELLATION_PREFIX(PREFIX.value + SETTINGS.value + NOISE_CANCELLATION.value),
    NOISE_CANCELLATION_OFF(NOISE_CANCELLATION_PREFIX.value + Capabilities.NoiseCancellation.OFF.value + SUFFIX.value),
    NOISE_CANCELLATION_ON(NOISE_CANCELLATION_PREFIX.value + Capabilities.NoiseCancellation.ON.value + SUFFIX.value),
    NOISE_CANCELLATION_TRANSPARENCY(NOISE_CANCELLATION_PREFIX.value + Capabilities.NoiseCancellation.TRANSPARENCY.value + SUFFIX.value),
    NOISE_CANCELLATION_ADAPTIVE(NOISE_CANCELLATION_PREFIX.value + Capabilities.NoiseCancellation.ADAPTIVE.value + SUFFIX.value),
    SET_CONVERSATION_AWARENESS_OFF(PREFIX.value + SETTINGS.value + CONVERSATION_AWARENESS.value + Capabilities.ConversationAwareness.OFF.value + SUFFIX.value),
    SET_CONVERSATION_AWARENESS_ON(PREFIX.value + SETTINGS.value + CONVERSATION_AWARENESS.value + Capabilities.ConversationAwareness.ON.value + SUFFIX.value),
    CONVERSATION_AWARENESS_RECEIVE_PREFIX(PREFIX.value + byteArrayOf(0x4b, 0x00, 0x02, 0x00)),
    START_HEAD_TRACKING(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x17, 0x00, 0x00, 0x00, 0x10, 0x00, 0x10, 0x00, 0x08, 0xA1.toByte(), 0x02, 0x42, 0x0B, 0x08, 0x0E, 0x10, 0x02, 0x1A, 0x05, 0x01, 0x40, 0x9C.toByte(), 0x00, 0x00)),
    STOP_HEAD_TRACKING(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x17, 0x00, 0x00, 0x00, 0x10, 0x00, 0x11, 0x00, 0x08, 0x7E.toByte(), 0x10, 0x02, 0x42, 0x0B, 0x08, 0x4E.toByte(), 0x10, 0x02, 0x1A, 0x05, 0x01, 0x00, 0x00, 0x00, 0x00));
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
}

@Parcelize
data class Battery(val component: Int, val level: Int, val status: Int) : Parcelable {
    fun getComponentName(): String? {
        return when (component) {
            BatteryComponent.LEFT -> "LEFT"
            BatteryComponent.RIGHT -> "RIGHT"
            BatteryComponent.CASE -> "CASE"
            else -> null
        }
    }

    fun getStatusName(): String? {
        return when (status) {
            BatteryStatus.CHARGING -> "CHARGING"
            BatteryStatus.NOT_CHARGING -> "NOT_CHARGING"
            BatteryStatus.DISCONNECTED -> "DISCONNECTED"
            else -> null
        }
    }
}

enum class NoiseControlMode {
    OFF,  NOISE_CANCELLATION, TRANSPARENCY, ADAPTIVE
}

class AirPodsNotifications {
    companion object {
        const val AIRPODS_CONNECTED = "me.kavishdevar.librepods.AIRPODS_CONNECTED"
        const val AIRPODS_DATA = "me.kavishdevar.librepods.AIRPODS_DATA"
        const val EAR_DETECTION_DATA = "me.kavishdevar.librepods.EAR_DETECTION_DATA"
        const val ANC_DATA = "me.kavishdevar.librepods.ANC_DATA"
        const val BATTERY_DATA = "me.kavishdevar.librepods.BATTERY_DATA"
        const val CA_DATA = "me.kavishdevar.librepods.CA_DATA"
        const val AIRPODS_DISCONNECTED = "me.kavishdevar.librepods.AIRPODS_DISCONNECTED"
        const val AIRPODS_CONNECTION_DETECTED = "me.kavishdevar.librepods.AIRPODS_CONNECTION_DETECTED"
        const val DISCONNECT_RECEIVERS = "me.kavishdevar.librepods.DISCONNECT_RECEIVERS"
    }

    class EarDetection {
        private val notificationBit = Capabilities.EAR_DETECTION
        private val notificationPrefix = Enums.PREFIX.value + notificationBit

        var status: List<Byte> = listOf(0x01, 0x01)

        fun setStatus(data: ByteArray) {
            status = listOf(data[6], data[7])
        }

        fun isEarDetectionData(data: ByteArray): Boolean {
            if (data.size != 8) {
                return false
            }
            val prefixHex = notificationPrefix.joinToString("") { "%02x".format(it) }
            val dataHex = data.joinToString("") { "%02x".format(it) }
            return dataHex.startsWith(prefixHex)
        }
    }

    class ANC {
        private val notificationPrefix = Enums.NOISE_CANCELLATION_PREFIX.value

        var status: Int = 1
            private set

        fun isANCData(data: ByteArray): Boolean {
            if (data.size != 11) {
                return false
            }
            val prefixHex = notificationPrefix.joinToString("") { "%02x".format(it) }
            val dataHex = data.joinToString("") { "%02x".format(it) }
            return dataHex.startsWith(prefixHex)
        }

        fun setStatus(data: ByteArray) {
            when (data.size) {
                // if the whole packet is given
                11 -> {
                    status = data[7].toInt()
                }
                // if only the data is given
                1 -> {
                    status = data[0].toInt()
                }
                // if the value of control command is given
                4 -> {
                    status = data[0].toInt()
                }
                else -> {
                    Log.d("ANC", "Invalid ANC data size: ${data.size}")
                }
            }
        }

        val name: String =
             when (status) {
                1 -> "OFF"
                2 -> "ON"
                3 -> "TRANSPARENCY"
                4 -> "ADAPTIVE"
                else -> "UNKNOWN"
            }

    }

    class BatteryNotification {
        private var first: Battery = Battery(BatteryComponent.LEFT, 0, BatteryStatus.DISCONNECTED)
        private var second: Battery = Battery(BatteryComponent.RIGHT, 0, BatteryStatus.DISCONNECTED)
        private var case: Battery = Battery(BatteryComponent.CASE, 0, BatteryStatus.DISCONNECTED)

        fun isBatteryData(data: ByteArray): Boolean {
            if (data.joinToString("") { "%02x".format(it) }.startsWith("040004000400")) {
                Log.d("BatteryNotification", "Battery data starts with 040004000400. Most likely is a battery packet.")
            } else {
                return false
            }
            if (data.size != 22) {
                Log.d("BatteryNotification", "Battery data size is not 22, probably being used with Airpods with fewer or more battery count.")
                return false
            }
            Log.d("BatteryNotification", data.joinToString("") { "%02x".format(it) }.startsWith("040004000400").toString())
            return data.joinToString("") { "%02x".format(it) }.startsWith("040004000400")
        }

        fun setBatteryDirect(
            leftLevel: Int,
            leftCharging: Boolean,
            rightLevel: Int,
            rightCharging: Boolean,
            caseLevel: Int,
            caseCharging: Boolean
        ) {
            first = Battery(BatteryComponent.LEFT, leftLevel, if (leftCharging) BatteryStatus.CHARGING else BatteryStatus.NOT_CHARGING)
            second = Battery(BatteryComponent.RIGHT, rightLevel, if (rightCharging) BatteryStatus.CHARGING else BatteryStatus.NOT_CHARGING)
            case = Battery(BatteryComponent.CASE, caseLevel, if (caseCharging) BatteryStatus.CHARGING else BatteryStatus.NOT_CHARGING)
        }

        fun setBattery(data: ByteArray) {
            if (data.size != 22) {
                return
            }
            first = if (data[10].toInt() == BatteryStatus.DISCONNECTED) {
                Battery(first.component, first.level, data[10].toInt())
            } else {
                Battery(data[7].toInt(), data[9].toInt(), data[10].toInt())
            }
            second = if (data[15].toInt() == BatteryStatus.DISCONNECTED) {
                Battery(second.component, second.level, data[15].toInt())
            } else {
                Battery(data[12].toInt(), data[14].toInt(), data[15].toInt())
            }
            case = if (data[20].toInt() == BatteryStatus.DISCONNECTED && case.status != BatteryStatus.DISCONNECTED) {
                Battery(case.component, case.level, data[20].toInt())
            } else {
                Battery(data[17].toInt(), data[19].toInt(), data[20].toInt())
            }
        }

        fun getBattery(): List<Battery> {
            val left = if (first.component == BatteryComponent.LEFT) first else second
            val right = if (first.component == BatteryComponent.LEFT) second else first
            return listOf(left, right, case)
        }
    }

    class ConversationalAwarenessNotification {
        @Suppress("PrivatePropertyName")
        private val NOTIFICATION_PREFIX = Enums.CONVERSATION_AWARENESS_RECEIVE_PREFIX.value

        var status: Byte = 0
            private set

        fun isConversationalAwarenessData(data: ByteArray): Boolean {
            if (data.size != 10) {
                return false
            }
            val prefixHex = NOTIFICATION_PREFIX.joinToString("") { "%02x".format(it) }
            val dataHex = data.joinToString("") { "%02x".format(it) }
            return dataHex.startsWith(prefixHex)
        }

        fun setData(data: ByteArray) {
            status = data[9]
        }
    }
}

class Capabilities {
    companion object {
        val NOISE_CANCELLATION = byteArrayOf(0x0d)
        val CONVERSATION_AWARENESS = byteArrayOf(0x28)
        val CUSTOMIZABLE_ADAPTIVE_TRANSPARENCY = byteArrayOf(0x01, 0x02)
        val EAR_DETECTION = byteArrayOf(0x06)
    }

    enum class NoiseCancellation(val value: ByteArray) {
        OFF(byteArrayOf(0x01)),
        ON(byteArrayOf(0x02)),
        TRANSPARENCY(byteArrayOf(0x03)),
        ADAPTIVE(byteArrayOf(0x04));
    }

    enum class ConversationAwareness(val value: ByteArray) {
        OFF(byteArrayOf(0x02)),
        ON(byteArrayOf(0x01));
    }
}

enum class LongPressPackets(val value: ByteArray) {
    ENABLE_EVERYTHING(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x0F, 0x00, 0x00, 0x00)),

    DISABLE_OFF_FROM_EVERYTHING(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x0e, 0x00, 0x00, 0x00)),
    DISABLE_OFF_FROM_TRANSPARENCY_AND_ADAPTIVE(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x0c, 0x00, 0x00, 0x00)),
    DISABLE_OFF_FROM_TRANSPARENCY_AND_ANC(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x06, 0x00, 0x00, 0x00)),
    DISABLE_OFF_FROM_ADAPTIVE_AND_ANC(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x0a, 0x00, 0x00, 0x00)),

    ENABLE_OFF_FROM_TRANSPARENCY_AND_ANC(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x07, 0x00, 0x00, 0x00)),
    ENABLE_OFF_FROM_ADAPTIVE_AND_ANC(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x0b, 0x00, 0x00, 0x00)),
    ENABLE_OFF_FROM_TRANSPARENCY_AND_ADAPTIVE(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x0d, 0x00, 0x00, 0x00)),

    DISABLE_TRANSPARENCY_FROM_EVERYTHING(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x0b, 0x00, 0x00, 0x00)),
    DISABLE_TRANSPARENCY_FROM_OFF_AND_ANC(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x03, 0x00, 0x00, 0x00)),
    DISABLE_TRANSPARENCY_FROM_ADAPTIVE_AND_ANC(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x0a, 0x00, 0x00, 0x00)),
    DISABLE_TRANSPARENCY_FROM_OFF_AND_ADAPTIVE(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x09, 0x00, 0x00, 0x00)),

    ENABLE_TRANSPARENCY_FROM_OFF_AND_ANC(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x07, 0x00, 0x00, 0x00)),
    ENABLE_TRANSPARENCY_FROM_ADAPTIVE_AND_ANC(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x0e, 0x00, 0x00, 0x00)),
    ENABLE_TRANSPARENCY_FROM_OFF_AND_ADAPTIVE(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x0d, 0x00, 0x00, 0x00)),

    DISABLE_ANC_FROM_EVERYTHING(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x0D, 0x00, 0x00, 0x00)),
    DISABLE_ANC_FROM_OFF_AND_TRANSPARENCY(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x05, 0x00, 0x00, 0x00)),
    DISABLE_ANC_FROM_ADAPTIVE_AND_TRANSPARENCY(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x0c, 0x00, 0x00, 0x00)),
    DISABLE_ANC_FROM_OFF_AND_ADAPTIVE(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x09, 0x00, 0x00, 0x00)),

    ENABLE_ANC_FROM_OFF_AND_TRANSPARENCY(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x07, 0x00, 0x00, 0x00)),
    ENABLE_ANC_FROM_ADAPTIVE_AND_TRANSPARENCY(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x0e, 0x00, 0x00, 0x00)),
    ENABLE_ANC_FROM_OFF_AND_ADAPTIVE(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x0b, 0x00, 0x00, 0x00)),

    DISABLE_ADAPTIVE_FROM_EVERYTHING(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x07, 0x00, 0x00, 0x00)),
    DISABLE_ADAPTIVE_FROM_OFF_AND_TRANSPARENCY(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x05, 0x00, 0x00, 0x00)),
    DISABLE_ADAPTIVE_FROM_TRANSPARENCY_AND_ANC(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x06, 0x00, 0x00, 0x00)),
    DISABLE_ADAPTIVE_FROM_OFF_AND_ANC(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x03, 0x00, 0x00, 0x00)),

    ENABLE_ADAPTIVE_FROM_OFF_AND_TRANSPARENCY(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x0d, 0x00, 0x00, 0x00)),
    ENABLE_ADAPTIVE_FROM_TRANSPARENCY_AND_ANC(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x0e, 0x00, 0x00, 0x00)),
    ENABLE_ADAPTIVE_FROM_OFF_AND_ANC(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x0b, 0x00, 0x00, 0x00)),

    ENABLE_EVERYTHING_OFF_DISABLED(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x0E, 0x00, 0x00, 0x00)),
    DISABLE_TRANSPARENCY_OFF_DISABLED(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x0A, 0x00, 0x00, 0x00)),
    DISABLE_ADAPTIVE_OFF_DISABLED(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x06, 0x00, 0x00, 0x00)),
    DISABLE_ANC_OFF_DISABLED(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A, 0x0C, 0x00, 0x00, 0x00)),
}

//enum class LongPressMode {
//    OFF, TRANSPARENCY, ADAPTIVE, ANC
//}
//
//data class LongPressPacket(val modes: Set<LongPressMode>) {
//    val value: ByteArray
//        get() {
//            val baseArray = byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A)
//            val modeByte = calculateModeByte()
//            return baseArray + byteArrayOf(modeByte, 0x00, 0x00, 0x00)
//        }
//
//    private fun calculateModeByte(): Byte {
//        var modeByte: Byte = 0x00
//        modes.forEach { mode ->
//            modeByte = when (mode) {
//                LongPressMode.OFF -> (modeByte + 0x01).toByte()
//                LongPressMode.TRANSPARENCY -> (modeByte + 0x02).toByte()
//                LongPressMode.ADAPTIVE -> (modeByte + 0x04).toByte()
//                LongPressMode.ANC -> (modeByte + 0x08).toByte()
//            }
//        }
//        return modeByte
//    }
//}
//
//fun determinePacket(changedIndex: Int, newEnabled: Boolean, oldModes: Set<LongPressMode>, newModes: Set<LongPressMode>): ByteArray? {
//    return if (newEnabled) {
//        LongPressPacket(oldModes + newModes.elementAt(changedIndex)).value
//    } else {
//        LongPressPacket(oldModes - newModes.elementAt(changedIndex)).value
//    }
//}

fun isHeadTrackingData(data: ByteArray): Boolean {
    if (data.size <= 60) return false

    val prefixPattern = byteArrayOf(
        0x04, 0x00, 0x04, 0x00, 0x17, 0x00, 0x00, 0x00,
        0x10, 0x00
    )

    for (i in prefixPattern.indices) {
        if (data[i] != prefixPattern[i].toByte()) return false
    }

    if (data[10] != 0x44.toByte() && data[10] != 0x45.toByte()) return false

    if (data[11] != 0x00.toByte()) return false

    return true
}
