package me.kavishdevar.librepods.utils

object BluetoothMetadata {
    /**
     * Device type which is used in METADATA_DEVICE_TYPE
     * Indicates this Bluetooth device is an untethered headset.
     * @hide
     */
    const val DEVICE_TYPE_UNTETHERED_HEADSET: String = "Untethered Headset"

    /**
     * Maximum length of a metadata entry, this is to avoid exploding Bluetooth
     * disk usage
     * @hide
     */
    const val METADATA_MAX_LENGTH: Int = 2048

    /**
     * Manufacturer name of this Bluetooth device
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    const val METADATA_MANUFACTURER_NAME: Int = 0

    /**
     * Model name of this Bluetooth device
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    const val METADATA_MODEL_NAME: Int = 1

    /**
     * Software version of this Bluetooth device
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    const val METADATA_SOFTWARE_VERSION: Int = 2

    /**
     * Hardware version of this Bluetooth device
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    const val METADATA_HARDWARE_VERSION: Int = 3

    /**
     * Package name of the companion app, if any
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    const val METADATA_COMPANION_APP: Int = 4

    /**
     * URI to the main icon shown on the settings UI
     * Data type should be [Byte] array.
     * @hide
     */
    const val METADATA_MAIN_ICON: Int = 5

    /**
     * Whether this device is an untethered headset with left, right and case
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    const val METADATA_IS_UNTETHERED_HEADSET: Int = 6

    /**
     * URI to icon of the left headset
     * Data type should be [Byte] array.
     * @hide
     */
    const val METADATA_UNTETHERED_LEFT_ICON: Int = 7

    /**
     * URI to icon of the right headset
     * Data type should be [Byte] array.
     * @hide
     */
    const val METADATA_UNTETHERED_RIGHT_ICON: Int = 8

    /**
     * URI to icon of the headset charging case
     * Data type should be [Byte] array.
     * @hide
     */
    const val METADATA_UNTETHERED_CASE_ICON: Int = 9

    /**
     * Battery level of left headset
     * Data type should be {@String} 0-100 as [Byte] array, otherwise
     * as invalid.
     * @hide
     */
    const val METADATA_UNTETHERED_LEFT_BATTERY: Int = 10

    /**
     * Battery level of rigth headset
     * Data type should be {@String} 0-100 as [Byte] array, otherwise
     * as invalid.
     * @hide
     */
    const val METADATA_UNTETHERED_RIGHT_BATTERY: Int = 11

    /**
     * Battery level of the headset charging case
     * Data type should be {@String} 0-100 as [Byte] array, otherwise
     * as invalid.
     * @hide
     */
    const val METADATA_UNTETHERED_CASE_BATTERY: Int = 12

    /**
     * Whether the left headset is charging
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    const val METADATA_UNTETHERED_LEFT_CHARGING: Int = 13

    /**
     * Whether the right headset is charging
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    const val METADATA_UNTETHERED_RIGHT_CHARGING: Int = 14

    /**
     * Whether the headset charging case is charging
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    const val METADATA_UNTETHERED_CASE_CHARGING: Int = 15

    /**
     * URI to the enhanced settings UI slice
     * Data type should be {@String} as [Byte] array, null means
     * the UI does not exist.
     * @hide
     */
    const val METADATA_ENHANCED_SETTINGS_UI_URI: Int = 16

    /**
     * @hide
     */
    const val COMPANION_TYPE_PRIMARY: String = "COMPANION_PRIMARY"

    /**
     * @hide
     */
    const val COMPANION_TYPE_SECONDARY: String = "COMPANION_SECONDARY"

    /**
     * @hide
     */
    const val COMPANION_TYPE_NONE: String = "COMPANION_NONE"

    /**
     * Type of the Bluetooth device, must be within the list of
     * DEVICE_TYPE_*
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    const val METADATA_DEVICE_TYPE: Int = 17

    /**
     * Battery level of the Bluetooth device, use when the Bluetooth device
     * does not support HFP battery indicator.
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    const val METADATA_MAIN_BATTERY: Int = 18

    /**
     * Whether the device is charging.
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    const val METADATA_MAIN_CHARGING: Int = 19

    /**
     * The battery threshold of the Bluetooth device to show low battery icon.
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    const val METADATA_MAIN_LOW_BATTERY_THRESHOLD: Int = 20

    /**
     * The battery threshold of the left headset to show low battery icon.
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    const val METADATA_UNTETHERED_LEFT_LOW_BATTERY_THRESHOLD: Int = 21

    /**
     * The battery threshold of the right headset to show low battery icon.
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    const val METADATA_UNTETHERED_RIGHT_LOW_BATTERY_THRESHOLD: Int = 22

    /**
     * The battery threshold of the case to show low battery icon.
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    const val METADATA_UNTETHERED_CASE_LOW_BATTERY_THRESHOLD: Int = 23


    /**
     * The metadata of the audio spatial data.
     * Data type should be [Byte] array.
     * @hide
     */
    const val METADATA_SPATIAL_AUDIO: Int = 24

    /**
     * The metadata of the Fast Pair for any custmized feature.
     * Data type should be [Byte] array.
     * @hide
     */
    const val METADATA_FAST_PAIR_CUSTOMIZED_FIELDS: Int = 25

    /**
     * The metadata of the Fast Pair for LE Audio capable devices.
     * Data type should be [Byte] array.
     * @hide
     */
    const val METADATA_LE_AUDIO: Int = 26

    /**
     * The UUIDs (16-bit) of registered to CCC characteristics from Media Control services.
     * Data type should be [Byte] array.
     * @hide
     */
    const val METADATA_GMCS_CCCD: Int = 27

    /**
     * The UUIDs (16-bit) of registered to CCC characteristics from Telephony Bearer service.
     * Data type should be [Byte] array.
     * @hide
     */
    const val METADATA_GTBS_CCCD: Int = 28

    const val BATTERY_LEVEL_UNKNOWN: Int = -1

    const val ACTION_BLUETOOTH_HANDSFREE_BATTERY_CHANGED =
        "android.intent.action.BLUETOOTH_HANDSFREE_BATTERY_CHANGED"
    const val EXTRA_SHOW_BT_HANDSFREE_BATTERY =
        "android.intent.extra.show_bluetooth_handsfree_battery"
    const val EXTRA_BT_HANDSFREE_BATTERY_LEVEL =
        "android.intent.extra.bluetooth_handsfree_battery_level"

}
