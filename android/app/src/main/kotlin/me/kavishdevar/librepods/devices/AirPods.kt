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

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package me.kavishdevar.librepods.devices

import androidx.annotation.DrawableRes
import kotlinx.serialization.Serializable
import me.kavishdevar.librepods.R

data class ComponentSpec(
    val type: DeviceComponent,
    val iconName: String,
    val label: String,
)

@Serializable
enum class AirPodsModel {
    AIRPODS_1,
    AIRPODS_2,
    AIRPODS_3,
    AIRPODS_4,
    AIRPODS_4_ANC,
    AIRPODS_PRO_1,
    AIRPODS_PRO_2_LIGHTNING,
    AIRPODS_PRO_2_USBC,
    AIRPODS_PRO_3,
    AIRPODS_MAX_LIGHTNING,
    AIRPODS_MAX_USBC,
    AIRPODS_MAX_2,
    UNKNOWN;

    companion object {
        fun fromModelNumber(modelNumber: String): AirPodsModel {
            return AirPodsSpecs.specs.entries.firstOrNull { (_, spec) ->
                spec.modelNumbers.contains(modelNumber)
            }?.key ?: UNKNOWN
        }
    }
}

data class AirPodsSpec(
    val modelNumbers: Set<String>,
    val name: String,
    val displayName: String,
    val components: Set<ComponentSpec> = emptySet(),
    val genericIconName: String = "AirPodsPro3",
    @DrawableRes val primaryImageRes: Int = R.drawable.img_airpods_pro_2_buds,
    @DrawableRes val caseImageRes: Int? = R.drawable.img_airpods_pro_2_case,
    val baseCapabilities: Set<BaseCapability>,
)

