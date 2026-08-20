/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
*/

@file:OptIn(ExperimentalEncodingApi::class)

package me.kavishdevar.librepods.bluetooth

import android.util.Log
import me.kavishdevar.librepods.data.Capability
import me.kavishdevar.librepods.data.CustomEq
import me.kavishdevar.librepods.wear.bluetooth.AirPodsProtocolTransport
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Wear-facing AACP packet engine.
 *
 * Transport ownership stays outside this class. The Wear session owns the
 * L2CAP socket while this class owns the AACP handshake and packet framing.
 */
class AACPManager {
    private val tag = "AACPManager[${System.identityHashCode(this)}]"
    private var transport: AirPodsProtocolTransport? = null

    fun bindTransport(protocolTransport: AirPodsProtocolTransport) {
        transport = protocolTransport
        Log.d(tag, "AACP transport bound")
    }

    fun unbindTransport() {
        transport = null
        Log.d(tag, "AACP transport unbound")
    }

    companion object {
        object Opcodes {
            const val SET_FEATURE_FLAGS: Byte = 0x4D
            const val REQUEST_NOTIFICATIONS: Byte = 0x0F
            const val BATTERY_INFO: Byte = 0x04
            const val CONTROL_COMMAND: Byte = 0x09
            const val EAR_DETECTION: Byte = 0x06
            const val CONVERSATION_AWARENESS: Byte = 0x4B
            const val INFORMATION: Byte = 0x1D
            const val RENAME: Byte = 0x1A
            const val HEADTRACKING: Byte = 0x17
            const val PROXIMITY_KEYS_REQ: Byte = 0x30
            const val PROXIMITY_KEYS_RSP: Byte = 0x31
            const val STEM_PRESS: Byte = 0x19
            const val HEADPHONE_ACCOMMODATION: Byte = 0x53
            const val CONNECTED_DEVICES: Byte = 0x2E
            const val AUDIO_SOURCE: Byte = 0x0E
            const val SMART_ROUTING: Byte = 0x10
            const val TIPI_3: Byte = 0x0C
            const val SMART_ROUTING_RESP: Byte = 0x11
            const val SEND_CONNECTED_MAC: Byte = 0x14
            const val AUDIO_SOURCE_2: Byte = 0x0C
            const val CUSTOM_EQ: Byte = 0x63
        }

        private val header = byteArrayOf(0x04, 0x00, 0x04, 0x00)
        private val handshake = byteArrayOf(
            0x00, 0x00, 0x04, 0x00, 0x01, 0x00, 0x02, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        )

        data class ControlCommandStatus(
            val identifier: ControlCommandIdentifiers,
            val value: ByteArray,
        ) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ControlCommandStatus
                return identifier == other.identifier && value.contentEquals(other.value)
            }

