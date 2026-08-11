package me.kavishdevar.librepods.database.widget

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import me.kavishdevar.librepods.bluetooth.MacAddress

@Entity
data class WidgetConfigEntity(
    @PrimaryKey
    val appWidgetId: Int,

    val macAddress: MacAddress
)

