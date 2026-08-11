/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package me.kavishdevar.librepods.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import me.kavishdevar.librepods.presentation.icons.AppleIcons
import me.kavishdevar.librepods.presentation.icons.LocalIcons
import me.kavishdevar.librepods.presentation.icons.MaterialIcons

val ColorScheme.sectionHeader: Color
    get() = onBackground.copy(alpha = 0.6f)

private val AppleDarkColorScheme = darkColorScheme(
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    surfaceDim = Color(0x40888888),
    primary = Color(0xFF0091FF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF003258),
    onPrimaryContainer = Color(0xFFB3D9FF),
    secondaryContainer = Color(0xFF366AA8),
    onSecondaryContainer = Color(0xFFB3D9FF),
    tertiary = Color(0xFFEA7B00),
    scrim = Color(0x8C000000),
    surfaceContainerHigh = Color(0xFF1C1C1E),
    surfaceContainerLow = Color(0xFF2C2C2E)
)

private val AppleLightColorScheme = lightColorScheme(
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF000000),
    surfaceContainer = Color(0xFFF2F2F7),
    onSurface = Color(0xFF000000),
    surfaceDim = Color(0x40D9D9D9),
    primary = Color(0xFF0088FF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB3D9FF),
    onPrimaryContainer = Color(0xFF003258),
    secondaryContainer = Color(0xFF6BC0FF),
    onSecondaryContainer = Color(0xFF003258),
    tertiary = Color(0xFFEA7B00),
    scrim = Color(0xD9F2F2F7),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFE7E7E7)
)

@Composable
fun LibrePodsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    designSystem: DesignSystem = DesignSystem.Material,
    content: @Composable () -> Unit
) {
    val colorScheme = when(designSystem) {
        DesignSystem.Material -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        DesignSystem.Apple -> if (darkTheme) AppleDarkColorScheme else AppleLightColorScheme
    }

    val typography = when(designSystem) {
        DesignSystem.Material -> MaterialTypography
        DesignSystem.Apple -> AppleTypography
    }

    CompositionLocalProvider(
        LocalDesignSystem provides designSystem,
        LocalIcons provides when (designSystem) {
            DesignSystem.Material -> MaterialIcons
            DesignSystem.Apple -> AppleIcons
        }
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = typography,
            content = content
        )
    }
}
