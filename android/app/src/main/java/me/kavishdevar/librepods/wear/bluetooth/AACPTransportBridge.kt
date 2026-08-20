package me.kavishdevar.librepods.wear.bluetooth

import android.util.Log
import me.kavishdevar.librepods.bluetooth.AACPManager

/**
 * Transitional bridge between the new Wear-owned transport and the inherited
 * AACP implementation. It deliberately does not implement packet framing;
 * AACPManager remains the single source of truth for packet construction and
 * parsing until its socket dependency is removed completely.
 */
class AACPTransportBridge(
    private val transport: AirPodsProtocolTransport,
    private val manager: AACPManager,
) {
    private val tag = "AACPTransportBridge"

    /** Sends one already-framed AACP packet through the Wear-owned socket. */
    fun send(packet: ByteArray): Boolean = runCatching {
        synchronized(transport.aacpOutput) {
            transport.aacpOutput.write(packet)
            transport.aacpOutput.flush()
        }
        true
    }.onFailure { Log.e(tag, "Failed to send AACP packet", it) }.getOrDefault(false)

    /** Delivers an already-framed packet to the inherited AACP parser. */
    fun receive(packet: ByteArray) {
        manager.receivePacket(packet)
    }
}
