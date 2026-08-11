package me.kavishdevar.librepods.database.app

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity
data class AppStateEntity(
    @PrimaryKey
    val id: Int = 0,

    val hasCompletedOnboarding: Boolean = false,
    val lastVersionShown: String? = null,

    val hasConnectedToAACP: Boolean = false,
    val firstSuccessfulConnectionTime: Long? = null,

    val reviewPrompted: Boolean = false,

    val timeUntilFOSSPremiumExpiry: Long = 0L,
)
