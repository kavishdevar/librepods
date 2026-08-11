package me.kavishdevar.librepods.database.widget

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert

@Dao
interface WidgetConfigDao {

    @Query("SELECT * FROM WidgetConfigEntity")
    suspend fun getAll(): List<WidgetConfigEntity>

    @Query("SELECT * FROM WidgetConfigEntity WHERE appWidgetId = :appWidgetId")
    suspend fun getWidgetById(appWidgetId: Int): WidgetConfigEntity?

    @Upsert
    suspend fun upsert(widgetConfig: WidgetConfigEntity)
}
