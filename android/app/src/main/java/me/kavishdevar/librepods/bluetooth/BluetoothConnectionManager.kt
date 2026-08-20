/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    Transitional compatibility facade for the legacy AACP implementation.
    New Wear code owns sockets through AirPodsConnectionSession.
*/

package me.kavishdevar.librepods.bluetooth

import android.bluetooth.BluetoothSocket
import me.kavishdevar.librepods.wear.bluetooth.AirPodsConnectionSession

/**
 * @deprecated Use AirPodsConnectionSession directly. This facade exists only
 * while the inherited AACP implementation is being migrated.
 */
@Deprecated("Use AirPodsConnectionSession; retained only for AACP migration")
object BluetoothConnectionManager {
    private var session: AirPodsConnectionSession? = null

    val aacpSocket: BluetoothSocket?
        get() = session?.aacpSocket

    val attSocket: BluetoothSocket?
        get() = session?.attSocket

    fun bind(connectionSession: AirPodsConnectionSession) {
        session = connectionSession
    }

    fun unbind(connectionSession: AirPodsConnectionSession) {
        if (session === connectionSession) session = null
    }
}
