package me.kavishdevar.librepods.database.app

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert

@Dao
interface AppSettingsDao {

    @Query("SELECT * FROM AppSettingsEntity WHERE id = 0")
    suspend fun get(): AppSettingsEntity?

    @Upsert
    suspend fun upsert(settings: AppSettingsEntity)
}
