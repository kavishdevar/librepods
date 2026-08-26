package me.kavishdevar.librepods.wear.bluetooth

import java.io.InputStream
import java.io.OutputStream

/**
 * Protocol-neutral view of an active AirPods transport.
 *
 * ATT and AACP can consume this interface without knowing how Android created
 * or owns the underlying Bluetooth sockets.
 */
interface AirPodsProtocolTransport {
    val aacpInput: InputStream
    val aacpOutput: OutputStream
    val attInput: InputStream
    val attOutput: OutputStream
}
