package me.kavishdevar.librepods.wear.core

/**
 * User-selectable Bluetooth device identity exposed by the Wear UI.
 *
 * The platform Bluetooth address is kept as the stable selection key for the
 * current Android/Wear OS runtime. Protocol-specific model detection remains
 * outside this value object.
 */
data class AirPodsDevice(
    val name: String,
    val address: String,
    val rssi: Int? = null,
    val bonded: Boolean = false,
)
