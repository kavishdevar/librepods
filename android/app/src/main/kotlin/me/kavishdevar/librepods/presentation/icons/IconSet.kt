@file:Suppress("PropertyName")

package me.kavishdevar.librepods.presentation.icons

import androidx.compose.ui.graphics.vector.ImageVector
import me.kavishdevar.librepods.presentation.icons.common.airpods.AirPods
import me.kavishdevar.librepods.presentation.icons.common.airpods.AirPods3
import me.kavishdevar.librepods.presentation.icons.common.airpods.AirPods4
import me.kavishdevar.librepods.presentation.icons.common.airpods.AirPods4Case
import me.kavishdevar.librepods.presentation.icons.common.airpods.AirPods4CaseFill
import me.kavishdevar.librepods.presentation.icons.common.airpods.AirPods4Left
import me.kavishdevar.librepods.presentation.icons.common.airpods.AirPods4Right
import me.kavishdevar.librepods.presentation.icons.common.airpods.AirPodsCase
import me.kavishdevar.librepods.presentation.icons.common.airpods.AirPodsCaseFill
import me.kavishdevar.librepods.presentation.icons.common.airpods.AirPodsMax
import me.kavishdevar.librepods.presentation.icons.common.airpods.AirPodsPro1
import me.kavishdevar.librepods.presentation.icons.common.airpods.AirPodsPro1Case
import me.kavishdevar.librepods.presentation.icons.common.airpods.AirPodsPro1CaseFill
import me.kavishdevar.librepods.presentation.icons.common.airpods.AirPodsPro1Left
import me.kavishdevar.librepods.presentation.icons.common.airpods.AirPodsPro1Right
import me.kavishdevar.librepods.presentation.icons.common.airpods.AirPodsPro3
import me.kavishdevar.librepods.presentation.icons.common.airpods.AirPodsPro3Case
import me.kavishdevar.librepods.presentation.icons.common.airpods.AirPodsPro3CaseFill
import me.kavishdevar.librepods.presentation.icons.common.airpods.AirPodsPro3Left
import me.kavishdevar.librepods.presentation.icons.common.airpods.AirPodsPro3Right
import me.kavishdevar.librepods.presentation.icons.common.airpods.AirPodsWirelessCase

interface IconSet {
    val Notifications: ImageVector
    val Headphones: ImageVector
    val Play: ImageVector
    val Pause: ImageVector
    val Bluetooth: ImageVector
    val Call: ImageVector
    val Overlay: ImageVector
    val ArrowBack: ImageVector
    val LeftCircleFill: ImageVector
    val RightCircleFill: ImageVector
    val Settings: ImageVector
    val Send: ImageVector
    val Close: ImageVector
    val CloseCircle: ImageVector
    val SpeakerMin: ImageVector
    val SpeakerMax: ImageVector
    val Bolt: ImageVector
    val Check: ImageVector
    val ChevronLeft: ImageVector
    val ChevronRight: ImageVector
    val Save: ImageVector
    val Incoming: ImageVector
    val Outgoing: ImageVector

    val BoltCircle: ImageVector
    val Circle: ImageVector

    val CircleDotted: ImageVector

    val VitalSigns: ImageVector

    /*
     * AirPods Icons
     */


    val AirPods1: ImageVector
        get() = CommonIcons.AirPods
    val AirPods1Case: ImageVector
        get() = CommonIcons.AirPodsCase
    val AirPods1CaseFill: ImageVector
        get() = CommonIcons.AirPodsCaseFill

    val AirPods2: ImageVector
        get() = CommonIcons.AirPods
    val AirPods2Case: ImageVector
        get() = CommonIcons.AirPodsWirelessCase
    val AirPods2CaseFill: ImageVector
        get() = CommonIcons.AirPodsCaseFill

    val AirPods3: ImageVector
        get() = CommonIcons.AirPods3
    val AirPods3Case: ImageVector
        get() = CommonIcons.AirPodsPro3Case
    val AirPods3CaseFill: ImageVector
        get() = CommonIcons.AirPodsPro3CaseFill

    val AirPods4: ImageVector
        get() = CommonIcons.AirPods4
    val AirPods4Left: ImageVector
        get() = CommonIcons.AirPods4Left
    val AirPods4Right: ImageVector
        get() = CommonIcons.AirPods4Right
    val AirPods4Case: ImageVector
        get() = CommonIcons.AirPods4Case
    val AirPods4CaseFill: ImageVector
        get() = CommonIcons.AirPods4CaseFill

