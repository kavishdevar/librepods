package me.kavishdevar.librepods.presentation.icons.common.airpods

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.icons.CommonIcons

val CommonIcons.AirPods4Left: ImageVector
    get() {
        val current = _airPods4Left
        if (current != null) return current

        return ImageVector.Builder(
            name = ".MyIcon",
            defaultWidth = 33.09400177001953.dp,
            defaultHeight = 55.28099822998047.dp,
            viewportWidth = 33.094f,
            viewportHeight = 55.281f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFFFFFFFF)),
                fillAlpha = 0.85f,
            ) {
                moveTo(x = 20.22f, y = 31.53f)
                curveToRelative(dx1 = 8.31f, dy1 = 0.0f, dx2 = 12.69f, dy2 = -7.44f, dx3 = 12.69f, dy3 = -14.75f)
                curveToRelative(dx1 = 0.0f, dy1 = -7.75f, dx2 = -11.47f, dy2 = -16.47f, dx3 = -19.75f, dy3 = -16.47f)
                curveTo(x1 = 6.75f, y1 = 0.31f, x2 = 0.0f, y2 = 6.28f, x3 = 0.0f, y3 = 14.47f)
                curveToRelative(dx1 = 0.0f, dy1 = 8.4f, dx2 = 10.9f, dy2 = 17.06f, dx3 = 20.22f, dy3 = 17.06f)
                moveToRelative(dx = 5.28f, dy = -5.44f)
                curveToRelative(dx1 = -1.34f, dy1 = 0.72f, dx2 = -1.81f, dy2 = -0.37f, dx3 = -1.81f, dy3 = -1.59f)
                curveToRelative(dx1 = 0.0f, dy1 = -3.1f, dx2 = 2.25f, dy2 = -9.6f, dx3 = 4.53f, dy3 = -9.6f)
                curveToRelative(dx1 = 0.72f, dy1 = 0.0f, dx2 = 1.12f, dy2 = 0.72f, dx3 = 1.12f, dy3 = 1.91f)
                curveToRelative(dx1 = 0.07f, dy1 = 3.25f, dx2 = -1.78f, dy2 = 8.19f, dx3 = -3.84f, dy3 = 9.28f)
                moveToRelative(dx = -7.44f, dy = -8.65f)
                arcToRelative(a = 3.03f, b = 3.03f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -4.25f, dy1 = 0.68f)
                lineToRelative(dx = -1.75f, dy = -1.25f)
                arcToRelative(a = 2.87f, b = 2.87f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -0.62f, dy1 = -4.18f)
                arcToRelative(a = 2.93f, b = 2.93f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 4.15f, dy1 = -0.72f)
                lineToRelative(dx = 1.75f, dy = 1.25f)
                arcToRelative(a = 3.0f, b = 3.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 0.72f, dy1 = 4.22f)
                moveToRelative(dx = -0.25f, dy = 16.9f)
                curveToRelative(dx1 = -4.0f, dy1 = -0.5f, dx2 = -8.06f, dy2 = -2.18f, dx3 = -11.53f, dy3 = -4.65f)
                verticalLineToRelative(dy = 20.75f)
                curveToRelative(dx1 = 0.0f, dy1 = 4.06f, dx2 = 1.72f, dy2 = 4.84f, dx3 = 5.72f, dy3 = 4.84f)
                curveToRelative(dx1 = 4.06f, dy1 = 0.0f, dx2 = 5.81f, dy2 = -0.78f, dx3 = 5.81f, dy3 = -4.84f)
                close()
            }
        }.build().also { _airPods4Left = it }
    }

@Suppress("ObjectPropertyName")
private var _airPods4Left: ImageVector? = null

