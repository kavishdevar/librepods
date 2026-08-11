package me.kavishdevar.librepods.presentation.icons.common.airpods

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.icons.CommonIcons

val CommonIcons.AirPodsCase: ImageVector
    get() {
        val current = _airPodsCase
        if (current != null) return current

        return ImageVector.Builder(
            name = "AirPodsCase",
            defaultWidth = 51.0.dp,
            defaultHeight = 62.09400177001953.dp,
            viewportWidth = 51.0f,
            viewportHeight = 62.094f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFFFFFFFF)),
                fillAlpha = 0.85f,
            ) {
                moveTo(x = 15.75f, y = 62.06f)
                horizontalLineToRelative(dx = 19.31f)
                curveToRelative(dx1 = 10.63f, dy1 = 0.0f, dx2 = 15.75f, dy2 = -5.12f, dx3 = 15.75f, dy3 = -15.75f)
                verticalLineTo(y = 15.75f)
                curveTo(x1 = 50.81f, y1 = 5.13f, x2 = 45.7f, y2 = 0.0f, x3 = 35.06f, y3 = 0.0f)
                horizontalLineTo(x = 15.75f)
                curveTo(x1 = 5.13f, y1 = 0.0f, x2 = 0.0f, y2 = 5.13f, x3 = 0.0f, y3 = 15.75f)
                verticalLineToRelative(dy = 30.56f)
                curveToRelative(dx1 = 0.0f, dy1 = 10.63f, dx2 = 5.13f, dy2 = 15.75f, dx3 = 15.75f, dy3 = 15.75f)
                moveToRelative(dx = 0.0f, dy = -5.03f)
                curveToRelative(dx1 = -7.47f, dy1 = 0.0f, dx2 = -10.72f, dy2 = -3.25f, dx3 = -10.72f, dy3 = -10.72f)
                verticalLineTo(y = 15.75f)
                curveToRelative(dx1 = 0.0f, dy1 = -7.47f, dx2 = 3.25f, dy2 = -10.72f, dx3 = 10.72f, dy3 = -10.72f)
                horizontalLineToRelative(dx = 19.31f)
                curveToRelative(dx1 = 7.47f, dy1 = 0.0f, dx2 = 10.72f, dy2 = 3.25f, dx3 = 10.72f, dy3 = 10.72f)
                verticalLineToRelative(dy = 30.56f)
                curveToRelative(dx1 = 0.0f, dy1 = 7.47f, dx2 = -3.25f, dy2 = 10.72f, dx3 = -10.72f, dy3 = 10.72f)
                close()
                moveTo(x = 2.53f, y = 20.81f)
                horizontalLineToRelative(dx = 45.75f)
                verticalLineToRelative(dy = -3.15f)
                horizontalLineTo(x = 2.53f)
                close()
                moveToRelative(dx = 14.22f, dy = 2.13f)
                horizontalLineToRelative(dx = 17.31f)
                arcToRelative(a = 3.6f, b = 3.6f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 3.75f, dy1 = -3.72f)
                arcToRelative(a = 3.6f, b = 3.6f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -3.75f, dy1 = -3.72f)
                horizontalLineTo(x = 16.75f)
                arcTo(horizontalEllipseRadius = 3.66f, verticalEllipseRadius = 3.66f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 13.0f, y1 = 19.22f)
                arcToRelative(a = 3.66f, b = 3.66f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 3.75f, dy1 = 3.72f)
            }
        }.build().also { _airPodsCase = it }
    }

@Suppress("ObjectPropertyName")
private var _airPodsCase: ImageVector? = null
