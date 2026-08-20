package me.kavishdevar.librepods.wear.core

/** Commands accepted by the autonomous Wear controller. */
sealed interface AirPodsCommand {
    data object Connect : AirPodsCommand
    data object Disconnect : AirPodsCommand
    data class SetListeningMode(val mode: ListeningMode) : AirPodsCommand
    data class SetEarDetection(val enabled: Boolean) : AirPodsCommand
    data class SetConversationalAwareness(val enabled: Boolean) : AirPodsCommand
    data object RefreshState : AirPodsCommand
}
