package me.kavishdevar.librepods.presentation.icons.common.airpods

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.icons.CommonIcons

val CommonIcons.AirPodsPro3Left: ImageVector
    get() {
        val current = _airPodsPro3Left
        if (current != null) return current

        return ImageVector.Builder(
            name = "AirPodsPro3Left",
            defaultWidth = 42.09400177001953.dp,
            defaultHeight = 61.78300094604492.dp,
            viewportWidth = 42.094f,
            viewportHeight = 61.783f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFFFFFFFF)),
                fillAlpha = 0.85f,
            ) {
                moveTo(x = 24.4f, y = 37.51f)
                verticalLineToRelative(dy = 17.52f)
                curveToRelative(dx1 = 0.0f, dy1 = 2.69f, dx2 = -1.65f, dy2 = 3.78f, dx3 = -4.12f, dy3 = 3.78f)
                horizontalLineTo(x = 17.5f)
                curveToRelative(dx1 = -2.4f, dy1 = 0.0f, dx2 = -4.06f, dy2 = -1.28f, dx3 = -4.06f, dy3 = -3.78f)
                verticalLineTo(y = 34.28f)
                arcTo(horizontalEllipseRadius = 32.0f, verticalEllipseRadius = 32.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 24.4f, y1 = 37.5f)
                moveToRelative(dx = 6.12f, dy = -27.7f)
                lineTo(x = 30.2f, y = 9.77f)
                curveToRelative(dx1 = -8.53f, dy1 = 0.0f, dx2 = -15.07f, dy2 = 7.85f, dx3 = -15.07f, dy3 = 14.85f)
                curveToRelative(dx1 = 0.0f, dy1 = 2.9f, dx2 = 0.6f, dy2 = 5.34f, dx3 = 1.67f, dy3 = 7.28f)
                curveToRelative(dx1 = -8.85f, dy1 = -3.28f, dx2 = -13.76f, dy2 = -9.73f, dx3 = -13.76f, dy3 = -16.25f)
                curveTo(x1 = 3.03f, y1 = 8.38f, x2 = 8.66f, y2 = 3.06f, x3 = 15.81f, y3 = 3.0f)
                curveToRelative(dx1 = 4.53f, dy1 = -0.03f, dx2 = 10.08f, dy2 = 1.86f, dx3 = 14.71f, dy3 = 6.8f)
                moveTo(x = 8.47f, y = 9.62f)
                arcTo(horizontalEllipseRadius = 2.17f, verticalEllipseRadius = 2.17f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 9.0f, y1 = 12.75f)
                lineToRelative(dx = 1.19f, dy = 0.84f)
                curveToRelative(dx1 = 1.1f, dy1 = 0.75f, dx2 = 2.53f, dy2 = 0.57f, dx3 = 3.22f, dy3 = -0.46f)
                arcToRelative(a = 2.3f, b = 2.3f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -0.5f, dy1 = -3.25f)
                lineToRelative(dx = -1.2f, dy = -0.85f)
                curveToRelative(dx1 = -1.12f, dy1 = -0.78f, dx2 = -2.46f, dy2 = -0.47f, dx3 = -3.24f, dy3 = 0.6f)
            }
            path(
                fill = SolidColor(Color(0xFFFFFFFF)),
                fillAlpha = 0.425f,
            ) {
                moveTo(x = 27.06f, y = 34.69f)
                curveToRelative(dx1 = 6.32f, dy1 = 0.0f, dx2 = 11.85f, dy2 = -6.1f, dx3 = 11.85f, dy3 = -12.38f)
                curveToRelative(dx1 = 0.0f, dy1 = -4.65f, dx2 = -3.32f, dy2 = -9.5f, dx3 = -8.72f, dy3 = -9.5f)
                curveToRelative(dx1 = -6.75f, dy1 = 0.0f, dx2 = -12.07f, dy2 = 6.35f, dx3 = -12.07f, dy3 = 11.82f)
                curveToRelative(dx1 = 0.0f, dy1 = 6.8f, dx2 = 3.82f, dy2 = 10.06f, dx3 = 8.94f, dy3 = 10.06f)
                moveToRelative(dx = 2.19f, dy = -6.4f)
                curveToRelative(dx1 = -0.84f, dy1 = 0.0f, dx2 = -1.22f, dy2 = -0.45f, dx3 = -1.19f, dy3 = -1.73f)
                curveToRelative(dx1 = 0.03f, dy1 = -2.18f, dx2 = 2.35f, dy2 = -5.37f, dx3 = 4.32f, dy3 = -5.37f)
                curveToRelative(dx1 = 0.78f, dy1 = 0.0f, dx2 = 1.4f, dy2 = 0.44f, dx3 = 1.4f, dy3 = 1.84f)
                curveToRelative(dx1 = 0.0f, dy1 = 2.06f, dx2 = -2.37f, dy2 = 5.25f, dx3 = -4.53f, dy3 = 5.25f)
            }
        }.build().also { _airPodsPro3Left = it }
    }

@Suppress("ObjectPropertyName")
private var _airPodsPro3Left: ImageVector? = null
