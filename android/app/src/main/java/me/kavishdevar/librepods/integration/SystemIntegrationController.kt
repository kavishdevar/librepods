/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.
*/

package me.kavishdevar.librepods.integration

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserHandle
import android.util.Log
import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryComponent
import me.kavishdevar.librepods.utils.BatteryLevels
import me.kavishdevar.librepods.utils.SystemApisUtils

/**
 * Owns optional Android/OxygenOS integrations that normal applications may not be allowed to use.
 * A denied or unsupported operation is attempted at most once per process, then stays quiet.
 */
class SystemIntegrationController(private val context: Context) {
    private val suppressed = linkedSetOf<String>()

    fun applyBluetoothMetadata(
        device: BluetoothDevice,
        entries: List<Pair<Int, ByteArray>>,
    ): Boolean {
        if (!hasPermission(BLUETOOTH_PRIVILEGED)) {
            suppress(METADATA_LABEL, "missing privileged Bluetooth permission")
            return false
        }
        if (isSuppressed(METADATA_LABEL)) return false

        for ((key, value) in entries) {
            if (!SystemApisUtils.setMetadata(device, key, value)) {
                suppress(METADATA_LABEL, "Bluetooth metadata API rejected key $key")
                return false
            }
        }
        return true
    }

    fun publishSystemBattery(device: BluetoothDevice?, batteries: List<Battery>): Boolean {
        device ?: return false
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            suppress(SYSTEM_BATTERY_LABEL, "missing Bluetooth connect permission")
            return false
        }
        if (!hasPermission(INTERACT_ACROSS_USERS)) {
            suppress(SYSTEM_BATTERY_LABEL, "missing cross-user system permission")
            return false
        }
        if (isSuppressed(SYSTEM_BATTERY_LABEL)) return false

        val unifiedLevel = batteries.asSequence()
            .filter { it.component == BatteryComponent.LEFT || it.component == BatteryComponent.RIGHT }
            .map { it.level }
            .filter(BatteryLevels::isKnown)
            .minOrNull()
            ?: return false

        return try {
            val vendorIntent = Intent(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT).apply {
                putExtra(
                    BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD,
                    VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV,
                )
                putExtra(
                    BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE,
                    BluetoothHeadset.AT_CMD_TYPE_SET,
                )
                putExtra(
                    BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS,
                    arrayOf<Any>(1, VENDOR_SPECIFIC_HEADSET_EVENT_BATTERY_LEVEL, unifiedLevel),
                )
                putExtra(BluetoothDevice.EXTRA_DEVICE, device)
                putExtra(BluetoothDevice.EXTRA_NAME, device.name)
                addCategory(
                    "${BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY}.$APPLE_COMPANY_ID"
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.sendBroadcastAsUser(
                    vendorIntent,
                    UserHandle.getUserHandleForUid(-1),
                    Manifest.permission.BLUETOOTH_CONNECT,
                )
            } else {
                context.sendBroadcastAsUser(vendorIntent, UserHandle.getUserHandleForUid(-1))
            }

            val batteryIntent = Intent(ACTION_BATTERY_LEVEL_CHANGED).apply {
                putExtra(BluetoothDevice.EXTRA_DEVICE, device)
                putExtra(EXTRA_BATTERY_LEVEL, unifiedLevel)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.sendBroadcast(batteryIntent, Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                context.sendBroadcastAsUser(
                    batteryIntent,
                    UserHandle.getUserHandleForUid(-1),
                )
            }

            context.sendBroadcastAsUser(
                Intent(ACTION_ASI_UPDATE_BLUETOOTH_DATA).apply {
                    setPackage(PACKAGE_ASI)
                    putExtra(ACTION_BATTERY_LEVEL_CHANGED, vendorIntent)
                },
                UserHandle.getUserHandleForUid(-1),
            )
            true
        } catch (error: Exception) {
            suppress(SYSTEM_BATTERY_LABEL, error.javaClass.simpleName)
            false
        }
    }

    @Synchronized
    fun suppressedIntegrations(): List<String> {
        if (!hasPermission(BLUETOOTH_PRIVILEGED)) suppressed += METADATA_LABEL
        if (!hasPermission(INTERACT_ACROSS_USERS)) suppressed += SYSTEM_BATTERY_LABEL
        if (Build.MANUFACTURER.contains("oneplus", ignoreCase = true)) {
            suppressed += HEAD_TRACKED_SPATIAL_AUDIO_LABEL
        }
        return suppressed.toList()
    }

    @Synchronized
    private fun isSuppressed(label: String): Boolean = label in suppressed

    @Synchronized
    private fun suppress(label: String, reason: String) {
        if (suppressed.add(label)) {
            Log.i(TAG, "$label disabled for this session: $reason")
        }
    }

    private fun hasPermission(permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val TAG = "SystemIntegration"
        const val BLUETOOTH_PRIVILEGED = "android.permission.BLUETOOTH_PRIVILEGED"
        const val INTERACT_ACROSS_USERS = "android.permission.INTERACT_ACROSS_USERS"

        const val METADATA_LABEL =
            "System AirPods artwork and split-battery metadata require privileged Android access"
        const val SYSTEM_BATTERY_LABEL =
            "Android Settings battery integration requires system-level access"
        const val HEAD_TRACKED_SPATIAL_AUDIO_LABEL =
            "OnePlus head-tracked spatial audio is not exposed to third-party apps"

        const val VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV = "+IPHONEACCEV"
        const val VENDOR_SPECIFIC_HEADSET_EVENT_BATTERY_LEVEL = 1
        const val APPLE_COMPANY_ID = 0x004C
        const val ACTION_BATTERY_LEVEL_CHANGED =
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"
        const val PACKAGE_ASI = "com.google.android.settings.intelligence"
        const val ACTION_ASI_UPDATE_BLUETOOTH_DATA =
            "batterywidget.impl.action.update_bluetooth_data"
    }
}
