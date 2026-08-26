package me.kavishdevar.librepods.wear.core

/** Events emitted by the autonomous AirPods protocol pipeline. */
sealed interface AirPodsEvent {
    data class Connected(val address: String, val name: String?) : AirPodsEvent
    data object Disconnected : AirPodsEvent
    data class Battery(val left: Int?, val right: Int?, val caseBattery: Int?) : AirPodsEvent
    data class EarDetection(val leftInEar: Boolean, val rightInEar: Boolean) : AirPodsEvent
    data class ListeningMode(val mode: me.kavishdevar.librepods.wear.core.ListeningMode) : AirPodsEvent
    data class Error(val message: String, val cause: Throwable? = null) : AirPodsEvent
}
