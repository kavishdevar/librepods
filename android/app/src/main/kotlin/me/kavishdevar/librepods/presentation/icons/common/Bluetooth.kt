package me.kavishdevar.librepods.presentation.icons.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.icons.CommonIcons

val CommonIcons.Bluetooth: ImageVector
    get() {
        if (_bluetooth != null) {
            return _bluetooth!!
        }
        _bluetooth =
            ImageVector.Builder(
                name = "bluetooth",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    path(
                        fill = SolidColor(Color.Black),
                        fillAlpha = 1f,
                        stroke = null,
                        strokeAlpha = 1f,
                        strokeLineWidth = 1f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Bevel,
                        strokeLineMiter = 1f,
                        pathFillType = PathFillType.NonZero,
                    ) {
                        moveTo(11f, 20.58f)
                        verticalLineTo(14.4f)
                        lineTo(7.1f, 18.3f)
                        quadTo(6.83f, 18.58f, 6.4f, 18.58f)
                        reflectiveQuadTo(5.7f, 18.3f)
                        quadTo(5.43f, 18.02f, 5.43f, 17.6f)
                        reflectiveQuadTo(5.7f, 16.9f)
                        lineTo(10.6f, 12f)
                        lineTo(5.7f, 7.1f)
                        quadTo(5.43f, 6.82f, 5.43f, 6.4f)
                        reflectiveQuadTo(5.7f, 5.7f)
                        reflectiveQuadTo(6.4f, 5.43f)
                        reflectiveQuadTo(7.1f, 5.7f)
                        lineTo(11f, 9.6f)
                        verticalLineTo(3.42f)
                        quadTo(11f, 2.97f, 11.3f, 2.69f)
                        reflectiveQuadTo(12f, 2.4f)
                        quadToRelative(0.2f, 0f, 0.38f, 0.07f)
                        reflectiveQuadTo(12.7f, 2.7f)
                        lineTo(17f, 7f)
                        quadToRelative(0.15f, 0.15f, 0.21f, 0.32f)
                        reflectiveQuadTo(17.28f, 7.7f)
                        reflectiveQuadTo(17.21f, 8.07f)
                        reflectiveQuadTo(17f, 8.4f)
                        lineTo(13.4f, 12f)
                        lineTo(17f, 15.6f)
                        quadToRelative(0.15f, 0.15f, 0.21f, 0.32f)
                        reflectiveQuadToRelative(0.06f, 0.38f)
                        reflectiveQuadToRelative(-0.06f, 0.38f)
                        reflectiveQuadTo(17f, 17f)
                        lineToRelative(-4.3f, 4.3f)
                        quadToRelative(-0.15f, 0.15f, -0.33f, 0.22f)
                        reflectiveQuadTo(12f, 21.6f)
                        quadToRelative(-0.4f, 0f, -0.7f, -0.29f)
                        reflectiveQuadTo(11f, 20.58f)
                        close()
                        moveTo(13f, 9.6f)
                        lineTo(14.9f, 7.7f)
                        lineTo(13f, 5.85f)
                        verticalLineTo(9.6f)
                        close()
                        moveToRelative(0f, 8.55f)
                        lineTo(14.9f, 16.3f)
                        lineTo(13f, 14.4f)
                        verticalLineToRelative(3.75f)
                        close()
                    }
                }
                .build()
        return _bluetooth!!
    }

private var _bluetooth: ImageVector? = null
