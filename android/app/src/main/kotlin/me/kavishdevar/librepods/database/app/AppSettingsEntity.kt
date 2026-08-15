package me.kavishdevar.librepods.database.app

import android.bluetooth.le.ScanSettings
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.NightTheme

@Entity
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 0,

    val nightMode: NightTheme = NightTheme.System,
    val designSystem: DesignSystem = DesignSystem.Material,
    val useHighestRefreshRate: Boolean = false,

    val debugMode: Boolean = false,

    val bleScanMode: Int = ScanSettings.SCAN_MODE_BALANCED,
    val bleReportDelay: Long = 0,

    val swipeAnywhereForBack: Boolean = true,
)
