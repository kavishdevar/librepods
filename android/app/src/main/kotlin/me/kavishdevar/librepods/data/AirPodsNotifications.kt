package me.kavishdevar.librepods.data

enum class AirPodsNotifications(val action: String) {
    AIRPODS_CONNECTED("me.kavishdevar.librepods.AIRPODS_CONNECTED"),
    ANC_DATA("me.kavishdevar.librepods.ANC_DATA"),
    AIRPODS_DISCONNECTED("me.kavishdevar.librepods.AIRPODS_DISCONNECTED"),
}
