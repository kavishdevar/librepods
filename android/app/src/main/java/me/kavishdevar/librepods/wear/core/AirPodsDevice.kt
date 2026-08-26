package me.kavishdevar.librepods.wear.core

/**
 * User-selectable Bluetooth device identity exposed by the Wear UI.
 *
 * Discovery metadata is kept here so the UI can diagnose AirPods advertisements
 * without coupling the UI to Android Bluetooth APIs.
 */
data class AirPodsDevice(
    val name: String,
    val address: String,
    val rssi: Int? = null,
    val bonded: Boolean = false,
    val appleManufacturer: Boolean = false,
    val serviceUuids: List<String> = emptyList(),
    val seenAtMillis: Long = System.currentTimeMillis(),
)
