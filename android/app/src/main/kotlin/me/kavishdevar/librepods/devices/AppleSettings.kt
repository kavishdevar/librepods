package me.kavishdevar.librepods.devices

import kotlinx.serialization.Serializable
import me.kavishdevar.librepods.data.StemAction
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Serializable
data class AppleSettings(
    val disconnectWhenNotWearing: Boolean = true, // disconnect_when_not_wearing

    val cacheDisconnectedComponentBattery: Boolean = true,

    val headGesturesEnabled: Boolean = true, // head_gestures_enabled
    val headGesturesVerticalOffset: Int = 30,
    val headGesturesHorizontalOffset: Int = 28,
    val headTrackingInterval: Duration = 40.milliseconds,

    val leftLongPressAction: StemAction = StemAction.CYCLE_NOISE_CONTROL_MODES, // left_long_press_action
    val rightLongPressAction: StemAction = StemAction.CYCLE_NOISE_CONTROL_MODES, // right_long_press_action

    val showIslandPopup: Boolean = true, // show_island_popup
    val showBottomSheetPopup: Boolean = true, // show_bottom_sheet_popup

    val takeoverWhenDisconnected: Boolean = true, // takeover_when_disconnected
    val takeoverWhenIdle: Boolean = true, // takeover_when_idle
    val takeoverWhenMusic: Boolean = true, // takeover_when_music
    val takeoverWhenCall: Boolean = true, // takeover_when_call

    val takeoverWhenRingingCall: Boolean = true, // takeover_when_ringing_call
    val takeoverWhenMediaStart: Boolean = true, // takeover_when_media_start

    val conversationalAwarenessPauseMusicEnabled: Boolean = false, // conversational_awareness_pause_music
    val relativeConversationalAwarenessVolumeEnabled: Boolean = true, // relative_conversational_awareness_volume

    val conversationalAwarenessVolume: Float = 43f, // conversational_awareness_volume

): DeviceSettings