    val AirPodsPro1: ImageVector
        get() = CommonIcons.AirPodsPro1
    val AirPodsPro1Left: ImageVector
        get() = CommonIcons.AirPodsPro1Left
    val AirPodsPro1Right: ImageVector
        get() = CommonIcons.AirPodsPro1Right
    val AirPodsPro1Case: ImageVector
        get() = CommonIcons.AirPodsPro1Case
    val AirPodsPro1CaseFill: ImageVector
        get() = CommonIcons.AirPodsPro1CaseFill

    val AirPodsPro2: ImageVector
        get() = CommonIcons.AirPodsPro1
    val AirPodsPro2Left: ImageVector
        get() = CommonIcons.AirPodsPro1Left
    val AirPodsPro2Right: ImageVector
        get() = CommonIcons.AirPodsPro1Right
    val AirPodsPro2Case: ImageVector
        get() = CommonIcons.AirPodsPro1Case
    val AirPodsPro2CaseFill: ImageVector
        get() = CommonIcons.AirPodsPro1CaseFill

    val AirPodsPro3: ImageVector
        get() = CommonIcons.AirPodsPro3
    val AirPodsPro3Left: ImageVector
        get() = CommonIcons.AirPodsPro3Left
    val AirPodsPro3Right: ImageVector
        get() = CommonIcons.AirPodsPro3Right
    val AirPodsPro3Case: ImageVector
        get() = CommonIcons.AirPodsPro3Case
    val AirPodsPro3CaseFill: ImageVector
        get() = CommonIcons.AirPodsPro3CaseFill

    val AirPodsMax: ImageVector
        get() = CommonIcons.AirPodsMax

    val IconMap: Map<String, ImageVector>
        get() = mapOf(
            "Notifications" to Notifications,
            "Headphones" to Headphones,
            "Play" to Play,
            "Pause" to Pause,
            "Bluetooth" to Bluetooth,
            "Overlay" to Overlay,
            "ArrowBack" to ArrowBack,
            "LeftCircleFill" to LeftCircleFill,
            "RightCircleFill" to RightCircleFill,
            "Settings" to Settings,
            "Send" to Send,
            "Close" to Close,
            "CloseCircle" to CloseCircle,
            "SpeakerMin" to SpeakerMin,
            "SpeakerMax" to SpeakerMax,
            "Bolt" to Bolt,
            "Check" to Check,
            "ChevronLeft" to ChevronLeft,
            "ChevronRight" to ChevronRight,
            "Save" to Save,
            "Incoming" to Incoming,
            "Outgoing" to Outgoing,
            "BoltCircle" to BoltCircle,
            "Circle" to Circle,
            "CircleDotted" to CircleDotted,
            "VitalSign" to VitalSigns,

            "AirPods1" to AirPods1,
            "AirPods1Case" to AirPods1Case,
            "AirPods1CaseFill" to AirPods1CaseFill,
            "AirPods2" to AirPods2,
            "AirPods2Case" to AirPods2Case,
            "AirPods2CaseFill" to AirPods2CaseFill,
            "AirPods3" to AirPods3,
            "AirPods3Case" to AirPods3Case,
            "AirPods3CaseFill" to AirPods3CaseFill,
            "AirPods4" to AirPods4,
            "AirPods4Left" to AirPods4Left,
            "AirPods4Right" to AirPods4Right,
            "AirPods4Case" to AirPods4Case,
            "AirPods4CaseFill" to AirPods4CaseFill,
            "AirPodsPro1" to AirPodsPro1,
            "AirPodsPro1Left" to AirPodsPro1Left,
            "AirPodsPro1Right" to AirPodsPro1Right,
            "AirPodsPro1Case" to AirPodsPro1Case,
            "AirPodsPro1CaseFill" to AirPodsPro1CaseFill,
            "AirPodsPro2" to AirPodsPro2,
            "AirPodsPro2Left" to AirPodsPro2Left,
            "AirPodsPro2Right" to AirPodsPro2Right,
            "AirPodsPro2Case" to AirPodsPro2Case,
            "AirPodsPro2CaseFill" to AirPodsPro2CaseFill,
            "AirPodsPro3" to AirPodsPro3,
            "AirPodsPro3Left" to AirPodsPro3Left,
            "AirPodsPro3Right" to AirPodsPro3Right,
            "AirPodsPro3Case" to AirPodsPro3Case,
            "AirPodsPro3CaseFill" to AirPodsPro3CaseFill,
            "AirPodsMax" to AirPodsMax,
        )

    fun fromName(name: String): ImageVector? {
        return IconMap[name]
    }
}
