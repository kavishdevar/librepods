package me.kavishdevar.librepods.database

import androidx.room3.AutoMigration
import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import me.kavishdevar.librepods.database.app.AppSettingsDao
import me.kavishdevar.librepods.database.app.AppSettingsEntity
import me.kavishdevar.librepods.database.app.AppStateDao
import me.kavishdevar.librepods.database.app.AppStateEntity
import me.kavishdevar.librepods.database.apple.AppleDao
import me.kavishdevar.librepods.database.apple.AppleEntity
import me.kavishdevar.librepods.database.heartrate.HeartRateDao
import me.kavishdevar.librepods.database.heartrate.HeartRateSampleEntity
import me.kavishdevar.librepods.database.widget.WidgetConfigDao
import me.kavishdevar.librepods.database.widget.WidgetConfigEntity

@ColumnTypeConverters(Converters::class)
@Database(
    entities = [
        AppleEntity::class,
        AppSettingsEntity::class,
        AppStateEntity::class,
        WidgetConfigEntity::class,
        HeartRateSampleEntity::class
    ],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
    ]
)
abstract class LibrePodsDatabase: RoomDatabase() {
    abstract fun appleDao(): AppleDao

    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun appStateDao(): AppStateDao

    abstract fun widgetConfigDao(): WidgetConfigDao

    abstract fun heartRateDao(): HeartRateDao
}
