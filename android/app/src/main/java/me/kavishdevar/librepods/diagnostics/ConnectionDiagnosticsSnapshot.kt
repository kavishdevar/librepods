/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.
*/

package me.kavishdevar.librepods.diagnostics

data class ConnectionDiagnosticsSnapshot(
    val stateLabel: String,
    val deviceName: String,
    val bluetoothAudioConnected: Boolean,
    val controlChannelConnected: Boolean,
    val leftBattery: String,
    val rightBattery: String,
    val caseBattery: String,
    val batteryFreshnessLabel: String,
    val headTrackingActive: Boolean,
    val lastHeadTrackingPacketLabel: String,
    val liveAlertEnabled: Boolean,
    val suppressedIntegrations: List<String>,
    val generatedAtLabel: String,
    val redactedReport: String,
)
