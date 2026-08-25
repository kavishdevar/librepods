/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.
*/

package me.kavishdevar.librepods.battery

import android.os.SystemClock
import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryStatus
import me.kavishdevar.librepods.utils.BatteryLevels

enum class BatteryDataSource(
    val description: String,
    internal val freshForMillis: Long,
) {
    AACP("live control channel", 90_000L),
    BLE_EXACT("nearby exact reading", 45_000L),
    BLE_APPROXIMATE("nearby estimate", 45_000L),
    BLE_CACHED_CASE("last case reading", 15 * 60_000L),
}

data class BatteryObservation(
    val battery: Battery,
    val source: BatteryDataSource,
    val observedAtElapsedRealtime: Long,
) {
    fun ageMillis(now: Long = SystemClock.elapsedRealtime()): Long =
        (now - observedAtElapsedRealtime).coerceAtLeast(0L)

    fun isFresh(now: Long = SystemClock.elapsedRealtime()): Boolean =
        ageMillis(now) <= source.freshForMillis
}

/**
 * Records where and when each component was actually observed. This intentionally sits beside
 * the protocol's Parcelable [Battery] model so old broadcasts remain compatible.
 */
class BatteryStateTracker {
    private val observations = mutableMapOf<Int, BatteryObservation>()

    @Synchronized
    fun observe(
        batteries: List<Battery>,
        source: BatteryDataSource,
        components: Set<Int> = batteries.mapTo(mutableSetOf()) { it.component },
        observedAtElapsedRealtime: Long = SystemClock.elapsedRealtime(),
    ) {
        batteries.asSequence()
            .filter { it.component in components }
            .forEach { battery ->
                observations[battery.component] = BatteryObservation(
                    battery = battery,
                    source = source,
                    observedAtElapsedRealtime = observedAtElapsedRealtime,
                )
            }
    }

    @Synchronized
    fun clear() {
        observations.clear()
    }

    @Synchronized
    fun snapshot(): Map<Int, BatteryObservation> = observations.toMap()

    fun componentLabel(component: Int, now: Long = SystemClock.elapsedRealtime()): String {
        val observation = synchronized(this) { observations[component] } ?: return "Unavailable"
        val battery = observation.battery
        if (!BatteryLevels.isKnown(battery.level) || battery.status == BatteryStatus.DISCONNECTED) {
            return "Unavailable"
        }
        val charging = when (battery.status) {
            BatteryStatus.CHARGING, BatteryStatus.OPTIMIZED_CHARGING -> " · charging"
            else -> ""
        }
        val stale = if (observation.isFresh(now)) "" else " · last known"
        return "${battery.level}%$charging · ${observation.source.description}$stale"
    }

    fun freshnessLabel(now: Long = SystemClock.elapsedRealtime()): String {
        val values = synchronized(this) { observations.values.toList() }
            .filter { BatteryLevels.isKnown(it.battery.level) }
        if (values.isEmpty()) return "Waiting for a valid reading"

        val newest = values.minOf { it.ageMillis(now) }
        val hasFresh = values.any { it.isFresh(now) }
        val prefix = if (hasFresh) "Updated" else "Last updated"
        val suffix = if (hasFresh) "" else " · readings may be stale"
        return "$prefix ${formatAge(newest)}$suffix"
    }

    companion object {
        fun elapsedTimeForWallClock(observedAtMillis: Long): Long {
            val wallAge = (System.currentTimeMillis() - observedAtMillis).coerceAtLeast(0L)
            return (SystemClock.elapsedRealtime() - wallAge).coerceAtLeast(0L)
        }

        fun formatAge(ageMillis: Long): String = when {
            ageMillis < 5_000L -> "just now"
            ageMillis < 60_000L -> "${ageMillis / 1_000L}s ago"
            ageMillis < 60 * 60_000L -> "${ageMillis / 60_000L}m ago"
            else -> "${ageMillis / (60 * 60_000L)}h ago"
        }
    }
}
