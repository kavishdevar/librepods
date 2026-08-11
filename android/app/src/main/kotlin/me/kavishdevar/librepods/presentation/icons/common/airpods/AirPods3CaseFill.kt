package me.kavishdevar.librepods.presentation.icons.common.airpods

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.icons.CommonIcons

val CommonIcons.AirPods3CaseFill: ImageVector
    get() {
        val current = _airPods3CaseFill
        if (current != null) return current

        return ImageVector.Builder(
            name = "AirPods3CaseFill",
            defaultWidth = 64.56300354003906.dp,
            defaultHeight = 55.15599822998047.dp,
            viewportWidth = 64.563f,
            viewportHeight = 55.156f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFFFFFFFF)),
                fillAlpha = 0.85f,
            ) {
                moveTo(x = 18.94f, y = 55.13f)
                horizontalLineToRelative(dx = 26.5f)
                curveToRelative(dx1 = 12.75f, dy1 = 0.0f, dx2 = 18.94f, dy2 = -6.16f, dx3 = 18.94f, dy3 = -18.88f)
                verticalLineTo(y = 20.81f)
                horizontalLineTo(x = 48.22f)
                arcToRelative(a = 3.6f, b = 3.6f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -3.5f, dy1 = 2.32f)
                horizontalLineTo(x = 19.69f)
                curveToRelative(dx1 = -1.63f, dy1 = 0.0f, dx2 = -3.0f, dy2 = -0.91f, dx3 = -3.5f, dy3 = -2.32f)
                horizontalLineTo(x = 0.0f)
                verticalLineToRelative(dy = 15.44f)
                curveToRelative(dx1 = 0.0f, dy1 = 12.72f, dx2 = 6.19f, dy2 = 18.88f, dx3 = 18.94f, dy3 = 18.88f)
                moveToRelative(dx = 13.25f, dy = -20.1f)
                arcToRelative(a = 2.83f, b = 2.83f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2.88f, dy1 = -2.81f)
                arcToRelative(a = 2.9f, b = 2.9f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2.88f, dy1 = -2.88f)
                arcToRelative(a = 2.9f, b = 2.9f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2.87f, dy1 = 2.88f)
                arcToRelative(a = 2.94f, b = 2.94f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2.87f, dy1 = 2.81f)
                moveTo(x = 0.0f, y = 17.97f)
                horizontalLineToRelative(dx = 16.13f)
                arcToRelative(a = 3.6f, b = 3.6f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 3.5f, dy1 = -2.31f)
                horizontalLineToRelative(dx = 25.06f)
                curveToRelative(dx1 = 1.62f, dy1 = 0.0f, dx2 = 2.97f, dy2 = 0.9f, dx3 = 3.47f, dy3 = 2.3f)
                horizontalLineToRelative(dx = 16.12f)
                verticalLineToRelative(dy = -1.0f)
                curveTo(x1 = 64.28f, y1 = 5.57f, x2 = 57.5f, y2 = 0.0f, x3 = 45.34f, y3 = 0.0f)
                horizontalLineToRelative(dx = -26.4f)
                curveTo(x1 = 6.78f, y1 = 0.0f, x2 = 0.0f, y2 = 5.56f, x3 = 0.0f, y3 = 16.97f)
                close()
            }
        }.build().also { _airPods3CaseFill = it }
    }

@Suppress("ObjectPropertyName")
private var _airPods3CaseFill: ImageVector? = null
