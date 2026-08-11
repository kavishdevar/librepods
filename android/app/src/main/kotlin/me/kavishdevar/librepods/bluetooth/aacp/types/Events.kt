package me.kavishdevar.librepods.bluetooth.aacp.types

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

    data class HeadGesturesResult(
        val yes: Boolean,
    )
}
