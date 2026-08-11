package me.kavishdevar.librepods.presentation.icons.common.airpods

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.icons.CommonIcons

val CommonIcons.AirPodsPro1CaseFill: ImageVector
    get() {
        val current = _airPodsPro1CaseFill
        if (current != null) return current

        return ImageVector.Builder(
            name = "AirPodsPro1CaseFill",
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
                verticalLineTo(y = 20.8f)
                horizontalLineTo(x = 51.41f)
                arcToRelative(a = 3.6f, b = 3.6f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -3.5f, dy1 = 2.32f)
                horizontalLineTo(x = 22.88f)
                curveToRelative(dx1 = -1.63f, dy1 = 0.0f, dx2 = -3.0f, dy2 = -0.91f, dx3 = -3.5f, dy3 = -2.32f)
                horizontalLineTo(x = 0.0f)
                verticalLineToRelative(dy = 14.85f)
                curveTo(x1 = 0.0f, y1 = 48.38f, x2 = 6.19f, y2 = 54.5f, x3 = 18.94f, y3 = 54.5f)
                moveToRelative(dx = 16.43f, dy = -19.47f)
                arcToRelative(a = 2.83f, b = 2.83f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2.84f, dy1 = -2.81f)
                arcToRelative(a = 2.9f, b = 2.9f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2.84f, dy1 = -2.88f)
                arcToRelative(a = 2.9f, b = 2.9f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2.88f, dy1 = 2.88f)
                arcToRelative(a = 2.94f, b = 2.94f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2.87f, dy1 = 2.81f)
                moveTo(x = 0.0f, y = 17.97f)
                horizontalLineToRelative(dx = 19.31f)
                curveToRelative(dx1 = 0.5f, dy1 = -1.44f, dx2 = 1.88f, dy2 = -2.31f, dx3 = 3.5f, dy3 = -2.31f)
                horizontalLineToRelative(dx = 25.07f)
                curveToRelative(dx1 = 1.62f, dy1 = 0.0f, dx2 = 2.96f, dy2 = 0.87f, dx3 = 3.46f, dy3 = 2.3f)
                horizontalLineToRelative(dx = 19.32f)
                verticalLineToRelative(dy = -1.0f)
                curveTo(x1 = 70.66f, y1 = 5.57f, x2 = 63.9f, y2 = 0.0f, x3 = 51.72f, y3 = 0.0f)
                horizontalLineTo(x = 18.94f)
                curveTo(x1 = 6.78f, y1 = 0.0f, x2 = 0.0f, y2 = 5.56f, x3 = 0.0f, y3 = 16.97f)
                close()
            }
        }.build().also { _airPodsPro1CaseFill = it }
    }

@Suppress("ObjectPropertyName")
private var _airPodsPro1CaseFill: ImageVector? = null
