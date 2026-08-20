package me.kavishdevar.librepods.wear.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Immutable state exposed by the autonomous Wear AirPods controller. */
data class AirPodsState(
    val deviceName: String = "AirPods",
    val address: String? = null,
    val connected: Boolean = false,
    val connecting: Boolean = false,
    val leftBattery: Int? = null,
    val rightBattery: Int? = null,
    val caseBattery: Int? = null,
    val listeningMode: ListeningMode = ListeningMode.OFF,
    val leftInEar: Boolean? = null,
    val rightInEar: Boolean? = null,
    val earDetectionEnabled: Boolean? = null,
    val conversationalAwarenessEnabled: Boolean? = null,
    val lastError: String? = null,
)

enum class ListeningMode {
    ANC,
    TRANSPARENCY,
    OFF,
}

/** Small state holder; protocol adapters can update it without knowing about Compose. */
class AirPodsStateStore(initial: AirPodsState = AirPodsState()) {
    private val mutableState = MutableStateFlow(initial)
    val state: StateFlow<AirPodsState> = mutableState.asStateFlow()

    fun update(transform: (AirPodsState) -> AirPodsState) {
        mutableState.value = transform(mutableState.value)
    }

    fun reset() {
        mutableState.value = AirPodsState()
    }
}
