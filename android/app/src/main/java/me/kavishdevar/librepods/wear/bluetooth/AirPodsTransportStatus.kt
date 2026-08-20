package me.kavishdevar.librepods.wear.bluetooth

/**
 * Small diagnostic snapshot used by the Wear UI and reconnect logic.
 * Protocol handshake state intentionally stays separate from socket state.
 */
data class AirPodsTransportStatus(
    val state: AirPodsConnectionSession.State,
    val aacpOpen: Boolean,
    val attOpen: Boolean,
    val lastError: String? = null,
) {
    val transportReady: Boolean
        get() = state == AirPodsConnectionSession.State.CONNECTED && aacpOpen && attOpen
}
