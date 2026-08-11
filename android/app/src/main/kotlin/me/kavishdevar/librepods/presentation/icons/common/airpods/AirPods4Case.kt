package me.kavishdevar.librepods.presentation.icons.common.airpods

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.icons.CommonIcons

val CommonIcons.AirPods4Case: ImageVector
    get() {
        val current = _airPods4Case
        if (current != null) return current

        return ImageVector.Builder(
            name = "AirPods4Case",
            defaultWidth = 59.375.dp,
            defaultHeight = 54.53099822998047.dp,
            viewportWidth = 59.375f,
            viewportHeight = 54.531f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFFFFFFFF)),
                fillAlpha = 0.85f,
            ) {
                moveTo(x = 19.13f, y = 54.5f)
                horizontalLineToRelative(dx = 20.93f)
                curveToRelative(dx1 = 12.78f, dy1 = 0.0f, dx2 = 19.13f, dy2 = -6.31f, dx3 = 19.13f, dy3 = -19.03f)
                verticalLineTo(y = 19.03f)
                curveTo(x1 = 59.19f, y1 = 6.31f, x2 = 52.84f, y2 = 0.0f, x3 = 40.06f, y3 = 0.0f)
                horizontalLineTo(x = 19.13f)
                curveTo(x1 = 6.34f, y1 = 0.0f, x2 = 0.0f, y2 = 6.31f, x3 = 0.0f, y3 = 19.03f)
                verticalLineToRelative(dy = 16.44f)
                curveTo(x1 = 0.0f, y1 = 48.19f, x2 = 6.34f, y2 = 54.5f, x3 = 19.13f, y3 = 54.5f)
                moveToRelative(dx = 0.0f, dy = -5.03f)
                curveToRelative(dx1 = -9.63f, dy1 = 0.0f, dx2 = -14.1f, dy2 = -4.44f, dx3 = -14.1f, dy3 = -14.0f)
                verticalLineTo(y = 19.03f)
                curveToRelative(dx1 = 0.0f, dy1 = -9.56f, dx2 = 4.47f, dy2 = -14.0f, dx3 = 14.1f, dy3 = -14.0f)
                horizontalLineToRelative(dx = 20.93f)
                curveToRelative(dx1 = 9.63f, dy1 = 0.0f, dx2 = 14.1f, dy2 = 4.44f, dx3 = 14.1f, dy3 = 14.0f)
                verticalLineToRelative(dy = 16.44f)
                curveToRelative(dx1 = 0.0f, dy1 = 9.56f, dx2 = -4.47f, dy2 = 14.0f, dx3 = -14.1f, dy3 = 14.0f)
                close()
                moveTo(x = 1.93f, y = 18.25f)
                horizontalLineToRelative(dx = 54.41f)
                verticalLineToRelative(dy = -3.16f)
                horizontalLineTo(x = 1.94f)
                close()
                moveToRelative(dx = 18.04f, dy = 2.16f)
                horizontalLineToRelative(dx = 19.1f)
                arcToRelative(a = 3.6f, b = 3.6f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 3.74f, dy1 = -3.72f)
                arcToRelative(a = 3.6f, b = 3.6f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -3.75f, dy1 = -3.72f)
                horizontalLineToRelative(dx = -19.1f)
                arcToRelative(a = 3.6f, b = 3.6f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -3.74f, dy1 = 3.72f)
                arcToRelative(a = 3.6f, b = 3.6f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 3.75f, dy1 = 3.72f)
                moveToRelative(dx = 9.62f, dy = 10.68f)
                curveToRelative(dx1 = 1.66f, dy1 = -0.06f, dx2 = 3.04f, dy2 = -1.43f, dx3 = 3.04f, dy3 = -3.0f)
                arcToRelative(a = 3.03f, b = 3.03f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -6.07f, dy1 = 0.0f)
                arcToRelative(a = 3.03f, b = 3.03f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 3.03f, dy1 = 3.0f)
            }
        }.build().also { _airPods4Case = it }
    }

@Suppress("ObjectPropertyName")
private var _airPods4Case: ImageVector? = null
