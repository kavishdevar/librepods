package me.kavishdevar.librepods.presentation.theme

import androidx.compose.runtime.compositionLocalOf
import kotlinx.serialization.Serializable

val LocalDesignSystem = compositionLocalOf {
    DesignSystem.Material
}
