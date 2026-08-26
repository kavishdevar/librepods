/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.
*/

package me.kavishdevar.librepods.data

import me.kavishdevar.librepods.R

open class AirPodsBase(
    val modelNumber: List<String>,
    val name: String,
    val displayName: String = "AirPods",
    val manufacturer: String = "Apple Inc.",
    val budCaseRes: Int = R.drawable.airpods,
    val budsRes: Int = R.drawable.airpods,
    val leftBudsRes: Int = R.drawable.airpods,
    val rightBudsRes: Int = R.drawable.airpods,
    val caseRes: Int = R.drawable.airpods,
    val capabilities: Set<Capability>
)

enum class Capability {
    LISTENING_MODE,
    CONVERSATION_AWARENESS,
    STEM_CONFIG,
    HEAD_GESTURES,
    LOUD_SOUND_REDUCTION,
    PPE,
    SLEEP_DETECTION,
    HEARING_AID,
    ADAPTIVE_AUDIO,
    ADAPTIVE_VOLUME,
    SWIPE_FOR_VOLUME,
    HRM
}

class AirPods : AirPodsBase(listOf("A1523", "A1722"), "AirPods 1", capabilities = emptySet())
class AirPods2 : AirPodsBase(listOf("A2032", "A2031"), "AirPods 2", capabilities = emptySet())
class AirPods3 : AirPodsBase(listOf("A2565", "A2564"), "AirPods 3", capabilities = setOf(Capability.HEAD_GESTURES))
class AirPods4 : AirPodsBase(
    listOf("A3053", "A3050", "A3054"), "AirPods 4",
    capabilities = setOf(Capability.HEAD_GESTURES, Capability.SLEEP_DETECTION, Capability.ADAPTIVE_VOLUME)
)
class AirPods4ANC : AirPodsBase(
    listOf("A3056", "A3055", "A3057"), "AirPods 4 (ANC)",
    capabilities = setOf(Capability.LISTENING_MODE, Capability.CONVERSATION_AWARENESS, Capability.HEAD_GESTURES,
        Capability.ADAPTIVE_AUDIO, Capability.SLEEP_DETECTION, Capability.ADAPTIVE_VOLUME, Capability.STEM_CONFIG)
)
class AirPodsPro1 : AirPodsBase(
    listOf("A2084", "A2083"), "AirPods Pro 1", "AirPods Pro",
    capabilities = setOf(Capability.LISTENING_MODE)
)
class AirPodsPro2Lightning : AirPodsBase(
    listOf("A2931", "A2699", "A2698"), "AirPods Pro 2 with Magsafe Charging Case (Lightning)", "AirPods Pro",
    capabilities = setOf(Capability.LISTENING_MODE, Capability.CONVERSATION_AWARENESS, Capability.STEM_CONFIG,
        Capability.LOUD_SOUND_REDUCTION, Capability.SLEEP_DETECTION, Capability.HEARING_AID,
        Capability.ADAPTIVE_AUDIO, Capability.ADAPTIVE_VOLUME, Capability.SWIPE_FOR_VOLUME, Capability.HEAD_GESTURES)
)
class AirPodsPro2USBC : AirPodsBase(
    listOf("A3047", "A3048", "A3049"), "AirPods Pro 2 with Magsafe Charging Case (USB-C)", "AirPods Pro",
    capabilities = setOf(Capability.LISTENING_MODE, Capability.CONVERSATION_AWARENESS, Capability.STEM_CONFIG,
        Capability.LOUD_SOUND_REDUCTION, Capability.SLEEP_DETECTION, Capability.HEARING_AID,
        Capability.ADAPTIVE_AUDIO, Capability.ADAPTIVE_VOLUME, Capability.SWIPE_FOR_VOLUME, Capability.HEAD_GESTURES)
)
class AirPodsPro3 : AirPodsBase(
    listOf("A3063", "A3064", "A3065"), "AirPods Pro 3", "AirPods Pro",
    capabilities = setOf(Capability.LISTENING_MODE, Capability.CONVERSATION_AWARENESS, Capability.HEAD_GESTURES,
        Capability.STEM_CONFIG, Capability.LOUD_SOUND_REDUCTION, Capability.PPE, Capability.SLEEP_DETECTION,
        Capability.HEARING_AID, Capability.ADAPTIVE_AUDIO, Capability.ADAPTIVE_VOLUME, Capability.SWIPE_FOR_VOLUME,
        Capability.HRM)
)

data class AirPodsInstance(
    val name: String,
    val model: AirPodsBase,
    val actualModelNumber: String,
    val serialNumber: String?,
    val leftSerialNumber: String?,
    val rightSerialNumber: String?,
    val version1: String?,
    val version2: String?,
    val version3: String?,
)

object AirPodsModels {
    val models: List<AirPodsBase> = listOf(
        AirPods(), AirPods2(), AirPods3(), AirPods4(), AirPods4ANC(),
        AirPodsPro1(), AirPodsPro2Lightning(), AirPodsPro2USBC(), AirPodsPro3()
    )

    fun getModelByModelNumber(modelNumber: String): AirPodsBase? =
        models.find { modelNumber in it.modelNumber }
}
