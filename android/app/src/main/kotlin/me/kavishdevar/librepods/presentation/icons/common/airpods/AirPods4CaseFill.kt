package me.kavishdevar.librepods.presentation.icons.common.airpods

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.icons.CommonIcons

val CommonIcons.AirPods4CaseFill: ImageVector
    get() {
        val current = _airPods4CaseFill
        if (current != null) return current

        return ImageVector.Builder(
            name = "AirPods4CaseFill",
            defaultWidth = 59.375.dp,
            defaultHeight = 54.53099822998047.dp,
            viewportWidth = 59.375f,
            viewportHeight = 54.531f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFFFFFFFF)),
                fillAlpha = 0.85f,
            ) {
                moveTo(x = 0.22f, y = 15.22f)
                horizontalLineToRelative(dx = 15.87f)
                curveToRelative(dx1 = 0.5f, dy1 = -1.4f, dx2 = 1.85f, dy2 = -2.28f, dx3 = 3.47f, dy3 = -2.28f)
                horizontalLineToRelative(dx = 19.97f)
                curveToRelative(dx1 = 1.66f, dy1 = 0.0f, dx2 = 3.0f, dy2 = 0.87f, dx3 = 3.5f, dy3 = 2.28f)
                horizontalLineTo(x = 59.0f)
                curveTo(x1 = 57.81f, y1 = 5.03f, x2 = 51.5f, y2 = 0.0f, x3 = 40.1f, y3 = 0.0f)
                horizontalLineTo(x = 19.12f)
                curveTo(x1 = 7.69f, y1 = 0.0f, x2 = 1.4f, y2 = 5.03f, x3 = 0.21f, y3 = 15.22f)
                moveToRelative(dx = 39.34f, dy = 5.15f)
                horizontalLineTo(x = 19.62f)
                arcToRelative(a = 3.6f, b = 3.6f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -3.5f, dy1 = -2.3f)
                horizontalLineTo(x = 0.0f)
                verticalLineToRelative(dy = 17.4f)
                curveTo(x1 = 0.0f, y1 = 48.19f, x2 = 6.34f, y2 = 54.5f, x3 = 19.13f, y3 = 54.5f)
                horizontalLineToRelative(dx = 20.93f)
                curveToRelative(dx1 = 12.78f, dy1 = 0.0f, dx2 = 19.13f, dy2 = -6.31f, dx3 = 19.13f, dy3 = -19.03f)
                verticalLineToRelative(dy = -17.4f)
                horizontalLineToRelative(dx = -16.1f)
                arcToRelative(a = 3.6f, b = 3.6f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -3.53f, dy1 = 2.3f)
                moveToRelative(dx = -9.97f, dy = 11.7f)
                arcToRelative(a = 2.9f, b = 2.9f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2.87f, dy1 = -2.85f)
                arcToRelative(a = 2.88f, b = 2.88f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 5.75f, dy1 = 0.0f)
                curveToRelative(dx1 = 0.0f, dy1 = 1.47f, dx2 = -1.31f, dy2 = 2.78f, dx3 = -2.88f, dy3 = 2.84f)
            }
        }.build().also { _airPods4CaseFill = it }
    }

@Suppress("ObjectPropertyName")
private var _airPods4CaseFill: ImageVector? = null
