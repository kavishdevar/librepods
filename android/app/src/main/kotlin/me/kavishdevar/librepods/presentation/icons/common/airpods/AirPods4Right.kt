package me.kavishdevar.librepods.presentation.icons.common.airpods

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.icons.CommonIcons

val CommonIcons.AirPods4Right: ImageVector
    get() {
        val current = _airPods4Right
        if (current != null) return current

        return ImageVector.Builder(
            name = "AirPods4Right",
            defaultWidth = 33.09400177001953.dp,
            defaultHeight = 55.28099822998047.dp,
            viewportWidth = 33.094f,
            viewportHeight = 55.281f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFFFFFFFF)),
                fillAlpha = 0.85f,
            ) {
                moveTo(x = 12.69f, y = 31.53f)
                curveToRelative(dx1 = 9.28f, dy1 = 0.0f, dx2 = 20.22f, dy2 = -8.66f, dx3 = 20.22f, dy3 = -17.06f)
                curveToRelative(dx1 = 0.0f, dy1 = -8.19f, dx2 = -6.75f, dy2 = -14.16f, dx3 = -13.16f, dy3 = -14.16f)
                curveTo(x1 = 11.47f, y1 = 0.31f, x2 = 0.0f, y2 = 9.03f, x3 = 0.0f, y3 = 16.78f)
                curveToRelative(dx1 = 0.0f, dy1 = 7.31f, dx2 = 4.38f, dy2 = 14.75f, dx3 = 12.69f, dy3 = 14.75f)
                moveTo(x = 7.4f, y = 26.1f)
                curveTo(x1 = 5.34f, y1 = 25.0f, x2 = 3.5f, y2 = 20.06f, x3 = 3.53f, y3 = 16.81f)
                curveToRelative(dx1 = 0.0f, dy1 = -1.18f, dx2 = 0.44f, dy2 = -1.9f, dx3 = 1.16f, dy3 = -1.9f)
                curveToRelative(dx1 = 2.28f, dy1 = 0.0f, dx2 = 4.53f, dy2 = 6.5f, dx3 = 4.53f, dy3 = 9.59f)
                curveToRelative(dx1 = 0.0f, dy1 = 1.22f, dx2 = -0.47f, dy2 = 2.31f, dx3 = -1.81f, dy3 = 1.6f)
                moveToRelative(dx = 7.43f, dy = -8.65f)
                arcToRelative(a = 3.0f, b = 3.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 0.72f, dy1 = -4.22f)
                lineToRelative(dx = 1.75f, dy = -1.25f)
                curveToRelative(dx1 = 1.4f, dy1 = -1.0f, dx2 = 3.22f, dy2 = -0.63f, dx3 = 4.16f, dy3 = 0.72f)
                arcToRelative(a = 2.9f, b = 2.9f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -0.63f, dy1 = 4.19f)
                lineToRelative(dx = -1.75f, dy = 1.25f)
                arcToRelative(a = 3.03f, b = 3.03f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -4.25f, dy1 = -0.7f)
                moveToRelative(dx = 0.25f, dy = 16.9f)
                verticalLineToRelative(dy = 16.1f)
                curveToRelative(dx1 = 0.0f, dy1 = 4.06f, dx2 = 1.75f, dy2 = 4.84f, dx3 = 5.82f, dy3 = 4.84f)
                curveToRelative(dx1 = 4.0f, dy1 = 0.0f, dx2 = 5.72f, dy2 = -0.78f, dx3 = 5.72f, dy3 = -4.84f)
                verticalLineTo(y = 29.69f)
                arcToRelative(a = 26.0f, b = 26.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -11.54f, dy1 = 4.65f)
            }
        }.build().also { _airPods4Right = it }
    }

@Suppress("ObjectPropertyName")
private var _airPods4Right: ImageVector? = null
