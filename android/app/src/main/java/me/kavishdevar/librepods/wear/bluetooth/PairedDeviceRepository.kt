package me.kavishdevar.librepods.wear.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context

/**
 * System Bluetooth paired-device source.
 * Pairing and discovery are delegated to Wear OS.
 * LibrePods only handles connected device communication.
 */
class PairedDeviceRepository(private val context: Context) {

    @SuppressLint("MissingPermission")
    fun getBondedDevices(): List<BluetoothDevice> {
        val adapter = context
            .getSystemService(BluetoothManager::class.java)
            ?.adapter
            ?: return emptyList()

        return adapter.bondedDevices
            .orEmpty()
            .sortedByDescending { isLikelyAirPods(it) }
    }

    @SuppressLint("MissingPermission")
    fun getBondedAirPods(): List<BluetoothDevice> {
        return getBondedDevices().filter { isLikelyAirPods(it) }
    }

    @SuppressLint("MissingPermission")
    private fun isLikelyAirPods(device: BluetoothDevice): Boolean {
        val name = device.name.orEmpty()
        return name.contains("AirPods", true) ||
            name.contains("Pods", true) ||
            name.contains("Beats", true)
    }
}
