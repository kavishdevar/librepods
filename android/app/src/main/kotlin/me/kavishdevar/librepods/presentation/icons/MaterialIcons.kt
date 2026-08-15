package me.kavishdevar.librepods.presentation.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.icons.common.CircleDotted
import me.kavishdevar.librepods.presentation.icons.common.LeftCircleFill
import me.kavishdevar.librepods.presentation.icons.common.RightCircleFill

object MaterialIcons: IconSet {
    // Material Icons don't scale like Apple's. so we need to scale up all but Apple's
    fun isAppleIcon(name: String): Boolean {
        return when (name) {
            "CircleDotted", "LeftCircleFill", "RightCircleFill" -> true
            else -> name.startsWith("AirPods")
        }
    }

    override val Notifications: ImageVector
        get() {
            if (_notifications != null) {
                return _notifications!!
            }
            _notifications =
                ImageVector.Builder(
                    name = "notifications",
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
                            moveTo(4f, 19f)
                            verticalLineTo(17f)
                            horizontalLineTo(6f)
                            verticalLineTo(10f)
                            quadTo(6f, 7.93f, 7.25f, 6.31f)
                            reflectiveQuadTo(10.5f, 4.2f)
                            verticalLineTo(3.5f)
                            quadToRelative(0f, -0.63f, 0.44f, -1.06f)
                            reflectiveQuadTo(12f, 2f)
                            reflectiveQuadToRelative(1.06f, 0.44f)
                            reflectiveQuadTo(13.5f, 3.5f)
                            verticalLineTo(4.2f)
                            quadToRelative(2f, 0.5f, 3.25f, 2.11f)
                            reflectiveQuadTo(18f, 10f)
                            verticalLineToRelative(7f)
                            horizontalLineToRelative(2f)
                            verticalLineToRelative(2f)
                            horizontalLineTo(4f)
                            close()
                            moveToRelative(8f, -7.5f)
                            close()
                            moveTo(12f, 22f)
                            quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
                            reflectiveQuadTo(10f, 20f)
                            horizontalLineToRelative(4f)
                            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                            reflectiveQuadTo(12f, 22f)
                            close()
                            moveTo(8f, 17f)
                            horizontalLineToRelative(8f)
                            verticalLineTo(10f)
                            quadTo(16f, 8.35f, 14.83f, 7.18f)
                            reflectiveQuadTo(12f, 6f)
                            reflectiveQuadTo(9.18f, 7.18f)
                            reflectiveQuadTo(8f, 10f)
                            verticalLineToRelative(7f)
                            close()
                        }
                    }
                    .build()
            return _notifications!!
        }

    private var _notifications: ImageVector? = null


    override val Headphones: ImageVector
        get() {
            if (_headphones != null) {
                return _headphones!!
            }
            _headphones =
                ImageVector.Builder(
                    name = "headphones",
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
                            pathFillType = PathFillType.Companion.NonZero,
                        ) {
                            moveTo(9f, 21f)
                            horizontalLineTo(5f)
                            quadTo(4.18f, 21f, 3.59f, 20.41f)
                            reflectiveQuadTo(3f, 19f)
                            verticalLineTo(12f)
                            quadTo(3f, 10.13f, 3.71f, 8.49f)
                            reflectiveQuadTo(5.64f, 5.64f)
                            quadTo(6.85f, 4.42f, 8.49f, 3.71f)
                            reflectiveQuadTo(12f, 3f)
                            reflectiveQuadToRelative(3.51f, 0.71f)
                            reflectiveQuadToRelative(2.85f, 1.93f)
                            reflectiveQuadToRelative(1.93f, 2.85f)
                            reflectiveQuadTo(21f, 12f)
                            verticalLineToRelative(7f)
                            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                            reflectiveQuadTo(19f, 21f)
                            horizontalLineTo(15f)
                            verticalLineTo(13f)
                            horizontalLineToRelative(4f)
                            verticalLineTo(12f)
                            quadTo(19f, 9.07f, 16.96f, 7.04f)
                            reflectiveQuadTo(12f, 5f)
                            quadTo(9.08f, 5f, 7.04f, 7.04f)
                            reflectiveQuadTo(5f, 12f)
                            verticalLineToRelative(1f)
                            horizontalLineTo(9f)
                            verticalLineToRelative(8f)
                            close()
                            moveTo(7f, 15f)
                            horizontalLineTo(5f)
                            verticalLineToRelative(4f)
                            horizontalLineTo(7f)
                            verticalLineTo(15f)
                            close()
                            moveToRelative(10f, 0f)
                            verticalLineToRelative(4f)
                            horizontalLineToRelative(2f)
                            verticalLineTo(15f)
                            horizontalLineTo(17f)
                            close()
                            moveTo(7f, 15f)
                            horizontalLineTo(5f)
                            horizontalLineTo(7f)
                            close()
                            moveToRelative(10f, 0f)
                            horizontalLineToRelative(2f)
                            horizontalLineTo(17f)
                            close()
                        }
                    }
                    .build()
            return _headphones!!
        }

    private var _headphones: ImageVector? = null

    override val Play: ImageVector
        get() = PlayArrow

    override val Pause: ImageVector
        get() {
            if (_pause != null) {
                return _pause!!
            }
            _pause =
                ImageVector.Builder(
                    name = "pause",
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
                            moveTo(13f, 19f)
                            verticalLineTo(5f)
                            horizontalLineToRelative(6f)
                            verticalLineTo(19f)
                            horizontalLineTo(13f)
                            close()
                            moveTo(5f, 19f)
                            verticalLineTo(5f)
                            horizontalLineToRelative(6f)
                            verticalLineTo(19f)
                            horizontalLineTo(5f)
                            close()
                            moveTo(15f, 17f)
                            horizontalLineToRelative(2f)
                            verticalLineTo(7f)
                            horizontalLineTo(15f)
                            verticalLineTo(17f)
                            close()
                            moveTo(7f, 17f)
                            horizontalLineTo(9f)
                            verticalLineTo(7f)
                            horizontalLineTo(7f)
                            verticalLineTo(17f)
                            close()
                            moveTo(7f, 7f)
                            verticalLineTo(17f)
                            verticalLineTo(7f)
                            close()
                            moveToRelative(8f, 0f)
                            verticalLineTo(17f)
                            verticalLineTo(7f)
                            close()
                        }
                    }
                    .build()
            return _pause!!
        }

    private var _pause: ImageVector? = null

    override val Bluetooth: ImageVector
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
                            moveTo(11f, 22f)
                            verticalLineTo(14.4f)
                            lineTo(6.4f, 19f)
                            lineTo(5f, 17.6f)
                            lineTo(10.6f, 12f)
                            lineTo(5f, 6.4f)
                            lineTo(6.4f, 5f)
                            lineTo(11f, 9.6f)
                            verticalLineTo(2f)
                            horizontalLineToRelative(1f)
                            lineToRelative(5.7f, 5.7f)
                            lineTo(13.4f, 12f)
                            lineToRelative(4.3f, 4.3f)
                            lineTo(12f, 22f)
                            horizontalLineTo(11f)
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
    override val Call: ImageVector
        get() {
            if (_call != null) {
                return _call!!
            }
            _call =
                ImageVector.Builder(
                    name = "call",
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
                            moveTo(19.95f, 21f)
                            quadToRelative(-3.13f, 0f, -6.18f, -1.36f)
                            reflectiveQuadTo(8.23f, 15.78f)
                            quadTo(5.73f, 13.27f, 4.36f, 10.23f)
                            reflectiveQuadTo(3f, 4.05f)
                            quadTo(3f, 3.6f, 3.3f, 3.3f)
                            reflectiveQuadTo(4.05f, 3f)
                            horizontalLineTo(8.1f)
                            quadTo(8.45f, 3f, 8.73f, 3.24f)
                            reflectiveQuadTo(9.05f, 3.8f)
                            lineTo(9.7f, 7.3f)
                            quadTo(9.75f, 7.7f, 9.68f, 7.97f)
                            reflectiveQuadTo(9.4f, 8.45f)
                            lineTo(6.98f, 10.9f)
                            quadToRelative(0.5f, 0.93f, 1.19f, 1.79f)
                            reflectiveQuadToRelative(1.51f, 1.66f)
                            quadToRelative(0.78f, 0.78f, 1.63f, 1.44f)
                            reflectiveQuadTo(13.1f, 17f)
                            lineToRelative(2.35f, -2.35f)
                            quadToRelative(0.22f, -0.23f, 0.59f, -0.34f)
                            reflectiveQuadToRelative(0.71f, -0.06f)
                            lineToRelative(3.45f, 0.7f)
                            quadToRelative(0.35f, 0.1f, 0.57f, 0.36f)
                            reflectiveQuadTo(21f, 15.9f)
                            verticalLineToRelative(4.05f)
                            quadToRelative(0f, 0.45f, -0.3f, 0.75f)
                            reflectiveQuadTo(19.95f, 21f)
                            close()
                            moveTo(6.03f, 9f)
                            lineTo(7.68f, 7.35f)
                            lineTo(7.25f, 5f)
                            horizontalLineTo(5.03f)
                            quadTo(5.15f, 6.02f, 5.38f, 7.02f)
                            reflectiveQuadTo(6.03f, 9f)
                            close()
                            moveToRelative(8.95f, 8.95f)
                            quadToRelative(0.97f, 0.43f, 1.99f, 0.68f)
                            reflectiveQuadTo(19f, 18.95f)
                            verticalLineToRelative(-2.2f)
                            lineTo(16.65f, 16.27f)
                            lineToRelative(-1.68f, 1.68f)
                            close()
                            moveTo(6.03f, 9f)
                            close()
                            moveToRelative(8.95f, 8.95f)
                            close()
                        }
                    }
                    .build()
            return _call!!
        }

    private var _call: ImageVector? = null

    override val Overlay: ImageVector
        get() = Stack

    val Stack: ImageVector
        get() {
            if (_stack != null) {
                return _stack!!
            }
            _stack =
                ImageVector.Builder(
                    name = "stack",
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
                            moveTo(6f, 14f)
                            verticalLineToRelative(2f)
                            horizontalLineTo(4f)
                            quadTo(3.18f, 16f, 2.59f, 15.41f)
                            reflectiveQuadTo(2f, 14f)
                            verticalLineTo(4f)
                            quadTo(2f, 3.17f, 2.59f, 2.59f)
                            reflectiveQuadTo(4f, 2f)
                            horizontalLineTo(14f)
                            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                            reflectiveQuadTo(16f, 4f)
                            verticalLineTo(6f)
                            horizontalLineTo(14f)
                            verticalLineTo(4f)
                            horizontalLineTo(4f)
                            verticalLineTo(14f)
                            horizontalLineTo(6f)
                            close()
                            moveToRelative(4f, 8f)
                            quadTo(9.18f, 22f, 8.59f, 21.41f)
                            reflectiveQuadTo(8f, 20f)
                            verticalLineTo(10f)
                            quadTo(8f, 9.17f, 8.59f, 8.59f)
                            reflectiveQuadTo(10f, 8f)
                            horizontalLineTo(20f)
                            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                            reflectiveQuadTo(22f, 10f)
                            verticalLineTo(20f)
                            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                            reflectiveQuadTo(20f, 22f)
                            horizontalLineTo(10f)
                            close()
                            moveToRelative(0f, -2f)
                            horizontalLineTo(20f)
                            verticalLineTo(10f)
                            horizontalLineTo(10f)
                            verticalLineTo(20f)
                            close()
                            moveToRelative(5f, -5f)
                            close()
                        }
                    }
                    .build()
            return _stack!!
        }

    private var _stack: ImageVector? = null

    override val ArrowBack: ImageVector
        get() {
            if (_arrow_back != null) {
                return _arrow_back!!
            }
            _arrow_back =
                ImageVector.Builder(
                    name = "arrow_back",
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
                            moveTo(7.83f, 13f)
                            lineToRelative(4.9f, 4.9f)
                            quadToRelative(0.3f, 0.3f, 0.29f, 0.7f)
                            reflectiveQuadTo(12.7f, 19.3f)
                            quadTo(12.4f, 19.58f, 12f, 19.59f)
                            reflectiveQuadTo(11.3f, 19.3f)
                            lineTo(4.7f, 12.7f)
                            quadTo(4.55f, 12.55f, 4.49f, 12.38f)
                            reflectiveQuadTo(4.43f, 12f)
                            reflectiveQuadTo(4.49f, 11.63f)
                            reflectiveQuadTo(4.7f, 11.3f)
                            lineTo(11.3f, 4.7f)
                            quadTo(11.58f, 4.42f, 11.99f, 4.42f)
                            reflectiveQuadTo(12.7f, 4.7f)
                            quadTo(13f, 5f, 13f, 5.41f)
                            reflectiveQuadTo(12.7f, 6.13f)
                            lineTo(7.83f, 11f)
                            horizontalLineTo(19f)
                            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                            reflectiveQuadTo(20f, 12f)
                            reflectiveQuadToRelative(-0.29f, 0.71f)
                            reflectiveQuadTo(19f, 13f)
                            horizontalLineTo(7.83f)
                            close()
                        }
                    }
                    .build()
            return _arrow_back!!
        }

    private var _arrow_back: ImageVector? = null

    override val LeftCircleFill: ImageVector
        get() = CommonIcons.LeftCircleFill

    override val RightCircleFill: ImageVector
        get() = CommonIcons.RightCircleFill

    override val Settings: ImageVector
        get() {
            if (_settings != null) {
                return _settings!!
            }
            _settings =
                ImageVector.Builder(
                    name = "settings",
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
                            moveTo(10.83f, 22f)
                            quadTo(10.15f, 22f, 9.66f, 21.55f)
                            reflectiveQuadTo(9.08f, 20.45f)
                            lineTo(8.85f, 18.8f)
                            quadTo(8.53f, 18.68f, 8.24f, 18.5f)
                            reflectiveQuadTo(7.68f, 18.13f)
                            lineTo(6.13f, 18.77f)
                            quadTo(5.5f, 19.05f, 4.88f, 18.83f)
                            reflectiveQuadTo(3.9f, 18.02f)
                            lineTo(2.73f, 15.98f)
                            quadTo(2.38f, 15.4f, 2.53f, 14.75f)
                            reflectiveQuadTo(3.2f, 13.68f)
                            lineToRelative(1.33f, -1f)
                            quadTo(4.5f, 12.5f, 4.5f, 12.34f)
                            quadToRelative(0f, -0.16f, 0f, -0.34f)
                            reflectiveQuadToRelative(0f, -0.34f)
                            reflectiveQuadTo(4.53f, 11.33f)
                            lineToRelative(-1.33f, -1f)
                            quadTo(2.68f, 9.9f, 2.53f, 9.25f)
                            reflectiveQuadTo(2.73f, 8.02f)
                            lineTo(3.9f, 5.97f)
                            quadTo(4.25f, 5.4f, 4.88f, 5.18f)
                            reflectiveQuadTo(6.13f, 5.22f)
                            lineTo(7.68f, 5.88f)
                            quadTo(7.95f, 5.68f, 8.25f, 5.5f)
                            reflectiveQuadTo(8.85f, 5.2f)
                            lineTo(9.08f, 3.55f)
                            quadTo(9.18f, 2.9f, 9.66f, 2.45f)
                            reflectiveQuadTo(10.83f, 2f)
                            horizontalLineToRelative(2.35f)
                            quadToRelative(0.68f, 0f, 1.16f, 0.45f)
                            reflectiveQuadToRelative(0.59f, 1.1f)
                            lineTo(15.15f, 5.2f)
                            quadToRelative(0.33f, 0.13f, 0.61f, 0.3f)
                            reflectiveQuadToRelative(0.56f, 0.38f)
                            lineTo(17.88f, 5.22f)
                            quadTo(18.5f, 4.95f, 19.13f, 5.18f)
                            reflectiveQuadToRelative(0.98f, 0.8f)
                            lineToRelative(1.18f, 2.05f)
                            quadToRelative(0.35f, 0.58f, 0.2f, 1.23f)
                            reflectiveQuadTo(20.8f, 10.33f)
                            lineToRelative(-1.32f, 1f)
                            quadToRelative(0.02f, 0.18f, 0.02f, 0.34f)
                            reflectiveQuadToRelative(0f, 0.34f)
                            reflectiveQuadToRelative(0f, 0.34f)
                            reflectiveQuadToRelative(-0.05f, 0.34f)
                            lineToRelative(1.32f, 1f)
                            quadToRelative(0.52f, 0.43f, 0.68f, 1.08f)
                            reflectiveQuadToRelative(-0.2f, 1.22f)
                            lineToRelative(-1.2f, 2.05f)
                            quadToRelative(-0.35f, 0.58f, -0.98f, 0.8f)
                            reflectiveQuadTo(17.83f, 18.77f)
                            lineToRelative(-1.5f, -0.65f)
                            quadToRelative(-0.27f, 0.2f, -0.57f, 0.38f)
                            reflectiveQuadToRelative(-0.6f, 0.3f)
                            lineToRelative(-0.22f, 1.65f)
                            quadToRelative(-0.1f, 0.65f, -0.59f, 1.1f)
                            reflectiveQuadTo(13.18f, 22f)
                            horizontalLineTo(10.83f)
                            close()
                            moveTo(11f, 20f)
                            horizontalLineToRelative(1.98f)
                            lineToRelative(0.35f, -2.65f)
                            quadToRelative(0.78f, -0.2f, 1.44f, -0.59f)
                            reflectiveQuadToRelative(1.21f, -0.94f)
                            lineToRelative(2.47f, 1.03f)
                            lineToRelative(0.98f, -1.7f)
                            lineTo(17.28f, 13.52f)
                            quadToRelative(0.13f, -0.35f, 0.17f, -0.74f)
                            reflectiveQuadTo(17.5f, 12f)
                            reflectiveQuadTo(17.45f, 11.21f)
                            quadTo(17.4f, 10.83f, 17.28f, 10.48f)
                            lineTo(19.43f, 8.85f)
                            lineTo(18.45f, 7.15f)
                            lineTo(15.98f, 8.2f)
                            quadTo(15.43f, 7.63f, 14.76f, 7.24f)
                            reflectiveQuadTo(13.33f, 6.65f)
                            lineTo(13f, 4f)
                            horizontalLineTo(11.03f)
                            lineTo(10.68f, 6.65f)
                            quadTo(9.9f, 6.85f, 9.24f, 7.24f)
                            reflectiveQuadTo(8.03f, 8.17f)
                            lineTo(5.55f, 7.15f)
                            lineTo(4.58f, 8.85f)
                            lineToRelative(2.15f, 1.6f)
                            quadTo(6.6f, 10.83f, 6.55f, 11.2f)
                            reflectiveQuadTo(6.5f, 12f)
                            quadToRelative(0f, 0.4f, 0.05f, 0.77f)
                            reflectiveQuadToRelative(0.17f, 0.75f)
                            lineTo(4.58f, 15.15f)
                            lineToRelative(0.98f, 1.7f)
                            lineTo(8.03f, 15.8f)
                            quadToRelative(0.55f, 0.58f, 1.21f, 0.96f)
                            reflectiveQuadToRelative(1.44f, 0.59f)
                            lineTo(11f, 20f)
                            close()
                            moveToRelative(1.05f, -4.5f)
                            quadToRelative(1.45f, 0f, 2.47f, -1.03f)
                            reflectiveQuadTo(15.55f, 12f)
                            reflectiveQuadTo(14.53f, 9.52f)
                            reflectiveQuadTo(12.05f, 8.5f)
                            quadToRelative(-1.47f, 0f, -2.49f, 1.02f)
                            reflectiveQuadTo(8.55f, 12f)
                            reflectiveQuadToRelative(1.01f, 2.47f)
                            reflectiveQuadToRelative(2.49f, 1.03f)
                            close()
                            moveTo(12f, 12f)
                            close()
                        }
                    }
                    .build()
            return _settings!!
        }

    private var _settings: ImageVector? = null

    val PlayArrow: ImageVector
        get() {
            if (_play_arrow != null) {
                return _play_arrow!!
            }
            _play_arrow =
                ImageVector.Builder(
                    name = "play_arrow",
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
                            moveTo(8f, 17.18f)
                            verticalLineTo(6.82f)
                            quadTo(8f, 6.4f, 8.3f, 6.11f)
                            quadTo(8.6f, 5.82f, 9f, 5.82f)
                            quadToRelative(0.13f, 0f, 0.26f, 0.04f)
                            reflectiveQuadTo(9.53f, 5.97f)
                            lineToRelative(8.15f, 5.18f)
                            quadToRelative(0.23f, 0.15f, 0.34f, 0.38f)
                            quadToRelative(0.11f, 0.23f, 0.11f, 0.48f)
                            reflectiveQuadToRelative(-0.11f, 0.47f)
                            reflectiveQuadToRelative(-0.34f, 0.38f)
                            lineTo(9.53f, 18.02f)
                            quadTo(9.4f, 18.1f, 9.26f, 18.14f)
                            quadTo(9.13f, 18.18f, 9f, 18.18f)
                            quadToRelative(-0.4f, 0f, -0.7f, -0.29f)
                            reflectiveQuadTo(8f, 17.18f)
                            close()
                            moveTo(10f, 12f)
                            close()
                            moveToRelative(0f, 3.35f)
                            lineTo(15.25f, 12f)
                            lineTo(10f, 8.65f)
                            verticalLineToRelative(6.7f)
                            close()
                        }
                    }
                    .build()
            return _play_arrow!!
        }

    private var _play_arrow: ImageVector? = null

    override val Send: ImageVector
        get() {
            if (_send != null) {
                return _send!!
            }
            _send =
                ImageVector.Builder(
                    name = "send",
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
                            moveTo(19.8f, 12.93f)
                            lineTo(4.4f, 19.43f)
                            quadTo(3.9f, 19.63f, 3.45f, 19.34f)
                            reflectiveQuadTo(3f, 18.5f)
                            verticalLineTo(5.5f)
                            quadTo(3f, 4.95f, 3.45f, 4.66f)
                            quadTo(3.9f, 4.38f, 4.4f, 4.57f)
                            lineToRelative(15.4f, 6.5f)
                            quadToRelative(0.63f, 0.28f, 0.63f, 0.93f)
                            reflectiveQuadTo(19.8f, 12.93f)
                            close()
                            moveTo(5f, 17f)
                            lineTo(16.85f, 12f)
                            lineTo(5f, 7f)
                            verticalLineToRelative(3.5f)
                            lineTo(11f, 12f)
                            lineTo(5f, 13.5f)
                            verticalLineTo(17f)
                            close()
                            moveToRelative(0f, 0f)
                            verticalLineTo(12f)
                            verticalLineTo(7f)
                            verticalLineToRelative(3.5f)
                            verticalLineToRelative(3f)
                            verticalLineTo(17f)
                            close()
                        }
                    }
                    .build()
            return _send!!
        }

    private var _send: ImageVector? = null

    override val Close: ImageVector
        get() {
            if (_close != null) {
                return _close!!
            }
            _close =
                ImageVector.Builder(
                    name = "close",
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
                            moveTo(12f, 13.4f)
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
                            lineTo(12f, 10.6f)
                            lineTo(16.9f, 5.7f)
                            quadTo(17.18f, 5.43f, 17.6f, 5.43f)
                            reflectiveQuadTo(18.3f, 5.7f)
                            reflectiveQuadToRelative(0.27f, 0.7f)
                            reflectiveQuadTo(18.3f, 7.1f)
                            lineTo(13.4f, 12f)
                            lineToRelative(4.9f, 4.9f)
                            quadToRelative(0.27f, 0.28f, 0.27f, 0.7f)
                            quadToRelative(0f, 0.42f, -0.27f, 0.7f)
                            reflectiveQuadToRelative(-0.7f, 0.27f)
                            reflectiveQuadTo(16.9f, 18.3f)
                            lineTo(12f, 13.4f)
                            close()
                        }
                    }
                    .build()
            return _close!!
        }

    private var _close: ImageVector? = null

    override val CloseCircle: ImageVector
        get() = Cancel

    override val SpeakerMin: ImageVector
        get() = VolumeMute

    override val SpeakerMax: ImageVector
        get() = VolumeUp

    val VolumeMute: ImageVector
        get() {
            if (_volumeMute != null) {
                return _volumeMute!!
            }
            _volumeMute =
                ImageVector.Builder(
                    name = "volume_mute",
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
                            moveTo(11f, 15f)
                            horizontalLineTo(8f)
                            quadTo(7.58f, 15f, 7.29f, 14.71f)
                            reflectiveQuadTo(7f, 14f)
                            verticalLineTo(10f)
                            quadTo(7f, 9.57f, 7.29f, 9.29f)
                            reflectiveQuadTo(8f, 9f)
                            horizontalLineToRelative(3f)
                            lineTo(14.3f, 5.7f)
                            quadTo(14.78f, 5.22f, 15.39f, 5.49f)
                            reflectiveQuadTo(16f, 6.43f)
                            verticalLineTo(17.58f)
                            quadToRelative(0f, 0.68f, -0.61f, 0.94f)
                            reflectiveQuadTo(14.3f, 18.3f)
                            lineTo(11f, 15f)
                            close()
                            moveTo(9f, 13f)
                            horizontalLineToRelative(2.85f)
                            lineTo(14f, 15.15f)
                            verticalLineTo(8.85f)
                            lineTo(11.85f, 11f)
                            horizontalLineTo(9f)
                            verticalLineToRelative(2f)
                            close()
                            moveToRelative(2.5f, -1f)
                            close()
                        }
                    }
                    .build()
            return _volumeMute!!
        }

    private var _volumeMute: ImageVector? = null

    val VolumeUp: ImageVector
        get() {
            if (_volumeUp != null) {
                return _volumeUp!!
            }
            _volumeUp =
                ImageVector.Builder(
                    name = "volume_up",
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
                            moveTo(19f, 11.98f)
                            quadTo(19f, 9.9f, 17.9f, 8.19f)
                            quadTo(16.8f, 6.47f, 14.95f, 5.63f)
                            quadTo(14.58f, 5.45f, 14.4f, 5.09f)
                            reflectiveQuadTo(14.35f, 4.35f)
                            quadTo(14.5f, 3.95f, 14.89f, 3.77f)
                            reflectiveQuadToRelative(0.79f, 0f)
                            quadToRelative(2.43f, 1.07f, 3.88f, 3.29f)
                            quadTo(21f, 9.27f, 21f, 11.98f)
                            reflectiveQuadToRelative(-1.45f, 4.91f)
                            reflectiveQuadToRelative(-3.88f, 3.29f)
                            quadToRelative(-0.4f, 0.18f, -0.79f, 0f)
                            reflectiveQuadTo(14.35f, 19.6f)
                            quadTo(14.23f, 19.23f, 14.4f, 18.86f)
                            reflectiveQuadToRelative(0.55f, -0.54f)
                            quadTo(16.8f, 17.48f, 17.9f, 15.76f)
                            reflectiveQuadTo(19f, 11.98f)
                            close()
                            moveTo(7f, 15f)
                            horizontalLineTo(4f)
                            quadTo(3.58f, 15f, 3.29f, 14.71f)
                            reflectiveQuadTo(3f, 14f)
                            verticalLineTo(10f)
                            quadTo(3f, 9.57f, 3.29f, 9.29f)
                            reflectiveQuadTo(4f, 9f)
                            horizontalLineTo(7f)
                            lineTo(10.3f, 5.7f)
                            quadTo(10.78f, 5.22f, 11.39f, 5.49f)
                            reflectiveQuadTo(12f, 6.43f)
                            verticalLineTo(17.58f)
                            quadToRelative(0f, 0.68f, -0.61f, 0.94f)
                            reflectiveQuadTo(10.3f, 18.3f)
                            lineTo(7f, 15f)
                            close()
                            moveToRelative(9.5f, -3f)
                            quadToRelative(0f, 1.05f, -0.47f, 1.99f)
                            reflectiveQuadToRelative(-1.25f, 1.54f)
                            quadToRelative(-0.25f, 0.15f, -0.51f, 0.01f)
                            reflectiveQuadTo(14f, 15.1f)
                            verticalLineTo(8.85f)
                            quadToRelative(0f, -0.3f, 0.26f, -0.44f)
                            quadToRelative(0.26f, -0.14f, 0.51f, 0.01f)
                            quadTo(15.55f, 9.05f, 16.03f, 10f)
                            reflectiveQuadToRelative(0.47f, 2f)
                            close()
                            moveTo(10f, 8.85f)
                            lineTo(7.85f, 11f)
                            horizontalLineTo(5f)
                            verticalLineToRelative(2f)
                            horizontalLineTo(7.85f)
                            lineTo(10f, 15.15f)
                            verticalLineTo(8.85f)
                            close()
                            moveTo(7.5f, 12f)
                            close()
                        }
                    }
                    .build()
            return _volumeUp!!
        }

    private var _volumeUp: ImageVector? = null

    override val Bolt: ImageVector
        get() {
            if (_bolt != null) {
                return _bolt!!
            }
            _bolt =
                ImageVector.Builder(
                    name = "bolt",
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
                            moveTo(10.55f, 18.2f)
                            lineTo(15.73f, 12f)
                            horizontalLineToRelative(-4f)
                            lineTo(12.45f, 6.32f)
                            lineTo(7.83f, 13f)
                            horizontalLineTo(11.3f)
                            lineToRelative(-0.75f, 5.2f)
                            close()
                            moveTo(9f, 15f)
                            horizontalLineTo(5.9f)
                            quadTo(5.3f, 15f, 5.01f, 14.46f)
                            quadTo(4.73f, 13.93f, 5.08f, 13.43f)
                            lineTo(12.55f, 2.67f)
                            quadTo(12.8f, 2.32f, 13.2f, 2.19f)
                            reflectiveQuadTo(14.03f, 2.2f)
                            reflectiveQuadToRelative(0.63f, 0.52f)
                            reflectiveQuadToRelative(0.15f, 0.8f)
                            lineTo(14f, 10f)
                            horizontalLineToRelative(3.88f)
                            quadToRelative(0.65f, 0f, 0.91f, 0.57f)
                            reflectiveQuadToRelative(-0.16f, 1.07f)
                            lineTo(10.4f, 21.5f)
                            quadToRelative(-0.28f, 0.32f, -0.67f, 0.43f)
                            reflectiveQuadTo(8.95f, 21.85f)
                            reflectiveQuadTo(8.36f, 21.31f)
                            reflectiveQuadTo(8.2f, 20.53f)
                            lineTo(9f, 15f)
                            close()
                            moveToRelative(2.78f, -2.75f)
                            close()
                        }
                    }
                    .build()
            return _bolt!!
        }

    private var _bolt: ImageVector? = null

    val Cancel: ImageVector
        get() {
            if (_cancel != null) {
                return _cancel!!
            }
            _cancel =
                ImageVector.Builder(
                    name = "cancel",
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
                            moveTo(12f, 13.4f)
                            lineToRelative(2.9f, 2.9f)
                            quadToRelative(0.28f, 0.27f, 0.7f, 0.27f)
                            reflectiveQuadTo(16.3f, 16.3f)
                            quadToRelative(0.27f, -0.28f, 0.27f, -0.7f)
                            reflectiveQuadTo(16.3f, 14.9f)
                            lineTo(13.4f, 12f)
                            lineTo(16.3f, 9.1f)
                            quadTo(16.58f, 8.82f, 16.58f, 8.4f)
                            reflectiveQuadTo(16.3f, 7.7f)
                            reflectiveQuadTo(15.6f, 7.43f)
                            reflectiveQuadTo(14.9f, 7.7f)
                            lineTo(12f, 10.6f)
                            lineTo(9.1f, 7.7f)
                            quadTo(8.83f, 7.43f, 8.4f, 7.43f)
                            reflectiveQuadTo(7.7f, 7.7f)
                            reflectiveQuadTo(7.43f, 8.4f)
                            reflectiveQuadTo(7.7f, 9.1f)
                            lineTo(10.6f, 12f)
                            lineTo(7.7f, 14.9f)
                            quadTo(7.43f, 15.18f, 7.43f, 15.6f)
                            reflectiveQuadTo(7.7f, 16.3f)
                            reflectiveQuadToRelative(0.7f, 0.27f)
                            quadToRelative(0.43f, 0f, 0.7f, -0.27f)
                            lineTo(12f, 13.4f)
                            close()
                            moveTo(12f, 22f)
                            quadTo(9.93f, 22f, 8.1f, 21.21f)
                            quadTo(6.28f, 20.43f, 4.93f, 19.08f)
                            quadTo(3.58f, 17.73f, 2.79f, 15.9f)
                            reflectiveQuadTo(2f, 12f)
                            quadTo(2f, 9.92f, 2.79f, 8.1f)
                            quadTo(3.58f, 6.27f, 4.93f, 4.93f)
                            quadTo(6.28f, 3.57f, 8.1f, 2.79f)
                            quadTo(9.93f, 2f, 12f, 2f)
                            reflectiveQuadToRelative(3.9f, 0.79f)
                            reflectiveQuadToRelative(3.17f, 2.14f)
                            quadToRelative(1.35f, 1.35f, 2.14f, 3.17f)
                            quadTo(22f, 9.92f, 22f, 12f)
                            reflectiveQuadToRelative(-0.79f, 3.9f)
                            reflectiveQuadToRelative(-2.14f, 3.17f)
                            quadToRelative(-1.35f, 1.35f, -3.17f, 2.14f)
                            reflectiveQuadTo(12f, 22f)
                            close()
                            moveToRelative(0f, -2f)
                            quadToRelative(3.35f, 0f, 5.68f, -2.32f)
                            reflectiveQuadTo(20f, 12f)
                            reflectiveQuadTo(17.68f, 6.32f)
                            reflectiveQuadTo(12f, 4f)
                            reflectiveQuadTo(6.33f, 6.32f)
                            reflectiveQuadTo(4f, 12f)
                            reflectiveQuadToRelative(2.33f, 5.68f)
                            reflectiveQuadTo(12f, 20f)
                            close()
                            moveToRelative(0f, -8f)
                            close()
                        }
                    }
                    .build()
            return _cancel!!
        }

    private var _cancel: ImageVector? = null

    override val Check: ImageVector
        get() {
            if (_check != null) {
                return _check!!
            }
            _check =
                ImageVector.Builder(
                    name = "check",
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
                            pathFillType = PathFillType.Companion.NonZero,
                        ) {
                            moveTo(9.55f, 15.15f)
                            lineTo(18.03f, 6.68f)
                            quadToRelative(0.3f, -0.3f, 0.7f, -0.3f)
                            reflectiveQuadToRelative(0.7f, 0.3f)
                            quadToRelative(0.3f, 0.3f, 0.3f, 0.71f)
                            reflectiveQuadTo(19.43f, 8.1f)
                            lineToRelative(-9.18f, 9.2f)
                            quadToRelative(-0.3f, 0.3f, -0.7f, 0.3f)
                            reflectiveQuadTo(8.85f, 17.3f)
                            lineTo(4.55f, 13f)
                            quadTo(4.25f, 12.7f, 4.26f, 12.29f)
                            reflectiveQuadTo(4.58f, 11.58f)
                            reflectiveQuadToRelative(0.71f, -0.3f)
                            reflectiveQuadTo(6f, 11.58f)
                            lineToRelative(3.55f, 3.58f)
                            close()
                        }
                    }
                    .build()
            return _check!!
        }

    private var _check: ImageVector? = null

    override val ChevronRight: ImageVector
        get() {
            if (_chevron_right != null) {
                return _chevron_right!!
            }
            _chevron_right =
                ImageVector.Builder(
                    name = "chevron_right",
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
                            pathFillType = PathFillType.Companion.NonZero,
                        ) {
                            moveTo(12.6f, 12f)
                            lineTo(8.7f, 8.1f)
                            quadTo(8.43f, 7.82f, 8.43f, 7.4f)
                            reflectiveQuadTo(8.7f, 6.7f)
                            reflectiveQuadTo(9.4f, 6.43f)
                            reflectiveQuadTo(10.1f, 6.7f)
                            lineToRelative(4.6f, 4.6f)
                            quadToRelative(0.15f, 0.15f, 0.21f, 0.33f)
                            reflectiveQuadTo(14.98f, 12f)
                            reflectiveQuadToRelative(-0.06f, 0.38f)
                            reflectiveQuadTo(14.7f, 12.7f)
                            lineToRelative(-4.6f, 4.6f)
                            quadTo(9.83f, 17.58f, 9.4f, 17.58f)
                            reflectiveQuadTo(8.7f, 17.3f)
                            quadTo(8.43f, 17.02f, 8.43f, 16.6f)
                            reflectiveQuadTo(8.7f, 15.9f)
                            lineTo(12.6f, 12f)
                            close()
                        }
                    }
                    .build()
            return _chevron_right!!
        }

    private var _chevron_right: ImageVector? = null

    override val ChevronLeft: ImageVector
        get() {
            if (_chevron_left != null) {
                return _chevron_left!!
            }
            _chevron_left =
                ImageVector.Builder(
                    name = "chevron_left",
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
                            pathFillType = PathFillType.Companion.NonZero,
                        ) {
                            moveTo(10.8f, 12f)
                            lineToRelative(3.9f, 3.9f)
                            quadToRelative(0.28f, 0.28f, 0.28f, 0.7f)
                            quadToRelative(0f, 0.42f, -0.28f, 0.7f)
                            reflectiveQuadTo(14f, 17.58f)
                            reflectiveQuadTo(13.3f, 17.3f)
                            lineTo(8.7f, 12.7f)
                            quadTo(8.55f, 12.55f, 8.49f, 12.38f)
                            reflectiveQuadTo(8.43f, 12f)
                            reflectiveQuadTo(8.49f, 11.63f)
                            reflectiveQuadTo(8.7f, 11.3f)
                            lineTo(13.3f, 6.7f)
                            quadTo(13.58f, 6.43f, 14f, 6.43f)
                            reflectiveQuadTo(14.7f, 6.7f)
                            reflectiveQuadToRelative(0.28f, 0.7f)
                            reflectiveQuadTo(14.7f, 8.1f)
                            lineTo(10.8f, 12f)
                            close()
                        }
                    }
                    .build()
            return _chevron_left!!
        }

    private var _chevron_left: ImageVector? = null

    override val Save: ImageVector
        get() {
            if (_save != null) {
                return _save!!
            }
            _save =
                ImageVector.Builder(
                    name = "save",
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
                            pathFillType = PathFillType.Companion.NonZero,
                        ) {
                            moveTo(5f, 21f)
                            quadTo(4.18f, 21f, 3.59f, 20.41f)
                            reflectiveQuadTo(3f, 19f)
                            verticalLineTo(5f)
                            quadTo(3f, 4.17f, 3.59f, 3.59f)
                            reflectiveQuadTo(5f, 3f)
                            horizontalLineTo(16.18f)
                            quadToRelative(0.4f, 0f, 0.76f, 0.15f)
                            reflectiveQuadToRelative(0.64f, 0.43f)
                            lineToRelative(2.85f, 2.85f)
                            quadTo(20.7f, 6.7f, 20.85f, 7.06f)
                            reflectiveQuadTo(21f, 7.82f)
                            verticalLineTo(19f)
                            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                            reflectiveQuadTo(19f, 21f)
                            horizontalLineTo(5f)
                            close()
                            moveTo(19f, 7.85f)
                            lineTo(16.15f, 5f)
                            horizontalLineTo(5f)
                            verticalLineTo(19f)
                            horizontalLineTo(19f)
                            verticalLineTo(7.85f)
                            close()
                            moveToRelative(-4.88f, 9.28f)
                            quadTo(15f, 16.25f, 15f, 15f)
                            reflectiveQuadTo(14.13f, 12.88f)
                            reflectiveQuadTo(12f, 12f)
                            reflectiveQuadTo(9.88f, 12.88f)
                            reflectiveQuadTo(9f, 15f)
                            reflectiveQuadToRelative(0.88f, 2.13f)
                            reflectiveQuadTo(12f, 18f)
                            reflectiveQuadToRelative(2.13f, -0.88f)
                            close()
                            moveTo(7f, 10f)
                            horizontalLineToRelative(7f)
                            quadToRelative(0.43f, 0f, 0.71f, -0.29f)
                            reflectiveQuadTo(15f, 9f)
                            verticalLineTo(7f)
                            quadTo(15f, 6.57f, 14.71f, 6.29f)
                            reflectiveQuadTo(14f, 6f)
                            horizontalLineTo(7f)
                            quadTo(6.58f, 6f, 6.29f, 6.29f)
                            reflectiveQuadTo(6f, 7f)
                            verticalLineTo(9f)
                            quadTo(6f, 9.42f, 6.29f, 9.71f)
                            reflectiveQuadTo(7f, 10f)
                            close()
                            moveTo(5f, 7.85f)
                            verticalLineTo(19f)
                            verticalLineTo(5f)
                            verticalLineTo(7.85f)
                            close()
                        }
                    }
                    .build()
            return _save!!
        }

    private var _save: ImageVector? = null

    override val Incoming: ImageVector
        get() = InputCircle

    val InputCircle: ImageVector
        get() {
            if (_input_circle != null) {
                return _input_circle!!
            }
            _input_circle =
                ImageVector.Builder(
                    name = "input_circle",
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
                            pathFillType = PathFillType.Companion.NonZero,
                        ) {
                            moveTo(12f, 8f)
                            lineTo(7f, 13f)
                            lineToRelative(1.4f, 1.4f)
                            lineTo(11f, 11.83f)
                            verticalLineTo(22f)
                            horizontalLineToRelative(2f)
                            verticalLineTo(11.83f)
                            lineToRelative(2.6f, 2.57f)
                            lineTo(17f, 13f)
                            lineTo(12f, 8f)
                            close()
                            moveTo(3.65f, 17.5f)
                            quadTo(2.85f, 16.27f, 2.43f, 14.88f)
                            reflectiveQuadTo(2f, 12f)
                            quadTo(2f, 9.92f, 2.79f, 8.1f)
                            quadTo(3.58f, 6.27f, 4.93f, 4.93f)
                            quadTo(6.28f, 3.57f, 8.1f, 2.79f)
                            quadTo(9.93f, 2f, 12f, 2f)
                            reflectiveQuadToRelative(3.9f, 0.79f)
                            reflectiveQuadToRelative(3.17f, 2.14f)
                            quadToRelative(1.35f, 1.35f, 2.14f, 3.17f)
                            quadTo(22f, 9.92f, 22f, 12f)
                            quadToRelative(0f, 1.47f, -0.42f, 2.88f)
                            reflectiveQuadTo(20.35f, 17.5f)
                            lineTo(18.9f, 16.05f)
                            quadToRelative(0.55f, -0.93f, 0.82f, -1.95f)
                            reflectiveQuadTo(20f, 12f)
                            quadTo(20f, 8.65f, 17.68f, 6.32f)
                            reflectiveQuadTo(12f, 4f)
                            reflectiveQuadTo(6.33f, 6.32f)
                            reflectiveQuadTo(4f, 12f)
                            quadToRelative(0f, 1.07f, 0.28f, 2.1f)
                            reflectiveQuadTo(5.1f, 16.05f)
                            lineTo(3.65f, 17.5f)
                            close()
                        }
                    }
                    .build()
            return _input_circle!!
        }

    private var _input_circle: ImageVector? = null

    override val Outgoing: ImageVector
        get() = OutputCircle
    val OutputCircle: ImageVector
        get() {
            if (_output_circle != null) {
                return _output_circle!!
            }
            _output_circle =
                ImageVector.Builder(
                    name = "output_circle",
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
                            pathFillType = PathFillType.Companion.NonZero,
                        ) {
                            moveTo(12f, 22f)
                            lineTo(7f, 17f)
                            lineTo(8.4f, 15.6f)
                            lineTo(11f, 18.18f)
                            verticalLineTo(8f)
                            horizontalLineToRelative(2f)
                            verticalLineTo(18.18f)
                            lineTo(15.6f, 15.6f)
                            lineTo(17f, 17f)
                            lineToRelative(-5f, 5f)
                            close()
                            moveTo(3.65f, 17.5f)
                            quadTo(2.85f, 16.27f, 2.43f, 14.88f)
                            reflectiveQuadTo(2f, 12f)
                            quadTo(2f, 9.92f, 2.79f, 8.1f)
                            quadTo(3.58f, 6.27f, 4.93f, 4.93f)
                            quadTo(6.28f, 3.57f, 8.1f, 2.79f)
                            quadTo(9.93f, 2f, 12f, 2f)
                            reflectiveQuadToRelative(3.9f, 0.79f)
                            reflectiveQuadToRelative(3.17f, 2.14f)
                            quadToRelative(1.35f, 1.35f, 2.14f, 3.17f)
                            quadTo(22f, 9.92f, 22f, 12f)
                            quadToRelative(0f, 1.47f, -0.42f, 2.88f)
                            reflectiveQuadTo(20.35f, 17.5f)
                            lineTo(18.9f, 16.05f)
                            quadToRelative(0.55f, -0.93f, 0.82f, -1.95f)
                            reflectiveQuadTo(20f, 12f)
                            quadTo(20f, 8.65f, 17.68f, 6.32f)
                            reflectiveQuadTo(12f, 4f)
                            reflectiveQuadTo(6.33f, 6.32f)
                            reflectiveQuadTo(4f, 12f)
                            quadToRelative(0f, 1.07f, 0.28f, 2.1f)
                            reflectiveQuadTo(5.1f, 16.05f)
                            lineTo(3.65f, 17.5f)
                            close()
                        }
                    }
                    .build()
            return _output_circle!!
        }

    private var _output_circle: ImageVector? = null

    override val BoltCircle: ImageVector
        get() = Charger

    val Charger: ImageVector
        get() {
            if (_charger != null) {
                return _charger!!
            }
            _charger =
                ImageVector.Builder(
                    name = "charger",
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
                            pathFillType = PathFillType.Companion.NonZero,
                        ) {
                            moveTo(11.3f, 20f)
                            lineToRelative(5f, -9.75f)
                            horizontalLineTo(12.8f)
                            verticalLineTo(4f)
                            lineToRelative(-5f, 9.75f)
                            horizontalLineToRelative(3.5f)
                            verticalLineTo(20f)
                            close()
                            moveTo(12f, 22f)
                            quadTo(9.93f, 22f, 8.1f, 21.21f)
                            quadTo(6.28f, 20.43f, 4.93f, 19.08f)
                            quadTo(3.58f, 17.73f, 2.79f, 15.9f)
                            reflectiveQuadTo(2f, 12f)
                            quadTo(2f, 9.92f, 2.79f, 8.1f)
                            quadTo(3.58f, 6.27f, 4.93f, 4.93f)
                            quadTo(6.28f, 3.57f, 8.1f, 2.79f)
                            quadTo(9.93f, 2f, 12f, 2f)
                            reflectiveQuadToRelative(3.9f, 0.79f)
                            reflectiveQuadToRelative(3.17f, 2.14f)
                            quadToRelative(1.35f, 1.35f, 2.14f, 3.17f)
                            quadTo(22f, 9.92f, 22f, 12f)
                            reflectiveQuadToRelative(-0.79f, 3.9f)
                            reflectiveQuadToRelative(-2.14f, 3.17f)
                            quadToRelative(-1.35f, 1.35f, -3.17f, 2.14f)
                            reflectiveQuadTo(12f, 22f)
                            close()
                            moveTo(12f, 12f)
                            close()
                            moveToRelative(5.66f, 5.66f)
                            quadTo(20f, 15.33f, 20f, 12f)
                            quadTo(20f, 8.67f, 17.66f, 6.34f)
                            reflectiveQuadTo(12f, 4f)
                            quadTo(8.68f, 4f, 6.34f, 6.34f)
                            reflectiveQuadTo(4f, 12f)
                            reflectiveQuadToRelative(2.34f, 5.66f)
                            reflectiveQuadTo(12f, 20f)
                            reflectiveQuadToRelative(5.66f, -2.34f)
                            close()
                        }
                    }
                    .build()
            return _charger!!
        }

    private var _charger: ImageVector? = null

    override val Circle: ImageVector
        get() {
            if (_circle != null) {
                return _circle!!
            }
            _circle =
                ImageVector.Builder(
                    name = "circle",
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
                            pathFillType = PathFillType.Companion.NonZero,
                        ) {
                            moveTo(12f, 22f)
                            quadTo(9.93f, 22f, 8.1f, 21.21f)
                            quadTo(6.28f, 20.43f, 4.93f, 19.08f)
                            quadTo(3.58f, 17.73f, 2.79f, 15.9f)
                            reflectiveQuadTo(2f, 12f)
                            quadTo(2f, 9.92f, 2.79f, 8.1f)
                            quadTo(3.58f, 6.27f, 4.93f, 4.93f)
                            quadTo(6.28f, 3.57f, 8.1f, 2.79f)
                            quadTo(9.93f, 2f, 12f, 2f)
                            reflectiveQuadToRelative(3.9f, 0.79f)
                            reflectiveQuadToRelative(3.17f, 2.14f)
                            quadToRelative(1.35f, 1.35f, 2.14f, 3.17f)
                            quadTo(22f, 9.92f, 22f, 12f)
                            reflectiveQuadToRelative(-0.79f, 3.9f)
                            reflectiveQuadToRelative(-2.14f, 3.17f)
                            quadToRelative(-1.35f, 1.35f, -3.17f, 2.14f)
                            reflectiveQuadTo(12f, 22f)
                            close()
                            moveToRelative(0f, -2f)
                            quadToRelative(3.35f, 0f, 5.68f, -2.32f)
                            reflectiveQuadTo(20f, 12f)
                            reflectiveQuadTo(17.68f, 6.32f)
                            reflectiveQuadTo(12f, 4f)
                            reflectiveQuadTo(6.33f, 6.32f)
                            reflectiveQuadTo(4f, 12f)
                            reflectiveQuadToRelative(2.33f, 5.68f)
                            reflectiveQuadTo(12f, 20f)
                            close()
                            moveToRelative(0f, -8f)
                            close()
                        }
                    }
                    .build()
            return _circle!!
        }

    private var _circle: ImageVector? = null

    override val CircleDotted: ImageVector
        get() = CommonIcons.CircleDotted

    override val VitalSigns: ImageVector
        get() {
            if (_vital_signs != null) {
                return _vital_signs!!
            }
            _vital_signs =
                ImageVector.Builder(
                    name = "vital_signs",
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
                            pathFillType = PathFillType.Companion.NonZero,
                        ) {
                            moveTo(8.15f, 19.73f)
                            quadTo(7.78f, 19.45f, 7.6f, 19.02f)
                            lineTo(5.3f, 13f)
                            horizontalLineTo(2f)
                            quadTo(1.58f, 13f, 1.29f, 12.71f)
                            quadTo(1f, 12.43f, 1f, 12f)
                            reflectiveQuadTo(1.29f, 11.29f)
                            reflectiveQuadTo(2f, 11f)
                            horizontalLineTo(6f)
                            quadToRelative(0.33f, 0f, 0.56f, 0.17f)
                            reflectiveQuadToRelative(0.36f, 0.47f)
                            lineTo(9f, 17.1f)
                            lineTo(13.6f, 4.97f)
                            quadToRelative(0.17f, -0.43f, 0.55f, -0.7f)
                            reflectiveQuadTo(15f, 4f)
                            reflectiveQuadToRelative(0.85f, 0.27f)
                            reflectiveQuadToRelative(0.55f, 0.7f)
                            lineTo(18.7f, 11f)
                            horizontalLineTo(22f)
                            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                            reflectiveQuadTo(23f, 12f)
                            reflectiveQuadToRelative(-0.29f, 0.71f)
                            reflectiveQuadTo(22f, 13f)
                            horizontalLineTo(18f)
                            quadToRelative(-0.32f, 0f, -0.56f, -0.18f)
                            reflectiveQuadTo(17.08f, 12.35f)
                            lineTo(15f, 6.9f)
                            lineTo(10.4f, 19.02f)
                            quadToRelative(-0.17f, 0.43f, -0.55f, 0.7f)
                            reflectiveQuadTo(9f, 20f)
                            reflectiveQuadTo(8.15f, 19.73f)
                            close()
                        }
                    }
                    .build()
            return _vital_signs!!
        }

    private var _vital_signs: ImageVector? = null
}
