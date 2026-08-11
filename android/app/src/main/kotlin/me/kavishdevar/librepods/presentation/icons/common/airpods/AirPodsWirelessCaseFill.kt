package me.kavishdevar.librepods.presentation.icons.common.airpods

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.icons.CommonIcons

val CommonIcons.AirPodsWirelessCaseFill: ImageVector
    get() {
        val current = _airPodsWirelessCaseFill
        if (current != null) return current

        return ImageVector.Builder(
            name = ".MyIcon",
            defaultWidth = 51.0.dp,
            defaultHeight = 62.09400177001953.dp,
            viewportWidth = 51.0f,
            viewportHeight = 62.094f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFFFFFFFF)),
                fillAlpha = 0.85f,
            ) {
                moveTo(x = 25.4f, y = 35.69f)
                arcToRelative(a = 2.9f, b = 2.9f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2.87f, dy1 = -2.85f)
                arcToRelative(a = 2.9f, b = 2.9f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2.88f, dy1 = -2.87f)
                arcToRelative(a = 2.9f, b = 2.9f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2.87f, dy1 = 2.87f)
                arcToRelative(a = 2.97f, b = 2.97f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2.87f, dy1 = 2.85f)
                moveToRelative(dx = -9.65f, dy = 26.37f)
                horizontalLineToRelative(dx = 19.31f)
                curveToRelative(dx1 = 10.63f, dy1 = 0.0f, dx2 = 15.75f, dy2 = -5.12f, dx3 = 15.75f, dy3 = -15.75f)
                verticalLineToRelative(dy = -25.5f)
                horizontalLineTo(x = 37.6f)
                arcToRelative(a = 3.8f, b = 3.8f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -3.53f, dy1 = 2.13f)
                horizontalLineTo(x = 16.75f)
                arcToRelative(a = 3.8f, b = 3.8f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -3.5f, dy1 = -2.13f)
                horizontalLineTo(x = 0.0f)
                verticalLineToRelative(dy = 25.5f)
                curveToRelative(dx1 = 0.0f, dy1 = 10.63f, dx2 = 5.13f, dy2 = 15.75f, dx3 = 15.75f, dy3 = 15.75f)
                moveTo(x = 0.0f, y = 17.66f)
                horizontalLineToRelative(dx = 13.25f)
                arcToRelative(a = 3.8f, b = 3.8f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 3.5f, dy1 = -2.16f)
                horizontalLineToRelative(dx = 17.31f)
                arcToRelative(a = 3.7f, b = 3.7f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 3.53f, dy1 = 2.16f)
                horizontalLineToRelative(dx = 13.22f)
                verticalLineToRelative(dy = -1.91f)
                curveTo(x1 = 50.81f, y1 = 5.13f, x2 = 45.7f, y2 = 0.0f, x3 = 35.06f, y3 = 0.0f)
                horizontalLineTo(x = 15.75f)
                curveTo(x1 = 5.13f, y1 = 0.0f, x2 = 0.0f, y2 = 5.13f, x3 = 0.0f, y3 = 15.75f)
                close()
            }
        }.build().also { _airPodsWirelessCaseFill = it }
    }

@Suppress("ObjectPropertyName")
private var _airPodsWirelessCaseFill: ImageVector? = null
