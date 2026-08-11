package me.kavishdevar.librepods.devices

import kotlinx.serialization.Serializable

@Serializable
data class AppleMetadata(
    override val name: String = "",

    val model: AirPodsModel = AirPodsModel.UNKNOWN,
    val modelName: String = "",
    val modelNumber: String = "",
    val manufacturer: String = "",

    val serialNumber: String = "",
    val leftSerialNumber: String = "",
    val rightSerialNumber: String = "",

    val version1: String = "",
    val version2: String = "",
    val version3: String = "",

    val hardwareRevision: String = "",
    val updaterIdentifier: String = "",
): DeviceMetadata {
    override val iconName: String
        get() = AirPodsSpecs.getSpec(model).genericIconName
}
