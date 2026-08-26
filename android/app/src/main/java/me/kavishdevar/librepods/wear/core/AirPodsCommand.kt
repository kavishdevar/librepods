package me.kavishdevar.librepods.wear.core

import me.kavishdevar.librepods.bluetooth.AACPManager

/** Commands accepted by the autonomous Wear controller. */
sealed interface AirPodsCommand {
    data object Connect : AirPodsCommand
    data object Disconnect : AirPodsCommand
    data class SetListeningMode(val mode: ListeningMode) : AirPodsCommand
    data class SetEarDetection(val enabled: Boolean) : AirPodsCommand
    data class SetConversationalAwareness(val enabled: Boolean) : AirPodsCommand
    data class SetControlBoolean(
        val identifier: AACPManager.Companion.ControlCommandIdentifiers,
        val enabled: Boolean,
    ) : AirPodsCommand
    data class SetControlByte(
        val identifier: AACPManager.Companion.ControlCommandIdentifiers,
        val value: Int,
    ) : AirPodsCommand
    data object RefreshState : AirPodsCommand
}
