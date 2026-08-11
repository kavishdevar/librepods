package me.kavishdevar.librepods.presentation.icons.common.airpods

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.icons.CommonIcons

val CommonIcons.AirPodsPro3Right: ImageVector
    get() {
        val current = _myIcon
        if (current != null) return current

        return ImageVector.Builder(
            name = "AirPodsPro3Right",
            defaultWidth = 42.09400177001953.dp,
            defaultHeight = 61.78300094604492.dp,
            viewportWidth = 42.094f,
            viewportHeight = 61.783f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFFFFFFFF)),
                fillAlpha = 0.85f,
            ) {
                moveTo(x = 28.47f, y = 55.03f)
                curveToRelative(dx1 = 0.0f, dy1 = 2.5f, dx2 = -1.63f, dy2 = 3.78f, dx3 = -4.06f, dy3 = 3.78f)
                horizontalLineToRelative(dx = -2.78f)
                curveToRelative(dx1 = -2.44f, dy1 = 0.0f, dx2 = -4.1f, dy2 = -1.09f, dx3 = -4.1f, dy3 = -3.78f)
                verticalLineTo(y = 37.51f)
                arcToRelative(a = 32.0f, b = 32.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 10.94f, dy1 = -3.23f)
                close()
                moveTo(x = 38.9f, y = 15.66f)
                curveToRelative(dx1 = 0.0f, dy1 = 6.52f, dx2 = -4.93f, dy2 = 12.97f, dx3 = -13.8f, dy3 = 16.25f)
                arcToRelative(a = 15.0f, b = 15.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 1.67f, dy1 = -7.28f)
                curveToRelative(dx1 = 0.0f, dy1 = -7.0f, dx2 = -6.5f, dy2 = -14.85f, dx3 = -15.06f, dy3 = -14.85f)
                quadToRelative(dx1 = -0.15f, dy1 = 0.0f, dx2 = -0.3f, dy2 = 0.02f)
                curveTo(x1 = 16.05f, y1 = 4.86f, x2 = 21.59f, y2 = 2.97f, x3 = 26.09f, y3 = 3.0f)
                curveToRelative(dx1 = 7.2f, dy1 = 0.06f, dx2 = 12.82f, dy2 = 5.38f, dx3 = 12.82f, dy3 = 12.66f)
                moveToRelative(dx = -8.7f, dy = -6.63f)
                lineTo(x = 29.0f, y = 9.88f)
                arcToRelative(a = 2.25f, b = 2.25f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -0.47f, dy1 = 3.25f)
                curveToRelative(dx1 = 0.66f, dy1 = 1.03f, dx2 = 2.1f, dy2 = 1.21f, dx3 = 3.19f, dy3 = 0.46f)
                lineToRelative(dx = 1.19f, dy = -0.84f)
                arcToRelative(a = 2.17f, b = 2.17f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 0.53f, dy1 = -3.12f)
                curveToRelative(dx1 = -0.78f, dy1 = -1.07f, dx2 = -2.13f, dy2 = -1.38f, dx3 = -3.22f, dy3 = -0.6f)
            }
            path(
                fill = SolidColor(Color(0xFFFFFFFF)),
                fillAlpha = 0.425f,
            ) {
                moveTo(x = 14.84f, y = 34.69f)
                curveToRelative(dx1 = 5.13f, dy1 = 0.0f, dx2 = 8.94f, dy2 = -3.25f, dx3 = 8.94f, dy3 = -10.06f)
                curveToRelative(dx1 = 0.0f, dy1 = -5.47f, dx2 = -5.28f, dy2 = -11.82f, dx3 = -12.06f, dy3 = -11.82f)
                curveToRelative(dx1 = -5.38f, dy1 = 0.0f, dx2 = -8.69f, dy2 = 4.85f, dx3 = -8.69f, dy3 = 9.5f)
                curveToRelative(dx1 = 0.0f, dy1 = 6.28f, dx2 = 5.5f, dy2 = 12.38f, dx3 = 11.81f, dy3 = 12.38f)
                moveToRelative(dx = -2.15f, dy = -6.4f)
                curveToRelative(dx1 = -2.19f, dy1 = 0.0f, dx2 = -4.56f, dy2 = -3.2f, dx3 = -4.56f, dy3 = -5.26f)
                curveToRelative(dx1 = 0.0f, dy1 = -1.4f, dx2 = 0.62f, dy2 = -1.84f, dx3 = 1.4f, dy3 = -1.84f)
                curveToRelative(dx1 = 1.97f, dy1 = 0.0f, dx2 = 4.28f, dy2 = 3.19f, dx3 = 4.31f, dy3 = 5.37f)
                curveToRelative(dx1 = 0.04f, dy1 = 1.28f, dx2 = -0.34f, dy2 = 1.72f, dx3 = -1.15f, dy3 = 1.72f)
            }
        }.build().also { _myIcon = it }
    }

@Suppress("ObjectPropertyName")
private var _myIcon: ImageVector? = null
