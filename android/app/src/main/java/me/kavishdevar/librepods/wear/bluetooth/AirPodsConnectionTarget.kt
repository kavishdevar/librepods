package me.kavishdevar.librepods.wear.bluetooth

import android.bluetooth.BluetoothDevice
import android.os.ParcelUuid

/**
 * Complete transport target produced by discovery/protocol negotiation.
 *
 * UUIDs and PSMs are intentionally data, not constants here: AirPods model
 * and Android transport compatibility must be resolved by the discovery
 * layer before a session is started.
 */
data class AirPodsConnectionTarget(
    val device: BluetoothDevice,
    val aacpUuid: ParcelUuid,
    val aacpPsm: Int,
    val attUuid: ParcelUuid,
    val attPsm: Int,
)
