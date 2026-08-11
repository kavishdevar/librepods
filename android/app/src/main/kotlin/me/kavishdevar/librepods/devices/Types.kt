package me.kavishdevar.librepods.devices

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import me.kavishdevar.librepods.utils.redactMac

// TODO: differentiate between bluetooth connected and oem-specific stuff connected. availiable is reserved for BLE based things, hence not used bluetooth connected
enum class ConnectionState {
    DISCONNECTED,
    DISCONNECTING,
    AVAILABLE,
    CONNECTING,
    CONNECTED,
}

enum class BatteryComponent {
    LEFT,
    RIGHT,
    CASE,
    HEADSET;

    companion object {
        fun fromAirPodsByte(value: Byte): BatteryComponent {
            return when (value.toInt()) {
                1 -> HEADSET
                4 -> LEFT
                2 -> RIGHT
                8 -> CASE
                else -> throw IllegalArgumentException("Unknown battery component value: $value")
            }
        }
    }
}

enum class BatteryStatus {
    UNKNOWN, // got this from pro 3 when one of the airpods had crashed and the other was still connected
    CHARGING,
    NOT_CHARGING,
    DISCONNECTED,
    OPTIMIZED_CHARGING;

    companion object {
        fun fromAirPodsByte(value: Byte): BatteryStatus {
            return when (value.toInt()) {
                0 -> UNKNOWN
                1 -> CHARGING
                2 -> NOT_CHARGING
                4 -> DISCONNECTED
                5 -> OPTIMIZED_CHARGING
                else -> throw IllegalArgumentException("Unknown battery status value: $value")
            }
        }
    }
}

@Parcelize
data class Battery(
    val component: BatteryComponent,
    val level: Int,
    val status: BatteryStatus
) : Parcelable

// TODO: use this for battery too. BatteryComponent is very redundant
enum class DeviceComponent {
    LEFT,
    RIGHT,
    CASE,
    HEADSET
}

enum class ComponentStatus {
    IN_EAR,
    OUT_OF_EAR,
    IN_CASE,
    DISCONNECTED;

    companion object {
        fun fromAirPodsByte(value: Byte): ComponentStatus {
            return when (value.toInt()) {
                0 -> IN_EAR
                1 -> OUT_OF_EAR
                2 -> IN_CASE
                3 -> DISCONNECTED
                else -> throw IllegalArgumentException("Unknown device status value: $value")
            }
        }
    }

    fun toAirPodsByte(): Byte {
        return when (this) {
            IN_EAR -> 0
            OUT_OF_EAR -> 1
            IN_CASE -> 2
            DISCONNECTED -> 3
        }.toByte()
    }
}

data class DeviceComponentState(
    val component: DeviceComponent,
    val status: ComponentStatus
)

enum class PacketDestination {
    DEVICE,
    HOST;
}

enum class NoiseControlMode {
    OFF,  NOISE_CANCELLATION, TRANSPARENCY, ADAPTIVE
}
