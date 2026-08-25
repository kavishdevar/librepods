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

import android.os.Parcelable
import android.util.Log
import kotlinx.parcelize.Parcelize
import me.kavishdevar.librepods.utils.BatteryLevels

// TODO: Remove everything but Battery-related stuff

private fun ByteArray.startsWithBytes(prefix: ByteArray): Boolean {
    if (size < prefix.size) return false
    for (index in prefix.indices) {
        if (this[index] != prefix[index]) return false
    }
    return true
}

private val HEAD_TRACKING_PREFIX = byteArrayOf(
    0x04, 0x00, 0x04, 0x00, 0x17, 0x00, 0x00, 0x00,
    0x10, 0x00
)

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
    const val UNKNOWN = 0
    const val CHARGING = 1
    const val NOT_CHARGING = 2
    const val DISCONNECTED = 4
    const val OPTIMIZED_CHARGING = 5
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
            BatteryStatus.UNKNOWN -> "UNKNOWN"
            BatteryStatus.CHARGING -> "CHARGING"
            BatteryStatus.NOT_CHARGING -> "NOT_CHARGING"
            BatteryStatus.DISCONNECTED -> "DISCONNECTED"
            BatteryStatus.OPTIMIZED_CHARGING -> "OPTIMIZED_CHARGING"
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
        const val AIRPODS_L2CAP_CONNECTED = "me.kavishdevar.librepods.AIRPODS_L2CAP_CONNECTED"
        /** AACP handshake complete. Same moment as [AIRPODS_CONNECTED]; UI should wait for this. */
        const val AIRPODS_L2CAP_READY = "me.kavishdevar.librepods.AIRPODS_L2CAP_READY"
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
            return data.size == 8 && data.startsWithBytes(notificationPrefix)
        }
    }

    class ANC {
        private val notificationPrefix = Enums.NOISE_CANCELLATION_PREFIX.value

        var status: Int = 1
            private set

        fun isANCData(data: ByteArray): Boolean {
            return data.size == 11 && data.startsWithBytes(notificationPrefix)
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
        private val notificationPrefix = byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x04, 0x00)
        private fun emptySnapshot(): List<Battery> = listOf(
            Battery(BatteryComponent.LEFT, BatteryLevels.UNKNOWN_LEVEL, BatteryStatus.DISCONNECTED),
            Battery(BatteryComponent.RIGHT, BatteryLevels.UNKNOWN_LEVEL, BatteryStatus.DISCONNECTED),
            Battery(BatteryComponent.CASE, BatteryLevels.UNKNOWN_LEVEL, BatteryStatus.DISCONNECTED)
        )

        @Volatile
        private var batterySnapshot: List<Battery> = emptySnapshot()

        fun isBatteryData(data: ByteArray): Boolean {
            if (data.size < BATTERY_HEADER_SIZE || !data.startsWithBytes(notificationPrefix)) {
                return false
            }
            val componentCount = data[BATTERY_COUNT_OFFSET].toInt() and 0xFF
            return componentCount <= MAX_COMPONENT_COUNT &&
                data.size >= BATTERY_HEADER_SIZE + componentCount * COMPONENT_RECORD_SIZE
        }

        fun setBatteryDirect(
            leftLevel: Int,
            leftCharging: Boolean,
            rightLevel: Int,
            rightCharging: Boolean,
            caseLevel: Int,
            caseCharging: Boolean
        ) {
            updateComponents(
                listOf(
                    parseComponent(BatteryComponent.LEFT, leftLevel, if (leftCharging) BatteryStatus.CHARGING else BatteryStatus.NOT_CHARGING),
                    parseComponent(BatteryComponent.RIGHT, rightLevel, if (rightCharging) BatteryStatus.CHARGING else BatteryStatus.NOT_CHARGING),
                    parseComponent(BatteryComponent.CASE, caseLevel, if (caseCharging) BatteryStatus.CHARGING else BatteryStatus.NOT_CHARGING)
                )
            )
        }

        /**
         * Applies an AACP battery notification. The protocol carries a component count, so
         * updates may contain one, two, or all three components. Preserve components omitted
         * from a partial update instead of dropping the whole packet and leaving stale values.
         */
        fun setBattery(data: ByteArray): Boolean {
            if (!isBatteryData(data)) {
                return false
            }

            val componentCount = data[BATTERY_COUNT_OFFSET].toInt() and 0xFF
            val updates = buildList(componentCount) {
                repeat(componentCount) { index ->
                    val offset = BATTERY_HEADER_SIZE + index * COMPONENT_RECORD_SIZE
                    val component = data[offset].toInt() and 0xFF
                    if (component == BatteryComponent.LEFT ||
                        component == BatteryComponent.RIGHT ||
                        component == BatteryComponent.CASE
                    ) {
                        add(
                            parseComponent(
                                component,
                                data[offset + LEVEL_OFFSET].toInt(),
                                data[offset + STATUS_OFFSET].toInt() and 0xFF
                            )
                        )
                    }
                }
            }
            if (updates.isEmpty() && componentCount > 0) return false
            updateComponents(updates)
            return true
        }

        /** Components genuinely present in this packet, excluding values preserved from before. */
        fun componentsInPacket(data: ByteArray): Set<Int> {
            if (!isBatteryData(data)) return emptySet()
            val componentCount = data[BATTERY_COUNT_OFFSET].toInt() and 0xFF
            return buildSet(componentCount) {
                repeat(componentCount) { index ->
                    val component = data[BATTERY_HEADER_SIZE + index * COMPONENT_RECORD_SIZE]
                        .toInt() and 0xFF
                    if (component == BatteryComponent.LEFT ||
                        component == BatteryComponent.RIGHT ||
                        component == BatteryComponent.CASE
                    ) {
                        add(component)
                    }
                }
            }
        }

        @Synchronized
        fun reset() {
            batterySnapshot = emptySnapshot()
        }

        private fun parseComponent(component: Int, rawLevel: Int, rawStatus: Int): Battery {
            val level = BatteryLevels.sanitizePercent(rawLevel)
            val status = if (rawStatus == BatteryStatus.DISCONNECTED) {
                BatteryStatus.DISCONNECTED
            } else {
                BatteryLevels.statusFor(level, rawStatus)
            }
            return Battery(component, level, status)
        }

        @Synchronized
        private fun updateComponents(updates: List<Battery>) {
            if (updates.isEmpty()) return
            val byComponent = batterySnapshot.associateByTo(mutableMapOf()) { it.component }
            updates.forEach { byComponent[it.component] = it }
            val updatedSnapshot = listOfNotNull(
                byComponent[BatteryComponent.LEFT],
                byComponent[BatteryComponent.RIGHT],
                byComponent[BatteryComponent.CASE]
            )
            if (updatedSnapshot != batterySnapshot) batterySnapshot = updatedSnapshot
        }

        fun getBattery(): List<Battery> = batterySnapshot

        private companion object {
            const val BATTERY_COUNT_OFFSET = 6
            const val BATTERY_HEADER_SIZE = 7
            const val COMPONENT_RECORD_SIZE = 5
            const val LEVEL_OFFSET = 2
            const val STATUS_OFFSET = 3
            const val MAX_COMPONENT_COUNT = 3
        }
    }

    class ConversationalAwarenessNotification {
        @Suppress("PrivatePropertyName")
        private val NOTIFICATION_PREFIX = Enums.CONVERSATION_AWARENESS_RECEIVE_PREFIX.value

        var status: Byte = 0
            private set

        fun isConversationalAwarenessData(data: ByteArray): Boolean {
            return data.size == 10 && data.startsWithBytes(NOTIFICATION_PREFIX)
        }

        fun setData(data: ByteArray) {
            status = data[9]
        }
    }
}

fun isHeadTrackingData(data: ByteArray): Boolean {
    if (data.size <= 60) return false
    if (!data.startsWithBytes(HEAD_TRACKING_PREFIX)) return false

    if (data[10] != 0x44.toByte() && data[10] != 0x45.toByte()) return false

    if (data[11] != 0x00.toByte()) return false

    return true
}