            override fun hashCode(): Int = 31 * identifier.hashCode() + value.contentHashCode()
        }

        enum class ControlCommandIdentifiers(val value: Byte) {
            MIC_MODE(0x01), BUTTON_SEND_MODE(0x05), VOICE_TRIGGER(0x12), SINGLE_CLICK_MODE(0x14),
            DOUBLE_CLICK_MODE(0x15), CLICK_HOLD_MODE(0x16), DOUBLE_CLICK_INTERVAL(0x17),
            CLICK_HOLD_INTERVAL(0x18), LISTENING_MODE_CONFIGS(0x1A), ONE_BUD_ANC_MODE(0x1B),
            CROWN_ROTATION_DIRECTION(0x1C), LISTENING_MODE(0x0D), AUTO_ANSWER_MODE(0x1E),
            CHIME_VOLUME(0x1F), VOLUME_SWIPE_INTERVAL(0x23), CALL_MANAGEMENT_CONFIG(0x24),
            VOLUME_SWIPE_MODE(0x25), ADAPTIVE_VOLUME_CONFIG(0x26), SOFTWARE_MUTE_CONFIG(0x27),
            CONVERSATION_DETECT_CONFIG(0x28), SSL(0x29), HEARING_AID(0x2C), AUTO_ANC_STRENGTH(0x2E),
            HPS_GAIN_SWIPE(0x2F), HRM_STATE(0x30), IN_CASE_TONE_CONFIG(0x31), SIRI_MULTITONE_CONFIG(0x32),
            HEARING_ASSIST_CONFIG(0x33), ALLOW_OFF_OPTION(0x34), STEM_CONFIG(0x39),
            SLEEP_DETECTION_CONFIG(0x35), ALLOW_AUTO_CONNECT(0x36), EAR_DETECTION_CONFIG(0x0A),
            AUTOMATIC_CONNECTION_CONFIG(0x20), OWNS_CONNECTION(0x06), PPE_TOGGLE_CONFIG(0x37),
            PPE_CAP_LEVEL_CONFIG(0x38), DYNAMIC_END_OF_CHARGE(0x3B);

            companion object {
                fun fromByte(byte: Byte): ControlCommandIdentifiers? = entries.find { it.value == byte }
            }
        }

        enum class ProximityKeyType(val value: Byte) {
            IRK(0x01), ENC_KEY(0x04);
            companion object {
                fun fromByte(byte: Byte): ProximityKeyType = entries.find { it.value == byte }
                    ?: throw IllegalArgumentException("Unknown ProximityKeyType: $byte")
            }
        }

        enum class StemPressType(val value: Byte) {
            SINGLE_PRESS(0x05), DOUBLE_PRESS(0x06), TRIPLE_PRESS(0x07), LONG_PRESS(0x08)
        }

        enum class StemPressBudType(val value: Byte) { LEFT(0x01), RIGHT(0x02) }
        enum class AudioSourceType(val value: Byte) { NONE(0x00), CALL(0x01), MEDIA(0x02) }

        data class AudioSource(val mac: String, val type: AudioSourceType)
        data class ConnectedDevice(val mac: String, val info1: Byte, val info2: Byte, var type: String?)
        data class AirPodsInformation(
            val name: String,
            val modelNumber: String,
            val manufacturer: String,
            val serialNumber: String,
            val version1: String,
            val version2: String,
            val hardwareRevision: String,
            val updaterIdentifier: String,
            val leftSerialNumber: String,
            val rightSerialNumber: String,
            val version3: String,
        )
    }

    var controlCommandStatusList: MutableList<ControlCommandStatus> = mutableListOf()
    var controlCommandListeners: MutableMap<ControlCommandIdentifiers, MutableList<ControlCommandListener>> = mutableMapOf()
    var owns: Boolean = false
        private set

    interface PacketCallback {
        fun onBatteryInfoReceived(batteryInfo: ByteArray)
        fun onEarDetectionReceived(earDetection: ByteArray)
        fun onConversationAwarenessReceived(conversationAwareness: ByteArray)
        fun onControlCommandReceived(controlCommand: ByteArray)
        fun onDeviceInformationReceived(deviceInformation: AirPodsInformation)
        fun onHeadTrackingReceived(headTracking: ByteArray)
        fun onUnknownPacketReceived(packet: ByteArray)
        fun onProximityKeysReceived(proximityKeys: ByteArray)
        fun onStemPressReceived(stemPress: ByteArray)
        fun onAudioSourceReceived(audioSource: ByteArray)
        fun onOwnershipChangeReceived(owns: Boolean)
        fun onConnectedDevicesReceived(connectedDevices: List<ConnectedDevice>)
        fun onOwnershipToFalseRequest(sender: String, reasonReverseTapped: Boolean)
        fun onShowNearbyUI(sender: String)
        fun onHeadphoneAccommodationReceived(eqData: FloatArray)
        fun onCustomEqReceived(customEq: CustomEq)
        fun onCapabilitiesReceived(capabilities: List<Capability>)
    }

    interface ControlCommandListener {
        fun onControlCommandReceived(controlCommand: ControlCommand)
    }

    private var callback: PacketCallback? = null

    fun setPacketCallback(callback: PacketCallback) {
        this.callback = callback
    }

    /** Send the documented 16-byte AACP session handshake. */
    fun startSession(): Boolean {
        if (!sendRaw(handshake)) return false
        Log.i(tag, "AACP handshake sent")
        return sendNotificationRequest()
    }

    fun createDataPacket(data: ByteArray): ByteArray = header + data

    fun createRequestNotificationPacket(): ByteArray = byteArrayOf(
        Opcodes.REQUEST_NOTIFICATIONS, 0x00,
        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
    )

    fun sendNotificationRequest(): Boolean = sendDataPacket(createRequestNotificationPacket())

    fun sendDataPacket(data: ByteArray): Boolean = sendPacket(createDataPacket(data))

    fun sendPacket(packet: ByteArray): Boolean = sendRaw(packet)

    private fun sendRaw(packet: ByteArray): Boolean {
        return try {
            val output = transport?.aacpOutput ?: BluetoothConnectionManager.aacpSocket?.outputStream ?: return false
            output.write(packet)
            output.flush()
            true
        } catch (error: Exception) {
            Log.e(tag, "Error sending AACP packet", error)
            false
        }
    }

    fun createControlCommandPacket(identifier: Byte, data: ByteArray): ByteArray {
        val payload = ByteArray(7)
        payload[0] = Opcodes.CONTROL_COMMAND
        payload[2] = identifier
        System.arraycopy(data, 0, payload, 3, minOf(data.size, 4))
        return payload
    }

    fun sendControlCommand(identifier: Byte, value: ByteArray): Boolean {
        val id = ControlCommandIdentifiers.fromByte(identifier) ?: return false
        controlCommandStatusList.add(ControlCommandStatus(id, value))
        return sendDataPacket(createControlCommandPacket(identifier, value))
    }

    /** Feed one complete AACP packet from the Wear reader. */
    fun receivePacket(packet: ByteArray) {
        if (packet.size < 6 || !packet.copyOfRange(0, 4).contentEquals(header)) {
            callback?.onUnknownPacketReceived(packet)
            return
        }
        when (packet[4]) {
            Opcodes.BATTERY_INFO -> callback?.onBatteryInfoReceived(packet)
            Opcodes.EAR_DETECTION -> callback?.onEarDetectionReceived(packet)
            Opcodes.CONTROL_COMMAND -> runCatching {
                val command = ControlCommand.fromByteArray(packet)
                ControlCommandIdentifiers.fromByte(command.identifier)?.let { id ->
                    controlCommandStatusList.add(ControlCommandStatus(id, command.value))
                }
                callback?.onControlCommandReceived(packet)
            }.onFailure { callback?.onUnknownPacketReceived(packet) }
            else -> callback?.onUnknownPacketReceived(packet)
        }
    }

    fun registerControlCommandListener(identifier: ControlCommandIdentifiers, callback: ControlCommandListener) {
        controlCommandListeners.getOrPut(identifier) { mutableListOf() }.add(callback)
    }

    fun unregisterControlCommandListener(identifier: ControlCommandIdentifiers, callback: ControlCommandListener) {
        controlCommandListeners[identifier]?.remove(callback)
    }

    data class ControlCommand(val identifier: Byte, val value: ByteArray) {
        companion object {
            fun fromByteArray(data: ByteArray): ControlCommand {
                var offset = 0
                while (data.size - offset >= 4 && data[offset] == 0x04.toByte() && data[offset + 1] == 0x00.toByte() && data[offset + 2] == 0x04.toByte() && data[offset + 3] == 0x00.toByte()) {
                    offset += 4
                }
                if (data.size - offset < 7 || data[offset] != Opcodes.CONTROL_COMMAND) {
                    throw IllegalArgumentException("Invalid ControlCommand packet")
                }
                val identifier = data[offset + 2]
                val value = data.copyOfRange(offset + 3, offset + 7)
                return ControlCommand(identifier, value)
            }
        }
    }
}
