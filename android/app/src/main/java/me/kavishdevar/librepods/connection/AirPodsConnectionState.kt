/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.
*/

package me.kavishdevar.librepods.connection

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The user-visible connection lifecycle. Keeping this separate from Bluetooth callbacks avoids
 * every screen independently guessing whether ACL, A2DP, and AACP mean "connected".
 */
sealed interface AirPodsConnectionState {
    val label: String

    data object Disconnected : AirPodsConnectionState {
        override val label = "Not connected"
    }

    data object Detected : AirPodsConnectionState {
        override val label = "Connected for sound"
    }

    data object Connecting : AirPodsConnectionState {
        override val label = "Connecting controls…"
    }

    data object ControlConnected : AirPodsConnectionState {
        override val label = "Preparing controls…"
    }

    data object Ready : AirPodsConnectionState {
        override val label = "Connected"
    }

    data object Recovering : AirPodsConnectionState {
        override val label = "Sound connected · controls unavailable"
    }

    data object Disconnecting : AirPodsConnectionState {
        override val label = "Disconnecting…"
    }
}

data class AirPodsConnectionSnapshot(
    val state: AirPodsConnectionState = AirPodsConnectionState.Disconnected,
    val deviceName: String = "AirPods",
    val bluetoothAudioConnected: Boolean = false,
    val controlChannelConnected: Boolean = false,
    val changedAtElapsedRealtime: Long = SystemClock.elapsedRealtime(),
    val reason: String? = null,
) {
    val isReady: Boolean
        get() = state == AirPodsConnectionState.Ready && controlChannelConnected
}

/** Thread-safe reducer for connection events originating on Bluetooth and socket threads. */
class AirPodsConnectionStateMachine(initialDeviceName: String = "AirPods") {
    private val mutableSnapshot = MutableStateFlow(
        AirPodsConnectionSnapshot(deviceName = initialDeviceName)
    )
    val snapshot: StateFlow<AirPodsConnectionSnapshot> = mutableSnapshot.asStateFlow()

    @Synchronized
    fun rename(deviceName: String) {
        if (deviceName.isBlank() || mutableSnapshot.value.deviceName == deviceName) return
        mutableSnapshot.value = mutableSnapshot.value.copy(deviceName = deviceName)
    }

    @Synchronized
    fun detected(deviceName: String) {
        transition(
            state = AirPodsConnectionState.Detected,
            deviceName = deviceName,
            bluetoothAudioConnected = true,
            controlChannelConnected = false,
            reason = null,
        )
    }

    @Synchronized
    fun connecting(deviceName: String, manual: Boolean) {
        transition(
            state = AirPodsConnectionState.Connecting,
            deviceName = deviceName,
            bluetoothAudioConnected = true,
            controlChannelConnected = false,
            reason = if (manual) "Manual reconnect" else null,
        )
    }

    @Synchronized
    fun controlConnected(deviceName: String) {
        transition(
            state = AirPodsConnectionState.ControlConnected,
            deviceName = deviceName,
            bluetoothAudioConnected = true,
            controlChannelConnected = true,
            reason = null,
        )
    }

    @Synchronized
    fun ready(deviceName: String) {
        if (mutableSnapshot.value.state == AirPodsConnectionState.Disconnecting) return
        transition(
            state = AirPodsConnectionState.Ready,
            deviceName = deviceName,
            bluetoothAudioConnected = true,
            controlChannelConnected = true,
            reason = null,
        )
    }

    @Synchronized
    fun recovering(deviceName: String, reason: String?) {
        transition(
            state = AirPodsConnectionState.Recovering,
            deviceName = deviceName,
            bluetoothAudioConnected = true,
            controlChannelConnected = false,
            reason = reason?.take(120),
        )
    }

    @Synchronized
    fun disconnecting() {
        transition(
            state = AirPodsConnectionState.Disconnecting,
            bluetoothAudioConnected = mutableSnapshot.value.bluetoothAudioConnected,
            controlChannelConnected = false,
            reason = null,
        )
    }

    @Synchronized
    fun disconnected(reason: String? = null) {
        transition(
            state = AirPodsConnectionState.Disconnected,
            bluetoothAudioConnected = false,
            controlChannelConnected = false,
            reason = reason?.take(120),
        )
    }

    private fun transition(
        state: AirPodsConnectionState,
        deviceName: String = mutableSnapshot.value.deviceName,
        bluetoothAudioConnected: Boolean,
        controlChannelConnected: Boolean,
        reason: String?,
    ) {
        val previous = mutableSnapshot.value
        val next = previous.copy(
            state = state,
            deviceName = deviceName.ifBlank { previous.deviceName },
            bluetoothAudioConnected = bluetoothAudioConnected,
            controlChannelConnected = controlChannelConnected,
            changedAtElapsedRealtime = SystemClock.elapsedRealtime(),
            reason = reason,
        )
        if (next != previous) mutableSnapshot.value = next
    }
}
