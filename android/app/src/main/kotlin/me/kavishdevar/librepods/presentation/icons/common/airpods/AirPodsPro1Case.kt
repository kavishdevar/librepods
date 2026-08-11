package me.kavishdevar.librepods.presentation.icons.common.airpods

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.icons.CommonIcons

val CommonIcons.AirPodsPro1Case: ImageVector
    get() {
        val current = _airPodsPro1Case
        if (current != null) return current

        return ImageVector.Builder(
            name = "AirPodsPro1Case",
            defaultWidth = 70.93800354003906.dp,
            defaultHeight = 54.53099822998047.dp,
            viewportWidth = 70.938f,
            viewportHeight = 54.531f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFFFFFFFF)),
                fillAlpha = 0.85f,
            ) {
                moveTo(x = 18.94f, y = 54.5f)
                horizontalLineTo(x = 51.8f)
                curveToRelative(dx1 = 12.78f, dy1 = 0.0f, dx2 = 18.94f, dy2 = -6.12f, dx3 = 18.94f, dy3 = -18.84f)
                verticalLineTo(y = 18.84f)
                curveTo(x1 = 70.75f, y1 = 6.13f, x2 = 64.59f, y2 = 0.0f, x3 = 51.81f, y3 = 0.0f)
                horizontalLineTo(x = 18.94f)
                curveTo(x1 = 6.19f, y1 = 0.0f, x2 = 0.0f, y2 = 6.13f, x3 = 0.0f, y3 = 18.84f)
                verticalLineToRelative(dy = 16.82f)
                curveTo(x1 = 0.0f, y1 = 48.38f, x2 = 6.19f, y2 = 54.5f, x3 = 18.94f, y3 = 54.5f)
                moveToRelative(dx = 0.0f, dy = -5.03f)
                curveToRelative(dx1 = -9.6f, dy1 = 0.0f, dx2 = -13.9f, dy2 = -4.28f, dx3 = -13.9f, dy3 = -13.81f)
                verticalLineTo(y = 18.84f)
                curveToRelative(dx1 = 0.0f, dy1 = -9.53f, dx2 = 4.3f, dy2 = -13.8f, dx3 = 13.9f, dy3 = -13.8f)
                horizontalLineTo(x = 51.8f)
                curveToRelative(dx1 = 9.63f, dy1 = 0.0f, dx2 = 13.9f, dy2 = 4.27f, dx3 = 13.9f, dy3 = 13.8f)
                verticalLineToRelative(dy = 16.82f)
                curveToRelative(dx1 = 0.0f, dy1 = 9.53f, dx2 = -4.27f, dy2 = 13.8f, dx3 = -13.9f, dy3 = 13.8f)
                close()
                moveToRelative(dx = -16.4f, dy = -28.5f)
                horizontalLineToRelative(dx = 65.68f)
                verticalLineTo(y = 17.8f)
                horizontalLineTo(x = 2.53f)
                close()
                moveToRelative(dx = 20.34f, dy = 2.16f)
                horizontalLineTo(x = 47.9f)
                arcToRelative(a = 3.6f, b = 3.6f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 3.75f, dy1 = -3.75f)
                arcToRelative(a = 3.6f, b = 3.6f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -3.75f, dy1 = -3.72f)
                horizontalLineTo(x = 22.88f)
                arcToRelative(a = 3.65f, b = 3.65f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -3.79f, dy1 = 3.72f)
                arcToRelative(a = 3.65f, b = 3.65f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 3.79f, dy1 = 3.75f)
                moveToRelative(dx = 12.5f, dy = 10.65f)
                arcToRelative(a = 3.1f, b = 3.1f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 3.03f, dy1 = -2.97f)
                arcToRelative(a = 3.05f, b = 3.05f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -3.03f, dy1 = -3.03f)
                curveToRelative(dx1 = -1.66f, dy1 = 0.0f, dx2 = -3.0f, dy2 = 1.38f, dx3 = -3.0f, dy3 = 3.03f)
                curveToRelative(dx1 = 0.0f, dy1 = 1.63f, dx2 = 1.34f, dy2 = 3.0f, dx3 = 3.0f, dy3 = 2.97f)
            }
        }.build().also { _airPodsPro1Case = it }
    }

@Suppress("ObjectPropertyName")
private var _airPodsPro1Case: ImageVector? = null
