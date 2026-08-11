package me.kavishdevar.librepods.database.app

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert

@Dao
interface AppStateDao {

    @Query("SELECT * FROM AppStateEntity WHERE id = 0")
    suspend fun get(): AppStateEntity?

    @Upsert
    suspend fun upsert(state: AppStateEntity)
}
