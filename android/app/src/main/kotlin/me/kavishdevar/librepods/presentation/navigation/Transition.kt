package me.kavishdevar.librepods.presentation.navigation

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf

val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope> {
    error("LocalSharedTransitionScope not provided")
}

val LocalTransitionProgress = compositionLocalOf { 0f }
val LocalIsCurrentEntry = compositionLocalOf { false }
