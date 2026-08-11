package me.kavishdevar.librepods.presentation.icons.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.icons.CommonIcons

val CommonIcons.LeftCircleFill: ImageVector
    get() {
        val current = _leftCircleFill
        if (current != null) return current

        return ImageVector.Builder(
            name = "LeftCircleFill",
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
                moveTo(x = 23.31f, y = 19.4f)
                verticalLineToRelative(dy = 23.93f)
                curveToRelative(dx1 = 0.0f, dy1 = 1.72f, dx2 = 0.9f, dy2 = 2.91f, dx3 = 2.57f, dy3 = 2.91f)
                horizontalLineToRelative(dx = 14.84f)
                curveToRelative(dx1 = 1.25f, dy1 = 0.0f, dx2 = 2.16f, dy2 = -0.81f, dx3 = 2.16f, dy3 = -2.12f)
                curveToRelative(dx1 = 0.0f, dy1 = -1.32f, dx2 = -0.91f, dy2 = -2.13f, dx3 = -2.16f, dy3 = -2.13f)
                horizontalLineTo(x = 28.5f)
                verticalLineTo(y = 19.4f)
                curveToRelative(dx1 = 0.0f, dy1 = -1.77f, dx2 = -0.9f, dy2 = -2.93f, dx3 = -2.62f, dy3 = -2.93f)
                curveToRelative(dx1 = -1.66f, dy1 = 0.0f, dx2 = -2.57f, dy2 = 1.22f, dx3 = -2.57f, dy3 = 2.94f)
            }
        }.build().also { _leftCircleFill = it }
    }

private var _leftCircleFill: ImageVector? = null
