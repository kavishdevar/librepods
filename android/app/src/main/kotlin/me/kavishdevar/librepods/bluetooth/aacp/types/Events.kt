package me.kavishdevar.librepods.bluetooth.aacp.types

import me.kavishdevar.librepods.data.audio.MicrophoneFrame

sealed interface AppleEvent {
    data class StemPress(
        val pressType: StemPressType,
        val bud: StemPressBud
    ): AppleEvent

    data class ShowNearbyUi(
        val sender: String
    ): AppleEvent

    data class OwnershipToFalseRequest(
        val sender: String,
        val reverseTapped: Boolean
    ): AppleEvent

    data class MicrophoneFrameEvent(
        val frame: MicrophoneFrame
    )
}
