package me.kavishdevar.librepods.devices

import me.kavishdevar.librepods.bluetooth.MacAddress
import me.kavishdevar.librepods.bluetooth.aacp.packet.AACPPacket
import me.kavishdevar.librepods.bluetooth.aacp.types.AudioSource
import me.kavishdevar.librepods.bluetooth.aacp.types.AudioSourceType
import me.kavishdevar.librepods.bluetooth.aacp.types.CapabilityEntry
import me.kavishdevar.librepods.bluetooth.aacp.types.ConnectedDevice
import me.kavishdevar.librepods.bluetooth.aacp.types.ControlCommandIdentifier
import me.kavishdevar.librepods.bluetooth.aacp.types.CustomEq
import me.kavishdevar.librepods.bluetooth.aacp.types.MagicKeyType
import me.kavishdevar.librepods.data.apple.BuddyState
import me.kavishdevar.librepods.data.audio.MicrophoneFrame
import me.kavishdevar.librepods.data.audio.MicrophoneState
import me.kavishdevar.librepods.data.heartrate.HeartRateSample
import me.kavishdevar.librepods.data.recording.RecordingState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class AppleState(
    val isLocallyConnected: Boolean = false,

    val owns: Boolean = true,

    val componentState: Set<DeviceComponentState> = emptySet(),

    val conversationalAwarenessState: Int = 0,

    val capabilities: Set<CapabilityEntry> = emptySet(),

    val battery: Set<Battery> = emptySet(),
    val controlStates: Map<ControlCommandIdentifier, ByteArray> = emptyMap(),

    val magicKeys: Map<MagicKeyType, ByteArray> = emptyMap(),

    val headTrackingState: BuddyState = BuddyState.INACTIVE,

    val loudSoundReductionEnabled: Boolean = false,
    val transparencyData: ByteArray = byteArrayOf(),
    val hearingAidData: ByteArray = byteArrayOf(),

    val customEq: CustomEq = CustomEq(1, 50, 50, 50),

    val microphoneFrames: List<MicrophoneFrame> = emptyList(),
    val microphoneState: MicrophoneState = MicrophoneState(),
    val recordingState: RecordingState = RecordingState(),

    val audioSource: AudioSource = AudioSource(MacAddress("00:00:00:00:00:00"), AudioSourceType.NONE),
    var connectedDevices: List<ConnectedDevice> = emptyList(),

    val leftIsPrimary: Boolean = true,

    val headphoneAccomodation: FloatArray = FloatArray(8),
    val headphoneAccomodationEnabledForMedia: Boolean = false,
    val headphoneAccomodationEnabledForPhone: Boolean = false,

    val currentHeartRate: HeartRateSample? = null,
    val hrmState: BuddyState = BuddyState.INACTIVE,
    val heartRateInterval: Duration = 1.seconds,

    val aacpPackets: List<AACPPacket> = emptyList(),
): DeviceState
