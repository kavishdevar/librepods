package me.kavishdevar.librepods.presentation.icons.common.airpods

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.icons.CommonIcons

val CommonIcons.AirPodsPro1Left: ImageVector
    get() {
        val current = _airPodsPro1Left
        if (current != null) return current

        return ImageVector.Builder(
            name = "AirPodsPro1Left",
            defaultWidth = 47.28099822998047.dp,
            defaultHeight = 61.65700149536133.dp,
            viewportWidth = 47.281f,
            viewportHeight = 61.657f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFFFFFFFF)),
                fillAlpha = 0.85f,
            ) {
                moveTo(x = 23.4f, y = 36.64f)
                verticalLineToRelative(dy = 18.9f)
                curveToRelative(dx1 = 0.0f, dy1 = 1.93f, dx2 = -1.27f, dy2 = 2.93f, dx3 = -3.21f, dy3 = 2.93f)
                horizontalLineToRelative(dx = -2.44f)
                curveToRelative(dx1 = -1.94f, dy1 = 0.0f, dx2 = -3.22f, dy2 = -1.0f, dx3 = -3.22f, dy3 = -2.94f)
                verticalLineTo(y = 34.91f)
                arcToRelative(a = 31.0f, b = 31.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 8.88f, dy1 = 1.73f)
                moveTo(x = 31.0f, y = 11.31f)
                arcToRelative(a = 18.0f, b = 18.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 1.78f, dy1 = 2.83f)
                curveToRelative(dx1 = -7.19f, dy1 = 1.15f, dx2 = -12.4f, dy2 = 7.59f, dx3 = -12.4f, dy3 = 13.92f)
                quadToRelative(dx1 = 0.02f, dy1 = 2.95f, dx2 = 1.0f, dy2 = 5.18f)
                arcToRelative(a = 22.0f, b = 22.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -9.0f, dy1 = -2.8f)
                curveToRelative(dx1 = -5.16f, dy1 = -2.97f, dx2 = -9.22f, dy2 = -7.78f, dx3 = -9.22f, dy3 = -14.69f)
                curveToRelative(dx1 = 0.0f, dy1 = -7.97f, dx2 = 5.62f, dy2 = -12.5f, dx3 = 12.53f, dy3 = -12.56f)
                curveTo(x1 = 21.03f, y1 = 3.13f, x2 = 27.0f, y2 = 5.75f, x3 = 31.0f, y3 = 11.3f)
                moveTo(x = 9.72f, y = 10.63f)
                arcToRelative(a = 1.43f, b = 1.43f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 0.12f, dy1 = 2.09f)
                lineToRelative(dx = 4.66f, dy = 3.87f)
                arcToRelative(a = 1.46f, b = 1.46f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 2.1f, dy1 = -0.18f)
                arcToRelative(a = 1.47f, b = 1.47f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -0.23f, dy1 = -2.13f)
                lineToRelative(dx = -4.56f, dy = -3.87f)
                arcToRelative(a = 1.46f, b = 1.46f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -2.1f, dy1 = 0.22f)
            }
            path(
                fill = SolidColor(Color(0xFFFFFFFF)),
                fillAlpha = 0.425f,
            ) {
                moveTo(x = 32.56f, y = 37.13f)
                curveToRelative(dx1 = 5.6f, dy1 = 0.0f, dx2 = 11.35f, dy2 = -4.44f, dx3 = 11.35f, dy3 = -11.22f)
                curveToRelative(dx1 = 0.0f, dy1 = -4.53f, dx2 = -3.2f, dy2 = -8.97f, dx3 = -8.82f, dy3 = -8.97f)
                curveToRelative(dx1 = -6.59f, dy1 = 0.0f, dx2 = -11.68f, dy2 = 5.72f, dx3 = -11.68f, dy3 = 11.12f)
                curveToRelative(dx1 = 0.0f, dy1 = 6.07f, dx2 = 4.28f, dy2 = 9.07f, dx3 = 9.15f, dy3 = 9.07f)
                moveToRelative(dx = 1.53f, dy = -4.75f)
                curveToRelative(dx1 = -0.87f, dy1 = -0.75f, dx2 = -0.37f, dy2 = -2.25f, dx3 = 1.41f, dy3 = -4.54f)
                curveToRelative(dx1 = 1.84f, dy1 = -2.25f, dx2 = 3.31f, dy2 = -3.0f, dx3 = 4.25f, dy3 = -2.18f)
                curveToRelative(dx1 = 0.84f, dy1 = 0.75f, dx2 = 0.38f, dy2 = 2.28f, dx3 = -1.4f, dy3 = 4.5f)
                curveToRelative(dx1 = -1.88f, dy1 = 2.22f, dx2 = -3.35f, dy2 = 3.0f, dx3 = -4.26f, dy3 = 2.22f)
            }
        }.build().also { _airPodsPro1Left = it }
    }

@Suppress("ObjectPropertyName")
private var _airPodsPro1Left: ImageVector? = null
