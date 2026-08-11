package me.kavishdevar.librepods.presentation.icons.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.icons.CommonIcons

val CommonIcons.RightCircleFill: ImageVector
    get() {
        val current = _rightCircleFill
        if (current != null) return current

        return ImageVector.Builder(
            name = "RightCircleFill",
            defaultWidth = 63.9379997253418.dp,
            defaultHeight = 63.78099822998047.dp,
            viewportWidth = 63.938f,
            viewportHeight = 63.781f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFFFFFFFF)),
                fillAlpha = 0.85f,
            ) {
                moveTo(x = 63.75f, y = 31.88f)
                arcToRelative(a = 31.9f, b = 31.9f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -31.87f, dy1 = 31.87f)
                arcTo(horizontalEllipseRadius = 31.93f, verticalEllipseRadius = 31.93f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 0.0f, y1 = 31.88f)
                arcTo(horizontalEllipseRadius = 31.9f, verticalEllipseRadius = 31.9f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 31.88f, y1 = 0.0f)
                arcToRelative(a = 31.9f, b = 31.9f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 31.87f, dy1 = 31.88f)
                moveToRelative(dx = -40.19f, dy = -15.0f)
                curveToRelative(dx1 = -1.62f, dy1 = 0.0f, dx2 = -2.5f, dy2 = 1.18f, dx3 = -2.5f, dy3 = 2.93f)
                verticalLineToRelative(dy = 23.94f)
                curveToRelative(dx1 = 0.0f, dy1 = 1.72f, dx2 = 0.9f, dy2 = 2.9f, dx3 = 2.53f, dy3 = 2.9f)
                curveToRelative(dx1 = 1.7f, dy1 = 0.0f, dx2 = 2.63f, dy2 = -1.12f, dx3 = 2.63f, dy3 = -2.9f)
                verticalLineToRelative(dy = -8.0f)
                horizontalLineToRelative(dx = 6.0f)
                lineToRelative(dx = 5.56f, dy = 9.19f)
                curveToRelative(dx1 = 0.78f, dy1 = 1.25f, dx2 = 1.44f, dy2 = 1.72f, dx3 = 2.6f, dy3 = 1.72f)
                curveToRelative(dx1 = 1.37f, dy1 = 0.0f, dx2 = 2.34f, dy2 = -0.91f, dx3 = 2.34f, dy3 = -2.22f)
                arcToRelative(a = 3.3f, b = 3.3f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -0.6f, dy1 = -1.88f)
                lineTo(x = 37.33f, y = 35.0f)
                arcToRelative(a = 8.9f, b = 8.9f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 6.21f, dy1 = -8.62f)
                curveToRelative(dx1 = 0.0f, dy1 = -5.82f, dx2 = -4.16f, dy2 = -9.5f, dx3 = -10.69f, dy3 = -9.5f)
                close()
                moveToRelative(dx = 14.88f, dy = 9.62f)
                curveToRelative(dx1 = 0.0f, dy1 = 3.5f, dx2 = -2.47f, dy2 = 5.47f, dx3 = -6.35f, dy3 = 5.47f)
                horizontalLineToRelative(dx = -5.87f)
                verticalLineTo(y = 20.84f)
                horizontalLineToRelative(dx = 6.0f)
                curveToRelative(dx1 = 3.69f, dy1 = 0.0f, dx2 = 6.22f, dy2 = 2.07f, dx3 = 6.22f, dy3 = 5.66f)
            }
        }.build().also { _rightCircleFill = it }
    }

@Suppress("ObjectPropertyName")
private var _rightCircleFill: ImageVector? = null