object AirPodsSpecs {
    internal val specs = mapOf(
        AirPodsModel.AIRPODS_1 to AirPodsSpec(
            modelNumbers = setOf("A1523", "A1722"),
            name = "AirPods 1",
            displayName = "AirPods1",
            components = setOf(
                ComponentSpec(
                    type = DeviceComponent.LEFT,
                    iconName = "LeftCircleFill",
                    label = "Left",
                ),
                ComponentSpec(
                    type = DeviceComponent.RIGHT,
                    iconName = "RightCircleFill",
                    label = "Right"
                ),
                ComponentSpec(
                    type = DeviceComponent.CASE,
                    iconName = "AirPods1Case",
                    label = "Charging Case"
                )
            ),
            genericIconName = "AirPods",
            baseCapabilities = emptySet()
        ),
        AirPodsModel.AIRPODS_2 to AirPodsSpec(
            modelNumbers = setOf("A2032", "A2031"),
            name = "AirPods 2",
            displayName = "AirPods",
            components = setOf(
                ComponentSpec(
                    type = DeviceComponent.LEFT,
                    iconName = "LeftCircleFill",
                    label = "Left"
                ),
                ComponentSpec(
                    type = DeviceComponent.RIGHT,
                    iconName = "RightCircleFill",
                    label = "Right"
                ),
                ComponentSpec(
                    type = DeviceComponent.CASE,
                    iconName = "AirPods2Case",
                    label = "Charging Case"
                )
            ),
            genericIconName = "AirPods",
            baseCapabilities = emptySet()
        ),
        AirPodsModel.AIRPODS_3 to AirPodsSpec(
            modelNumbers = setOf("A2565", "A2564"),
            name = "AirPods 3",
            displayName = "AirPods",
            components = setOf(
                ComponentSpec(
                    type = DeviceComponent.LEFT,
                    iconName = "LeftCircleFill",
                    label = "Left"
                ),
                ComponentSpec(
                    type = DeviceComponent.RIGHT,
                    iconName = "RightCircleFill",
                    label = "Right"
                ),
                ComponentSpec(
                    type = DeviceComponent.CASE,
                    iconName = "AirPods3Case",
                    label = "Charging Case"
                )
            ),
            genericIconName = "AirPods3",
            baseCapabilities = setOf(
                BaseCapability.HEAD_GESTURES
            )
        ),
        AirPodsModel.AIRPODS_4 to AirPodsSpec(
            modelNumbers = setOf("A3053", "A3050", "A3054"),
            name = "AirPods 4",
            displayName = "AirPods",
            components = setOf(
                ComponentSpec(
                    type = DeviceComponent.LEFT,
                    iconName = "AirPods4Left",
                    label = "Left"
                ),
                ComponentSpec(
                    type = DeviceComponent.RIGHT,
                    iconName = "AirPods4Right",
                    label = "Right"
                ),
                ComponentSpec(
                    type = DeviceComponent.CASE,
                    iconName = "AirPods4Case",
                    label = "Charging Case"
                )
            ),
            genericIconName = "AirPods4",
            baseCapabilities = setOf(
                BaseCapability.HEAD_GESTURES,
                BaseCapability.SLEEP_DETECTION,
                BaseCapability.ADAPTIVE_VOLUME
            )
        ),
        AirPodsModel.AIRPODS_4_ANC to AirPodsSpec(
            modelNumbers = setOf("A3056", "A3055", "A3057"),
            name = "AirPods 4 (ANC)",
            displayName = "AirPods",
            components = setOf(
                ComponentSpec(
                    type = DeviceComponent.LEFT,
                    iconName = "AirPods4Left",
                    label = "Left"
                ),
                ComponentSpec(
                    type = DeviceComponent.RIGHT,
                    iconName = "AirPods4Right",
                    label = "Right"
                ),
                ComponentSpec(
                    type = DeviceComponent.CASE,
                    iconName = "AirPods4Case",
                    label = "Charging Case"
                )
            ),
            genericIconName = "AirPods4",
            baseCapabilities = setOf(
                BaseCapability.LISTENING_MODE,
                BaseCapability.CONVERSATION_AWARENESS,
                BaseCapability.HEAD_GESTURES,
                BaseCapability.ADAPTIVE_AUDIO,
                BaseCapability.SLEEP_DETECTION,
                BaseCapability.ADAPTIVE_VOLUME,
                BaseCapability.STEM_CONFIG
            )
        ),
        AirPodsModel.AIRPODS_PRO_1 to AirPodsSpec(
            modelNumbers = setOf("A2084", "A2083"),
            name = "AirPods Pro 1",
            displayName = "AirPods Pro",
            components = setOf(
                ComponentSpec(
                    type = DeviceComponent.LEFT,
                    iconName = "AirPodsPro1Left",
                    label = "Left"
                ),
                ComponentSpec(
                    type = DeviceComponent.RIGHT,
                    iconName = "AirPodsPro1Right",
                    label = "Right"
                ),
                ComponentSpec(
                    type = DeviceComponent.CASE,
                    iconName = "AirPodsPro1Case",
                    label = "Charging Case"
                )
            ),
            genericIconName = "AirPodsPro2",
            baseCapabilities = setOf(
                BaseCapability.LISTENING_MODE
            )
        ),
        AirPodsModel.AIRPODS_PRO_2_LIGHTNING to AirPodsSpec(
            modelNumbers = setOf("A2931", "A2699", "A2698"),
            name = "AirPods Pro 2 with Magsafe Charging Case (Lightning)",
            displayName = "AirPods Pro",
            components = setOf(
                ComponentSpec(
                    type = DeviceComponent.LEFT,
                    iconName = "AirPodsPro2Left",
                    label = "Left"
                ),
                ComponentSpec(
                    type = DeviceComponent.RIGHT,
                    iconName = "AirPodsPro2Right",
                    label = "Right"
                ),
                ComponentSpec(
                    type = DeviceComponent.CASE,
                    iconName = "AirPodsPro2Case",
                    label = "Charging Case"
                )
            ),
            genericIconName = "AirPodsPro2",
            baseCapabilities = setOf(
                BaseCapability.LISTENING_MODE,
                BaseCapability.CONVERSATION_AWARENESS,
                BaseCapability.STEM_CONFIG,
                BaseCapability.LOUD_SOUND_REDUCTION,
                BaseCapability.SLEEP_DETECTION,
                BaseCapability.HEARING_AID,
                BaseCapability.ADAPTIVE_AUDIO,
                BaseCapability.ADAPTIVE_VOLUME,
                BaseCapability.SWIPE_FOR_VOLUME,
                BaseCapability.HEAD_GESTURES
            )
        ),
        AirPodsModel.AIRPODS_PRO_2_USBC to AirPodsSpec(
            modelNumbers = setOf("A3047", "A3048", "A3049"),
            name = "AirPods Pro 2 with Magsafe Charging Case (USB-C)",
            displayName = "AirPods Pro",
            components = setOf(
                ComponentSpec(
                    type = DeviceComponent.LEFT,
                    iconName = "AirPodsPro2Left",
                    label = "Left"
                ),
                ComponentSpec(
                    type = DeviceComponent.RIGHT,
                    iconName = "AirPodsPro2Right",
                    label = "Right"
                ),
                ComponentSpec(
                    type = DeviceComponent.CASE,
                    iconName = "AirPodsPro2Case",
                    label = "Charging Case"
                )
            ),
            genericIconName = "AirPodsPro2",
            baseCapabilities = setOf(
                BaseCapability.LISTENING_MODE,
                BaseCapability.CONVERSATION_AWARENESS,
                BaseCapability.STEM_CONFIG,
                BaseCapability.LOUD_SOUND_REDUCTION,
                BaseCapability.SLEEP_DETECTION,
                BaseCapability.HEARING_AID,
                BaseCapability.ADAPTIVE_AUDIO,
                BaseCapability.ADAPTIVE_VOLUME,
                BaseCapability.SWIPE_FOR_VOLUME,
                BaseCapability.HEAD_GESTURES
            )
        ),
        AirPodsModel.AIRPODS_PRO_3 to AirPodsSpec(
            modelNumbers = setOf("A3063", "A3064", "A3065"),
            name = "AirPods Pro 3",
            displayName = "AirPods Pro",
            components = setOf(
                ComponentSpec(
                    type = DeviceComponent.LEFT,
                    iconName = "AirPodsPro3Left",
                    label = "Left"
                ),
                ComponentSpec(
                    type = DeviceComponent.RIGHT,
                    iconName = "AirPodsPro3Right",
                    label = "Right"
                ),
                ComponentSpec(
                    type = DeviceComponent.CASE,
                    iconName = "AirPodsPro3Case",
                    label = "Charging Case"
                )
            ),
            genericIconName = "AirPodsPro3",
            baseCapabilities = setOf(
                BaseCapability.LISTENING_MODE,
                BaseCapability.CONVERSATION_AWARENESS,
                BaseCapability.HEAD_GESTURES,
                BaseCapability.STEM_CONFIG,
                BaseCapability.LOUD_SOUND_REDUCTION,
                BaseCapability.PPE,
                BaseCapability.SLEEP_DETECTION,
                BaseCapability.HEARING_AID,
                BaseCapability.ADAPTIVE_AUDIO,
                BaseCapability.ADAPTIVE_VOLUME,
                BaseCapability.SWIPE_FOR_VOLUME,
                BaseCapability.HRM
            )
        ),
        AirPodsModel.AIRPODS_MAX_LIGHTNING to AirPodsSpec(
            modelNumbers = setOf("A2096"),
            name = "AirPods Max (Lightning)",
            displayName = "AirPods Max",
            components = setOf(
                ComponentSpec(
                    type = DeviceComponent.HEADSET,
                    iconName = "AirPodsMax",
                    label = "Headset"
                )
            ),
            genericIconName = "AirPodsMax",
            baseCapabilities = setOf(
                BaseCapability.LISTENING_MODE,
            )
        ),
        AirPodsModel.AIRPODS_MAX_USBC to AirPodsSpec(
            modelNumbers = setOf("A3184"),
            name = "AirPods Max (USB-C)",
            displayName = "AirPods Max",
            components = setOf(
                ComponentSpec(
                    type = DeviceComponent.HEADSET,
                    iconName = "AirPodsMax",
                    label = "Headset"
                ),
            ),
            genericIconName = "AirPodsMax",
            baseCapabilities = setOf(
                BaseCapability.LISTENING_MODE,
            )
        ),
        AirPodsModel.AIRPODS_MAX_2 to AirPodsSpec(
            modelNumbers = setOf("A3454"),
            name = "AirPods Max 2",
            displayName = "AirPods Max 2",
            components = setOf(
                ComponentSpec(
                    type = DeviceComponent.HEADSET,
                    iconName = "AirPodsMax",
                    label = "Headset"
                ),
            ),
            genericIconName = "AirPodsMax2",
            baseCapabilities = setOf(
                BaseCapability.LISTENING_MODE,
                BaseCapability.CONVERSATION_AWARENESS,
                BaseCapability.LOUD_SOUND_REDUCTION,
                BaseCapability.ADAPTIVE_AUDIO,
                BaseCapability.ADAPTIVE_VOLUME,
            )
        ),
        AirPodsModel.UNKNOWN to AirPodsSpec(
            modelNumbers = emptySet(),
            name = "Unknown AirPods",
            displayName = "Unknown AirPods",
            components = emptySet(),
            genericIconName = "AirPods1",
            baseCapabilities = emptySet()
        )
    )
    fun getSpec(model: AirPodsModel): AirPodsSpec = specs[model] ?: specs[AirPodsModel.UNKNOWN]!!
}

@Serializable
enum class BaseCapability {
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
