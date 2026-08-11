@file:Suppress("PrivatePropertyName")

package me.kavishdevar.librepods.presentation.icons


import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.icons.common.Bluetooth
import me.kavishdevar.librepods.presentation.icons.common.CircleDotted
import me.kavishdevar.librepods.presentation.icons.common.LeftCircleFill
import me.kavishdevar.librepods.presentation.icons.common.RightCircleFill
object AppleIcons: IconSet {
    // Material Icons don't scale like Apple's, we scale those up
    fun isMaterialIcon(name: String): Boolean {
        return when (name) {
            "Bluetooth" -> true
            else -> false
        }
    }

    override val Notifications: ImageVector
        get() = Bell

    override val Headphones: ImageVector
        get() {
            val current = _headphones
            if (current != null) return current

            return ImageVector.Builder(
                name = "Headphones",
                defaultWidth = 63.8129997253418.dp,
                defaultHeight = 66.28099822998047.dp,
                viewportWidth = 63.813f,
                viewportHeight = 66.281f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFFFFFFFF)),
                    fillAlpha = 0.85f,
                ) {
                    moveTo(x = 0.0f, y = 34.22f)
                    curveTo(x1 = 0.0f, y1 = 43.62f, x2 = 2.72f, y2 = 54.0f, x3 = 7.31f, y3 = 62.06f)
                    curveToRelative(dx1 = 0.78f, dy1 = 1.35f, dx2 = 2.25f, dy2 = 1.72f, dx3 = 3.66f, dy3 = 0.94f)
                    curveToRelative(dx1 = 1.31f, dy1 = -0.72f, dx2 = 1.69f, dy2 = -2.19f, dx3 = 0.87f, dy3 = -3.66f)
                    arcToRelative(a = 53.7f, b = 53.7f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -6.53f, dy1 = -25.12f)
                    curveToRelative(dx1 = 0.0f, dy1 = -17.34f, dx2 = 10.6f, dy2 = -28.9f, dx3 = 26.5f, dy3 = -28.9f)
                    curveToRelative(dx1 = 15.88f, dy1 = 0.0f, dx2 = 26.5f, dy2 = 11.56f, dx3 = 26.5f, dy3 = 28.9f)
                    curveToRelative(dx1 = 0.0f, dy1 = 8.4f, dx2 = -2.37f, dy2 = 17.5f, dx3 = -6.56f, dy3 = 25.12f)
                    curveToRelative(dx1 = -0.81f, dy1 = 1.47f, dx2 = -0.44f, dy2 = 2.94f, dx3 = 0.88f, dy3 = 3.66f)
                    curveToRelative(dx1 = 1.4f, dy1 = 0.78f, dx2 = 2.9f, dy2 = 0.4f, dx3 = 3.65f, dy3 = -0.94f)
                    arcToRelative(a = 58.7f, b = 58.7f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 7.34f, dy1 = -27.84f)
                    curveTo(x1 = 63.63f, y1 = 13.66f, x2 = 50.95f, y2 = 0.0f, x3 = 31.83f, y3 = 0.0f)
                    curveTo(x1 = 12.65f, y1 = 0.0f, x2 = 0.0f, y2 = 13.66f, x3 = 0.0f, y3 = 34.22f)
                    moveToRelative(dx = 9.78f, dy = 27.06f)
                    curveToRelative(dx1 = 1.03f, dy1 = 3.6f, dx2 = 4.1f, dy2 = 5.25f, dx3 = 7.72f, dy3 = 4.22f)
                    curveToRelative(dx1 = 3.6f, dy1 = -1.03f, dx2 = 5.28f, dy2 = -4.16f, dx3 = 4.22f, dy3 = -7.75f)
                    lineTo(x = 17.25f, y = 42.5f)
                    curveToRelative(dx1 = -1.03f, dy1 = -3.56f, dx2 = -4.1f, dy2 = -5.25f, dx3 = -7.72f, dy3 = -4.22f)
                    curveToRelative(dx1 = -3.6f, dy1 = 1.06f, dx2 = -5.28f, dy2 = 4.16f, dx3 = -4.22f, dy3 = 7.78f)
                    close()
                    moveToRelative(dx = 44.03f, dy = 0.0f)
                    lineToRelative(dx = 4.47f, dy = -15.22f)
                    curveToRelative(dx1 = 1.06f, dy1 = -3.65f, dx2 = -0.6f, dy2 = -6.72f, dx3 = -4.22f, dy3 = -7.78f)
                    curveToRelative(dx1 = -3.62f, dy1 = -1.03f, dx2 = -6.65f, dy2 = 0.66f, dx3 = -7.72f, dy3 = 4.22f)
                    lineToRelative(dx = -4.47f, dy = 15.25f)
                    curveToRelative(dx1 = -1.06f, dy1 = 3.63f, dx2 = 0.63f, dy2 = 6.72f, dx3 = 4.22f, dy3 = 7.75f)
                    curveToRelative(dx1 = 3.66f, dy1 = 1.03f, dx2 = 6.7f, dy2 = -0.62f, dx3 = 7.72f, dy3 = -4.22f)
                }
            }.build().also { _headphones = it }
        }

    @Suppress("ObjectPropertyName")
    private var _headphones: ImageVector? = null

    override val Play: ImageVector
        get() = PlayFill

    override val Pause: ImageVector
        get() = PauseFill

    override val Bluetooth: ImageVector
        get() = CommonIcons.Bluetooth

    override val Call: ImageVector
        get() = Phone

    override val Overlay: ImageVector
        get() = RectangleOnRectangleDashed

    override val ArrowBack: ImageVector
        get() = ChevronLeft

    override val LeftCircleFill: ImageVector
        get() = CommonIcons.LeftCircleFill

    override val RightCircleFill: ImageVector
        get() = CommonIcons.RightCircleFill

    override val Settings: ImageVector
        get() = Gear

    override val Send: ImageVector
        get() = PaperplaneFill

    override val Close: ImageVector
        get() = XMark

    override val CloseCircle: ImageVector
        get() = XMarkCircleFill

    override val SpeakerMin: ImageVector
        get() = SpeakerFill

    override val SpeakerMax: ImageVector
        get() = SpeakerWave3Fill

    override val Bolt: ImageVector
        get() = BoltFill

    override val Check: ImageVector
        get() = Checkmark

    override val Save: ImageVector
        get() = SquareAndArrowDown

    override val Incoming: ImageVector
        get() = SquareAndArrowDown

    override val Outgoing: ImageVector
        get() = SquareAndArrowUp

    // SF names

    override val ChevronLeft: ImageVector
        get() {
            val current = _chevron_left
            if (current != null) return current

            return ImageVector.Builder(
                name = "chevron_left",
                defaultWidth = 38.6879997253418.dp,
                defaultHeight = 54.28099822998047.dp,
                viewportWidth = 38.688f,
                viewportHeight = 54.281f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFFFFFFFF)),
                    fillAlpha = 0.85f,
                ) {
                    moveTo(x = 0.0f, y = 27.13f)
                    arcToRelative(a = 2.8f, b = 2.8f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 0.88f, dy1 = 2.06f)
                    lineTo(x = 25.66f, y = 53.4f)
                    arcToRelative(a = 2.7f, b = 2.7f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 2.03f, dy1 = 0.84f)
                    arcToRelative(a = 2.8f, b = 2.8f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 2.87f, dy1 = -2.84f)
                    curveToRelative(dx1 = 0.0f, dy1 = -0.82f, dx2 = -0.34f, dy2 = -1.5f, dx3 = -0.84f, dy3 = -2.03f)
                    lineTo(x = 6.97f, y = 27.12f)
                    lineTo(x = 29.72f, y = 4.89f)
                    arcToRelative(a = 3.0f, b = 3.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 0.84f, dy1 = -2.04f)
                    arcTo(horizontalEllipseRadius = 2.8f, verticalEllipseRadius = 2.8f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 27.7f, y1 = 0.0f)
                    arcToRelative(a = 2.8f, b = 2.8f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -2.03f, dy1 = 0.81f)
                    lineTo(x = 0.88f, y = 25.06f)
                    arcTo(horizontalEllipseRadius = 2.8f, verticalEllipseRadius = 2.8f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 0.0f, y1 = 27.13f)
                }
            }.build().also { _chevron_left = it }
        }

    private var _chevron_left: ImageVector? = null

    override val ChevronRight: ImageVector
        get() {
            val current = _chevronRight
            if (current != null) return current

            return ImageVector.Builder(
                name = "ChevronRight",
                defaultWidth = 38.375.dp,
                defaultHeight = 54.28099822998047.dp,
                viewportWidth = 38.375f,
                viewportHeight = 54.281f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFFFFFFFF)),
                    fillAlpha = 0.85f,
                ) {
                    moveTo(x = 38.38f, y = 27.13f)
                    arcToRelative(a = 2.8f, b = 2.8f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -0.91f, dy1 = -2.07f)
                    lineTo(x = 12.72f, y = 0.81f)
                    arcTo(horizontalEllipseRadius = 3.0f, verticalEllipseRadius = 3.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 10.66f, y1 = 0.0f)
                    arcTo(horizontalEllipseRadius = 2.8f, verticalEllipseRadius = 2.8f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 7.8f, y1 = 2.84f)
                    curveToRelative(dx1 = 0.0f, dy1 = 0.79f, dx2 = 0.32f, dy2 = 1.5f, dx3 = 0.82f, dy3 = 2.04f)
                    lineToRelative(dx = 22.74f, dy = 22.25f)
                    lineTo(x = 8.64f, y = 49.38f)
                    arcTo(horizontalEllipseRadius = 3.0f, verticalEllipseRadius = 3.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 7.8f, y1 = 51.4f)
                    arcToRelative(a = 2.8f, b = 2.8f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 2.85f, dy1 = 2.84f)
                    arcToRelative(a = 2.8f, b = 2.8f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 2.06f, dy1 = -0.84f)
                    lineToRelative(dx = 24.75f, dy = -24.22f)
                    arcToRelative(a = 2.9f, b = 2.9f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 0.9f, dy1 = -2.07f)
                }
            }.build().also { _chevronRight = it }
        }

    private var _chevronRight: ImageVector? = null

    val Bell: ImageVector
        get() {
            val current = _bell
            if (current != null) return current

            return ImageVector.Builder(
                name = "Bell",
                defaultWidth = 59.15599822998047.dp,
                defaultHeight = 64.59400177001953.dp,
                viewportWidth = 59.156f,
                viewportHeight = 64.594f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFFFFFFFF)),
                    fillAlpha = 0.85f,
                ) {
                    moveTo(x = 0.0f, y = 49.47f)
                    curveToRelative(dx1 = 0.0f, dy1 = 2.28f, dx2 = 1.75f, dy2 = 3.78f, dx3 = 4.72f, dy3 = 3.78f)
                    horizontalLineTo(x = 17.9f)
                    curveToRelative(dx1 = 0.25f, dy1 = 6.03f, dx2 = 4.97f, dy2 = 11.31f, dx3 = 11.56f, dy3 = 11.31f)
                    curveToRelative(dx1 = 6.62f, dy1 = 0.0f, dx2 = 11.34f, dy2 = -5.25f, dx3 = 11.6f, dy3 = -11.31f)
                    horizontalLineToRelative(dx = 13.18f)
                    curveToRelative(dx1 = 2.94f, dy1 = 0.0f, dx2 = 4.72f, dy2 = -1.5f, dx3 = 4.72f, dy3 = -3.78f)
                    curveToRelative(dx1 = 0.0f, dy1 = -3.13f, dx2 = -3.19f, dy2 = -5.94f, dx3 = -5.88f, dy3 = -8.72f)
                    curveToRelative(dx1 = -2.06f, dy1 = -2.16f, dx2 = -2.62f, dy2 = -6.6f, dx3 = -2.87f, dy3 = -10.19f)
                    curveTo(x1 = 50.0f, y1 = 18.25f, x2 = 46.82f, y2 = 10.31f, x3 = 38.5f, y3 = 7.31f)
                    curveTo(x1 = 37.44f, y1 = 3.21f, x2 = 34.1f, y2 = 0.0f, x3 = 29.47f, y3 = 0.0f)
                    curveToRelative(dx1 = -4.6f, dy1 = 0.0f, dx2 = -7.97f, dy2 = 3.22f, dx3 = -9.0f, dy3 = 7.31f)
                    curveToRelative(dx1 = -8.31f, dy1 = 3.0f, dx2 = -11.5f, dy2 = 10.94f, dx3 = -11.72f, dy3 = 23.25f)
                    curveToRelative(dx1 = -0.25f, dy1 = 3.6f, dx2 = -0.81f, dy2 = 8.03f, dx3 = -2.87f, dy3 = 10.19f)
                    curveTo(x1 = 3.16f, y1 = 43.53f, x2 = 0.0f, y2 = 46.35f, x3 = 0.0f, y3 = 49.47f)
                    moveToRelative(dx = 6.06f, dy = -0.94f)
                    verticalLineToRelative(dy = -0.37f)
                    curveToRelative(dx1 = 0.57f, dy1 = -0.91f, dx2 = 2.44f, dy2 = -2.75f, dx3 = 4.07f, dy3 = -4.57f)
                    curveToRelative(dx1 = 2.24f, dy1 = -2.5f, dx2 = 3.3f, dy2 = -6.53f, dx3 = 3.59f, dy3 = -12.62f)
                    curveToRelative(dx1 = 0.25f, dy1 = -13.66f, dx2 = 4.31f, dy2 = -18.0f, dx3 = 9.65f, dy3 = -19.47f)
                    curveToRelative(dx1 = 0.79f, dy1 = -0.19f, dx2 = 1.22f, dy2 = -0.56f, dx3 = 1.25f, dy3 = -1.37f)
                    curveToRelative(dx1 = 0.1f, dy1 = -3.26f, dx2 = 1.97f, dy2 = -5.54f, dx3 = 4.85f, dy3 = -5.54f)
                    curveToRelative(dx1 = 2.9f, dy1 = 0.0f, dx2 = 4.75f, dy2 = 2.29f, dx3 = 4.87f, dy3 = 5.54f)
                    curveToRelative(dx1 = 0.03f, dy1 = 0.8f, dx2 = 0.44f, dy2 = 1.18f, dx3 = 1.22f, dy3 = 1.37f)
                    curveToRelative(dx1 = 5.38f, dy1 = 1.47f, dx2 = 9.44f, dy2 = 5.81f, dx3 = 9.69f, dy3 = 19.47f)
                    curveToRelative(dx1 = 0.28f, dy1 = 6.1f, dx2 = 1.34f, dy2 = 10.12f, dx3 = 3.56f, dy3 = 12.62f)
                    curveToRelative(dx1 = 1.66f, dy1 = 1.82f, dx2 = 3.5f, dy2 = 3.66f, dx3 = 4.06f, dy3 = 4.57f)
                    verticalLineToRelative(dy = 0.37f)
                    close()
                    moveToRelative(dx = 16.72f, dy = 4.72f)
                    horizontalLineToRelative(dx = 13.4f)
                    curveToRelative(dx1 = -0.24f, dy1 = 4.25f, dx2 = -2.93f, dy2 = 6.9f, dx3 = -6.71f, dy3 = 6.9f)
                    curveToRelative(dx1 = -3.75f, dy1 = 0.0f, dx2 = -6.47f, dy2 = -2.65f, dx3 = -6.69f, dy3 = -6.9f)
                }
            }.build().also { _bell = it }
        }

    private var _bell: ImageVector? = null

    val HeadphonesSlash: ImageVector
        get() {
            val current = _headphonesSlash
            if (current != null) return current

            return ImageVector.Builder(
                name = "HeadphonesSlash",
                defaultWidth = 72.60900115966797.dp,
                defaultHeight = 73.97699737548828.dp,
                viewportWidth = 72.609f,
                viewportHeight = 73.977f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFFFFFFFF)),
                    fillAlpha = 0.85f,
                ) {
                    moveTo(x = 12.63f, y = 23.69f)
                    arcTo(horizontalEllipseRadius = 34.0f, verticalEllipseRadius = 34.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 9.7f, y1 = 38.07f)
                    quadToRelative(dx1 = 0.0f, dy1 = 3.35f, dx2 = 0.49f, dy2 = 6.78f)
                    arcToRelative(a = 6.3f, b = 6.3f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 3.72f, dy1 = -2.72f)
                    curveToRelative(dx1 = 3.66f, dy1 = -1.03f, dx2 = 6.7f, dy2 = 0.66f, dx3 = 7.75f, dy3 = 4.22f)
                    lineToRelative(dx = 4.47f, dy = 15.25f)
                    curveToRelative(dx1 = 1.03f, dy1 = 3.6f, dx2 = -0.62f, dy2 = 6.72f, dx3 = -4.25f, dy3 = 7.75f)
                    curveToRelative(dx1 = -2.91f, dy1 = 0.83f, dx2 = -5.44f, dy2 = -0.08f, dx3 = -6.87f, dy3 = -2.34f)
                    arcToRelative(a = 2.5f, b = 2.5f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -3.31f, dy1 = -1.1f)
                    arcToRelative(a = 59.0f, b = 59.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -7.32f, dy1 = -27.84f)
                    curveToRelative(dx1 = 0.0f, dy1 = -7.04f, dx2 = 1.49f, dy2 = -13.27f, dx3 = 4.24f, dy3 = -18.4f)
                    close()
                    moveTo(x = 56.78f, y = 67.8f)
                    curveToRelative(dx1 = -1.48f, dy1 = 1.66f, dx2 = -3.74f, dy2 = 2.26f, dx3 = -6.3f, dy3 = 1.54f)
                    curveToRelative(dx1 = -3.6f, dy1 = -1.03f, dx2 = -5.28f, dy2 = -4.13f, dx3 = -4.22f, dy3 = -7.75f)
                    lineToRelative(dx = 0.97f, dy = -3.33f)
                    close()
                    moveTo(x = 68.0f, y = 38.07f)
                    curveToRelative(dx1 = 0.0f, dy1 = 6.6f, dx2 = -1.35f, dy2 = 13.67f, dx3 = -3.77f, dy3 = 20.07f)
                    lineTo(x = 51.2f, y = 45.11f)
                    curveToRelative(dx1 = 1.34f, dy1 = -2.7f, dx2 = 4.06f, dy2 = -3.88f, dx3 = 7.24f, dy3 = -2.98f)
                    arcToRelative(a = 6.3f, b = 6.3f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 3.75f, dy1 = 2.7f)
                    arcToRelative(a = 47.0f, b = 47.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 0.5f, dy1 = -6.76f)
                    curveToRelative(dx1 = 0.0f, dy1 = -17.35f, dx2 = -10.63f, dy2 = -28.91f, dx3 = -26.5f, dy3 = -28.91f)
                    curveToRelative(dx1 = -6.24f, dy1 = 0.0f, dx2 = -11.66f, dy2 = 1.78f, dx3 = -15.93f, dy3 = 5.0f)
                    lineToRelative(dx = -3.89f, dy = -3.88f)
                    curveTo(x1 = 21.6f, y1 = 6.14f, x2 = 28.35f, y2 = 3.85f, x3 = 36.2f, y3 = 3.85f)
                    curveTo(x1 = 55.32f, y1 = 3.85f, x2 = 68.0f, y2 = 17.5f, x3 = 68.0f, y3 = 38.07f)
                    moveToRelative(dx = -4.28f, dy = 31.56f)
                    arcToRelative(a = 2.41f, b = 2.41f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 3.4f, dy1 = -3.4f)
                    lineTo(x = 7.8f, y = 6.81f)
                    arcToRelative(a = 2.4f, b = 2.4f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -3.44f, dy1 = 0.0f)
                    arcToRelative(a = 2.44f, b = 2.44f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 0.0f, dy1 = 3.4f)
                    close()
                }
            }.build().also { _headphonesSlash = it }
        }

    private var _headphonesSlash: ImageVector? = null

    val PlayFill: ImageVector
        get() {
            val current = _playFill
            if (current != null) return current

            return ImageVector.Builder(
                name = "PlayFill",
                defaultWidth = 53.09400177001953.dp,
                defaultHeight = 52.53099822998047.dp,
                viewportWidth = 53.094f,
                viewportHeight = 52.531f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFFFFFFFF)),
                    fillAlpha = 0.85f,
                ) {
                    moveTo(x = 6.44f, y = 47.94f)
                    curveToRelative(dx1 = 0.0f, dy1 = 3.1f, dx2 = 1.78f, dy2 = 4.56f, dx3 = 3.9f, dy3 = 4.56f)
                    curveToRelative(dx1 = 0.94f, dy1 = 0.0f, dx2 = 1.91f, dy2 = -0.31f, dx3 = 2.88f, dy3 = -0.81f)
                    lineToRelative(dx = 36.4f, dy = -21.28f)
                    curveToRelative(dx1 = 2.6f, dy1 = -1.5f, dx2 = 3.47f, dy2 = -2.53f, dx3 = 3.47f, dy3 = -4.16f)
                    curveToRelative(dx1 = 0.0f, dy1 = -1.66f, dx2 = -0.87f, dy2 = -2.66f, dx3 = -3.47f, dy3 = -4.16f)
                    lineTo(x = 13.23f, y = 0.81f)
                    arcTo(horizontalEllipseRadius = 6.0f, verticalEllipseRadius = 6.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 10.34f, y1 = 0.0f)
                    curveToRelative(dx1 = -2.12f, dy1 = 0.0f, dx2 = -3.9f, dy2 = 1.47f, dx3 = -3.9f, dy3 = 4.56f)
                    close()
                }
            }.build().also { _playFill = it }
        }

    private var _playFill: ImageVector? = null

    val PauseFill: ImageVector
        get() {
            val current = _pauseFill
            if (current != null) return current

            return ImageVector.Builder(
                name = "PauseFill",
                defaultWidth = 38.3129997253418.dp,
                defaultHeight = 51.71900177001953.dp,
                viewportWidth = 38.313f,
                viewportHeight = 51.719f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFFFFFFFF)),
                    fillAlpha = 0.85f,
                ) {
                    moveTo(x = 4.16f, y = 51.69f)
                    horizontalLineToRelative(dx = 7.12f)
                    curveToRelative(dx1 = 2.72f, dy1 = 0.0f, dx2 = 4.16f, dy2 = -1.44f, dx3 = 4.16f, dy3 = -4.19f)
                    verticalLineTo(y = 4.16f)
                    curveTo(x1 = 15.44f, y1 = 1.28f, x2 = 14.0f, y2 = 0.0f, x3 = 11.28f, y3 = 0.0f)
                    horizontalLineTo(x = 4.16f)
                    curveTo(x1 = 1.44f, y1 = 0.0f, x2 = 0.0f, y2 = 1.4f, x3 = 0.0f, y3 = 4.16f)
                    verticalLineTo(y = 47.5f)
                    curveToRelative(dx1 = 0.0f, dy1 = 2.75f, dx2 = 1.44f, dy2 = 4.19f, dx3 = 4.16f, dy3 = 4.19f)
                    moveToRelative(dx = 22.72f, dy = 0.0f)
                    horizontalLineToRelative(dx = 7.09f)
                    curveToRelative(dx1 = 2.75f, dy1 = 0.0f, dx2 = 4.16f, dy2 = -1.44f, dx3 = 4.16f, dy3 = -4.19f)
                    verticalLineTo(y = 4.16f)
                    curveToRelative(dx1 = 0.0f, dy1 = -2.88f, dx2 = -1.41f, dy2 = -4.16f, dx3 = -4.16f, dy3 = -4.16f)
                    horizontalLineToRelative(dx = -7.1f)
                    curveToRelative(dx1 = -2.75f, dy1 = 0.0f, dx2 = -4.18f, dy2 = 1.4f, dx3 = -4.18f, dy3 = 4.16f)
                    verticalLineTo(y = 47.5f)
                    curveToRelative(dx1 = 0.0f, dy1 = 2.75f, dx2 = 1.43f, dy2 = 4.19f, dx3 = 4.18f, dy3 = 4.19f)
                }
            }.build().also { _pauseFill = it }
        }

    private var _pauseFill: ImageVector? = null

    val Phone: ImageVector
        get() {
            val current = _phone
            if (current != null) return current

            return ImageVector.Builder(
                name = "Phone",
                defaultWidth = 61.21900177001953.dp,
                defaultHeight = 61.03300094604492.dp,
                viewportWidth = 61.219f,
                viewportHeight = 61.033f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFFFFFFFF)),
                    fillAlpha = 0.85f,
                ) {
                    moveTo(x = 45.38f, y = 61.03f)
                    curveToRelative(dx1 = 5.43f, dy1 = 0.0f, dx2 = 9.03f, dy2 = -1.47f, dx3 = 12.18f, dy3 = -5.0f)
                    lineToRelative(dx = 0.72f, dy = -0.81f)
                    curveToRelative(dx1 = 1.88f, dy1 = -2.06f, dx2 = 2.75f, dy2 = -4.1f, dx3 = 2.75f, dy3 = -6.03f)
                    curveToRelative(dx1 = 0.0f, dy1 = -2.25f, dx2 = -1.28f, dy2 = -4.44f, dx3 = -4.06f, dy3 = -6.35f)
                    lineToRelative(dx = -7.84f, dy = -5.37f)
                    curveToRelative(dx1 = -2.41f, dy1 = -1.63f, dx2 = -4.44f, dy2 = -1.72f, dx3 = -7.44f, dy3 = -0.28f)
                    lineToRelative(dx = -4.85f, dy = 2.37f)
                    arcToRelative(a = 2.5f, b = 2.5f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2.65f, dy1 = -0.12f)
                    arcTo(horizontalEllipseRadius = 53.0f, verticalEllipseRadius = 53.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 27.0f, y1 = 33.38f)
                    arcToRelative(a = 41.0f, b = 41.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -5.72f, dy1 = -7.0f)
                    curveToRelative(dx1 = -0.4f, dy1 = -0.72f, dx2 = -0.34f, dy2 = -1.32f, dx3 = 0.19f, dy3 = -2.16f)
                    lineToRelative(dx = 2.81f, dy = -4.4f)
                    curveToRelative(dx1 = 1.22f, dy1 = -1.94f, dx2 = 1.6f, dy2 = -4.6f, dx3 = 0.03f, dy3 = -6.85f)
                    lineToRelative(dx = -6.18f, dy = -8.88f)
                    curveTo(x1 = 16.18f, y1 = 1.31f, x2 = 14.09f, y2 = 0.03f, x3 = 11.84f, y3 = 0.0f)
                    quadToRelative(dx1 = -2.9f, dy1 = -0.05f, dx2 = -6.06f, dy2 = 2.75f)
                    lineTo(x = 5.0f, y = 3.47f)
                    curveToRelative(dx1 = -3.53f, dy1 = 3.12f, dx2 = -5.0f, dy2 = 6.72f, dx3 = -5.0f, dy3 = 12.12f)
                    curveToRelative(dx1 = 0.0f, dy1 = 8.94f, dx2 = 5.53f, dy2 = 19.88f, dx3 = 15.56f, dy3 = 29.88f)
                    curveToRelative(dx1 = 9.97f, dy1 = 9.97f, dx2 = 20.88f, dy2 = 15.56f, dx3 = 29.82f, dy3 = 15.56f)
                    moveToRelative(dx = 0.03f, dy = -4.75f)
                    curveToRelative(dx1 = -7.97f, dy1 = 0.16f, dx2 = -18.16f, dy2 = -5.97f, dx3 = -26.25f, dy3 = -14.03f)
                    curveTo(x1 = 11.0f, y1 = 34.13f, x2 = 4.59f, y2 = 23.56f, x3 = 4.75f, y3 = 15.56f)
                    curveTo(x1 = 4.81f, y1 = 12.13f, x2 = 6.0f, y2 = 9.2f, x3 = 8.47f, y3 = 7.03f)
                    lineToRelative(dx = 0.56f, dy = -0.47f)
                    arcToRelative(a = 4.6f, b = 4.6f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2.85f, dy1 = -1.25f)
                    arcToRelative(a = 2.6f, b = 2.6f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2.34f, dy1 = 1.28f)
                    lineToRelative(dx = 5.62f, dy = 8.44f)
                    curveToRelative(dx1 = 0.54f, dy1 = 0.78f, dx2 = 0.5f, dy2 = 1.47f, dx3 = -0.12f, dy3 = 2.56f)
                    lineToRelative(dx = -3.13f, dy = 5.0f)
                    curveToRelative(dx1 = -1.37f, dy1 = 2.22f, dx2 = -1.12f, dy2 = 3.91f, dx3 = 0.25f, dy3 = 5.79f)
                    arcToRelative(a = 82.0f, b = 82.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 6.94f, dy1 = 8.25f)
                    arcToRelative(a = 79.0f, b = 79.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 8.5f, dy1 = 7.25f)
                    curveToRelative(dx1 = 1.88f, dy1 = 1.37f, dx2 = 3.56f, dy2 = 1.68f, dx3 = 6.47f, dy3 = 0.28f)
                    lineToRelative(dx = 5.25f, dy = -2.5f)
                    arcToRelative(a = 3.0f, b = 3.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 3.13f, dy1 = 0.25f)
                    lineToRelative(dx = 7.3f, dy = 4.9f)
                    arcToRelative(a = 2.6f, b = 2.6f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 1.29f, dy1 = 2.35f)
                    curveToRelative(dx1 = 0.0f, dy1 = 0.87f, dx2 = -0.44f, dy2 = 1.9f, dx3 = -1.25f, dy3 = 2.84f)
                    lineTo(x = 54.0f, y = 52.56f)
                    curveToRelative(dx1 = -2.16f, dy1 = 2.47f, dx2 = -5.12f, dy2 = 3.66f, dx3 = -8.6f, dy3 = 3.72f)
                }
            }.build().also { _phone = it }
        }

    private var _phone: ImageVector? = null

    val RectangleOnRectangleDashed: ImageVector
        get() {
            val current = _rectangleOnRectangleDashed
            if (current != null) return current

            return ImageVector.Builder(
                name = "RectangleOnRectangleDashed",
                defaultWidth = 77.93800354003906.dp,
                defaultHeight = 63.34400177001953.dp,
                viewportWidth = 77.938f,
                viewportHeight = 63.344f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFFFFFFFF)),
                    fillAlpha = 0.85f,
                ) {
                    moveTo(x = 5.03f, y = 38.9f)
                    curveToRelative(dx1 = 0.0f, dy1 = 3.26f, dx2 = 1.72f, dy2 = 4.91f, dx3 = 4.85f, dy3 = 4.91f)
                    horizontalLineToRelative(dx = 5.56f)
                    verticalLineToRelative(dy = 5.03f)
                    horizontalLineTo(x = 9.8f)
                    quadTo(x1 = 0.0f, y1 = 48.84f, x2 = 0.0f, y2 = 39.16f)
                    verticalLineToRelative(dy = -4.2f)
                    horizontalLineToRelative(dx = 5.03f)
                    close()
                    moveToRelative(dx = 0.0f, dy = -8.06f)
                    horizontalLineTo(x = 0.0f)
                    verticalLineTo(y = 18.0f)
                    horizontalLineToRelative(dx = 5.03f)
                    close()
                    moveTo(x = 62.28f, y = 9.7f)
                    verticalLineToRelative(dy = 4.78f)
                    horizontalLineToRelative(dx = -5.03f)
                    verticalLineToRelative(dy = -4.5f)
                    curveToRelative(dx1 = 0.0f, dy1 = -3.25f, dx2 = -1.75f, dy2 = -4.94f, dx3 = -4.87f, dy3 = -4.94f)
                    horizontalLineTo(x = 48.5f)
                    verticalLineTo(y = 0.0f)
                    horizontalLineToRelative(dx = 3.97f)
                    quadToRelative(dx1 = 9.8f, dy1 = 0.02f, dx2 = 9.81f, dy2 = 9.69f)
                    moveTo(x = 13.75f, y = 5.03f)
                    horizontalLineTo(x = 9.88f)
                    curveToRelative(dx1 = -3.13f, dy1 = 0.0f, dx2 = -4.85f, dy2 = 1.69f, dx3 = -4.85f, dy3 = 4.94f)
                    verticalLineToRelative(dy = 3.9f)
                    horizontalLineTo(x = 0.0f)
                    verticalLineTo(y = 9.7f)
                    quadTo(x1 = -0.02f, y1 = 0.0f, x2 = 9.81f, y2 = 0.0f)
                    horizontalLineToRelative(dx = 3.94f)
                    close()
                    moveToRelative(dx = 30.6f, dy = 0.0f)
                    horizontalLineTo(x = 33.18f)
                    verticalLineTo(y = 0.0f)
                    horizontalLineToRelative(dx = 11.15f)
                    close()
                    moveToRelative(dx = -15.29f, dy = 0.0f)
                    horizontalLineTo(x = 17.88f)
                    verticalLineTo(y = 0.0f)
                    horizontalLineToRelative(dx = 11.18f)
                    close()
                    moveToRelative(dx = -3.81f, dy = 58.28f)
                    horizontalLineToRelative(dx = 42.69f)
                    curveToRelative(dx1 = 6.5f, dy1 = 0.0f, dx2 = 9.81f, dy2 = -3.25f, dx3 = 9.81f, dy3 = -9.69f)
                    verticalLineTo(y = 24.17f)
                    curveToRelative(dx1 = 0.0f, dy1 = -6.44f, dx2 = -3.31f, dy2 = -9.7f, dx3 = -9.81f, dy3 = -9.7f)
                    horizontalLineTo(x = 25.25f)
                    curveToRelative(dx1 = -6.56f, dy1 = 0.0f, dx2 = -9.81f, dy2 = 3.26f, dx3 = -9.81f, dy3 = 9.7f)
                    verticalLineToRelative(dy = 29.47f)
                    quadToRelative(dx1 = -0.02f, dy1 = 9.68f, dx2 = 9.81f, dy2 = 9.68f)
                    moveToRelative(dx = 0.06f, dy = -5.03f)
                    curveToRelative(dx1 = -3.12f, dy1 = 0.0f, dx2 = -4.84f, dy2 = -1.66f, dx3 = -4.84f, dy3 = -4.9f)
                    verticalLineTo(y = 24.44f)
                    curveToRelative(dx1 = 0.0f, dy1 = -3.25f, dx2 = 1.72f, dy2 = -4.94f, dx3 = 4.84f, dy3 = -4.94f)
                    horizontalLineToRelative(dx = 42.53f)
                    curveToRelative(dx1 = 3.1f, dy1 = 0.0f, dx2 = 4.88f, dy2 = 1.69f, dx3 = 4.88f, dy3 = 4.94f)
                    verticalLineToRelative(dy = 28.94f)
                    curveToRelative(dx1 = 0.0f, dy1 = 3.24f, dx2 = -1.78f, dy2 = 4.9f, dx3 = -4.88f, dy3 = 4.9f)
                    close()
                }
            }.build().also { _rectangleOnRectangleDashed = it }
        }

    private var _rectangleOnRectangleDashed: ImageVector? = null

    val Gear: ImageVector
        get() {
            val current = _gear
            if (current != null) return current

            return ImageVector.Builder(
                name = "Gear",
                defaultWidth = 72.06300354003906.dp,
                defaultHeight = 71.84400177001953.dp,
                viewportWidth = 72.063f,
                viewportHeight = 71.844f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFFFFFFFF)),
                    fillAlpha = 0.85f,
                ) {
                    moveTo(x = 35.94f, y = 71.84f)
                    curveToRelative(dx1 = 0.81f, dy1 = 0.0f, dx2 = 1.47f, dy2 = -0.43f, dx3 = 1.69f, dy3 = -1.93f)
                    lineToRelative(dx = 0.3f, dy = -2.57f)
                    curveToRelative(dx1 = 0.2f, dy1 = -1.06f, dx2 = 0.82f, dy2 = -1.62f, dx3 = 1.95f, dy3 = -1.75f)
                    curveToRelative(dx1 = 1.09f, dy1 = -0.18f, dx2 = 1.8f, dy2 = 0.28f, dx3 = 2.21f, dy3 = 1.22f)
                    lineToRelative(dx = 0.97f, dy = 2.38f)
                    curveToRelative(dx1 = 0.63f, dy1 = 1.4f, dx2 = 1.38f, dy2 = 1.65f, dx3 = 2.16f, dy3 = 1.44f)
                    curveToRelative(dx1 = 0.78f, dy1 = -0.25f, dx2 = 1.31f, dy2 = -0.82f, dx3 = 1.1f, dy3 = -2.35f)
                    lineToRelative(dx = -0.35f, dy = -2.5f)
                    curveToRelative(dx1 = -0.16f, dy1 = -1.1f, dx2 = 0.37f, dy2 = -1.75f, dx3 = 1.47f, dy3 = -2.25f)
                    curveToRelative(dx1 = 0.94f, dy1 = -0.4f, dx2 = 1.81f, dy2 = -0.28f, dx3 = 2.47f, dy3 = 0.6f)
                    lineToRelative(dx = 1.56f, dy = 2.03f)
                    curveToRelative(dx1 = 0.97f, dy1 = 1.28f, dx2 = 1.69f, dy2 = 1.3f, dx3 = 2.44f, dy3 = 0.84f)
                    curveToRelative(dx1 = 0.75f, dy1 = -0.4f, dx2 = 1.06f, dy2 = -1.06f, dx3 = 0.47f, dy3 = -2.5f)
                    lineToRelative(dx = -0.97f, dy = -2.34f)
                    curveToRelative(dx1 = -0.44f, dy1 = -1.07f, dx2 = -0.16f, dy2 = -1.88f, dx3 = 0.78f, dy3 = -2.53f)
                    curveToRelative(dx1 = 0.87f, dy1 = -0.63f, dx2 = 1.69f, dy2 = -0.76f, dx3 = 2.56f, dy3 = -0.07f)
                    lineToRelative(dx = 2.03f, dy = 1.56f)
                    curveToRelative(dx1 = 1.22f, dy1 = 0.94f, dx2 = 2.0f, dy2 = 0.82f, dx3 = 2.6f, dy3 = 0.22f)
                    curveToRelative(dx1 = 0.56f, dy1 = -0.62f, dx2 = 0.71f, dy2 = -1.37f, dx3 = -0.26f, dy3 = -2.56f)
                    lineToRelative(dx = -1.56f, dy = -2.03f)
                    quadToRelative(dx1 = -0.92f, dy1 = -1.28f, dx2 = 0.13f, dy2 = -2.66f)
                    curveToRelative(dx1 = 0.62f, dy1 = -0.8f, dx2 = 1.44f, dy2 = -1.12f, dx3 = 2.47f, dy3 = -0.68f)
                    lineToRelative(dx = 2.37f, dy = 0.97f)
                    curveToRelative(dx1 = 1.44f, dy1 = 0.59f, dx2 = 2.1f, dy2 = 0.28f, dx3 = 2.5f, dy3 = -0.5f)
                    curveToRelative(dx1 = 0.44f, dy1 = -0.72f, dx2 = 0.4f, dy2 = -1.44f, dx3 = -0.84f, dy3 = -2.41f)
                    lineToRelative(dx = -2.0f, dy = -1.56f)
                    curveToRelative(dx1 = -0.85f, dy1 = -0.7f, dx2 = -1.0f, dy2 = -1.57f, dx3 = -0.6f, dy3 = -2.53f)
                    curveToRelative(dx1 = 0.38f, dy1 = -1.0f, dx2 = 1.07f, dy2 = -1.47f, dx3 = 2.2f, dy3 = -1.38f)
                    lineToRelative(dx = 2.55f, dy = 0.34f)
                    curveToRelative(dx1 = 1.5f, dy1 = 0.2f, dx2 = 2.13f, dy2 = -0.3f, dx3 = 2.32f, dy3 = -1.12f)
                    curveToRelative(dx1 = 0.18f, dy1 = -0.78f, dx2 = -0.03f, dy2 = -1.53f, dx3 = -1.47f, dy3 = -2.13f)
                    lineToRelative(dx = -2.35f, dy = -0.97f)
                    curveToRelative(dx1 = -0.97f, dy1 = -0.37f, dx2 = -1.37f, dy2 = -1.12f, dx3 = -1.25f, dy3 = -2.34f)
                    curveToRelative(dx1 = 0.16f, dy1 = -1.03f, dx2 = 0.7f, dy2 = -1.69f, dx3 = 1.82f, dy3 = -1.84f)
                    lineToRelative(dx = 2.5f, dy = -0.31f)
                    curveToRelative(dx1 = 1.5f, dy1 = -0.22f, dx2 = 1.97f, dy2 = -0.88f, dx3 = 1.97f, dy3 = -1.7f)
                    curveToRelative(dx1 = 0.0f, dy1 = -0.84f, dx2 = -0.47f, dy2 = -1.46f, dx3 = -1.97f, dy3 = -1.68f)
                    lineToRelative(dx = -2.5f, dy = -0.31f)
                    curveToRelative(dx1 = -1.16f, dy1 = -0.19f, dx2 = -1.63f, dy2 = -0.85f, dx3 = -1.82f, dy3 = -1.94f)
                    curveToRelative(dx1 = -0.12f, dy1 = -1.06f, dx2 = 0.25f, dy2 = -1.81f, dx3 = 1.25f, dy3 = -2.25f)
                    lineToRelative(dx = 2.38f, dy = -0.94f)
                    curveToRelative(dx1 = 1.4f, dy1 = -0.62f, dx2 = 1.66f, dy2 = -1.37f, dx3 = 1.44f, dy3 = -2.15f)
                    curveToRelative(dx1 = -0.22f, dy1 = -0.79f, dx2 = -0.82f, dy2 = -1.32f, dx3 = -2.35f, dy3 = -1.13f)
                    lineToRelative(dx = -2.5f, dy = 0.34f)
                    arcToRelative(a = 1.97f, b = 1.97f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2.25f, dy1 = -1.37f)
                    curveToRelative(dx1 = -0.4f, dy1 = -1.1f, dx2 = -0.28f, dy2 = -1.84f, dx3 = 0.63f, dy3 = -2.53f)
                    lineToRelative(dx = 2.0f, dy = -1.56f)
                    curveToRelative(dx1 = 1.25f, dy1 = -0.97f, dx2 = 1.28f, dy2 = -1.7f, dx3 = 0.87f, dy3 = -2.41f)
                    curveToRelative(dx1 = -0.47f, dy1 = -0.81f, dx2 = -1.15f, dy2 = -1.06f, dx3 = -2.53f, dy3 = -0.53f)
                    lineToRelative(dx = -2.37f, dy = 1.03f)
                    curveToRelative(dx1 = -1.03f, dy1 = 0.44f, dx2 = -1.88f, dy2 = 0.06f, dx3 = -2.5f, dy3 = -0.78f)
                    reflectiveCurveToRelative(dx1 = -0.72f, dy1 = -1.69f, dx2 = -0.07f, dy2 = -2.6f)
                    lineToRelative(dx = 1.57f, dy = -2.0f)
                    curveToRelative(dx1 = 0.93f, dy1 = -1.21f, dx2 = 0.8f, dy2 = -2.0f, dx3 = 0.22f, dy3 = -2.59f)
                    curveToRelative(dx1 = -0.6f, dy1 = -0.56f, dx2 = -1.38f, dy2 = -0.72f, dx3 = -2.57f, dy3 = 0.22f)
                    lineToRelative(dx = -2.03f, dy = 1.6f)
                    curveToRelative(dx1 = -0.84f, dy1 = 0.68f, dx2 = -1.78f, dy2 = 0.53f, dx3 = -2.62f, dy3 = -0.13f)
                    curveToRelative(dx1 = -0.91f, dy1 = -0.75f, dx2 = -1.2f, dy2 = -1.44f, dx3 = -0.75f, dy3 = -2.5f)
                    lineToRelative(dx = 0.97f, dy = -2.38f)
                    curveToRelative(dx1 = 0.59f, dy1 = -1.43f, dx2 = 0.28f, dy2 = -2.06f, dx3 = -0.47f, dy3 = -2.5f)
                    curveToRelative(dx1 = -0.82f, dy1 = -0.46f, dx2 = -1.57f, dy2 = -0.28f, dx3 = -2.44f, dy3 = 0.85f)
                    lineTo(x = 49.9f, y = 7.72f)
                    curveToRelative(dx1 = -0.7f, dy1 = 0.84f, dx2 = -1.53f, dy2 = 1.0f, dx3 = -2.57f, dy3 = 0.56f)
                    curveToRelative(dx1 = -0.97f, dy1 = -0.37f, dx2 = -1.5f, dy2 = -1.06f, dx3 = -1.34f, dy3 = -2.19f)
                    lineToRelative(dx = 0.38f, dy = -2.53f)
                    curveToRelative(dx1 = 0.15f, dy1 = -1.5f, dx2 = -0.26f, dy2 = -2.1f, dx3 = -1.16f, dy3 = -2.31f)
                    curveToRelative(dx1 = -0.88f, dy1 = -0.22f, dx2 = -1.6f, dy2 = 0.16f, dx3 = -2.13f, dy3 = 1.44f)
                    lineToRelative(dx = -0.93f, dy = 2.37f)
                    curveToRelative(dx1 = -0.41f, dy1 = 1.0f, dx2 = -1.22f, dy2 = 1.44f, dx3 = -2.35f, dy3 = 1.25f)
                    curveTo(x1 = 38.7f, y1 = 6.13f, x2 = 38.1f, y2 = 5.6f, x3 = 37.97f, y3 = 4.5f)
                    lineToRelative(dx = -0.34f, dy = -2.53f)
                    curveToRelative(dx1 = -0.22f, dy1 = -1.5f, dx2 = -0.88f, dy2 = -1.94f, dx3 = -1.66f, dy3 = -1.94f)
                    curveToRelative(dx1 = -0.88f, dy1 = 0.0f, dx2 = -1.53f, dy2 = 0.44f, dx3 = -1.72f, dy3 = 1.9f)
                    lineToRelative(dx = -0.31f, dy = 2.6f)
                    curveTo(x1 = 33.75f, y1 = 5.56f, x2 = 33.16f, y2 = 6.2f, x3 = 32.0f, y3 = 6.28f)
                    quadToRelative(dx1 = -1.65f, dy1 = 0.2f, dx2 = -2.22f, dy2 = -1.22f)
                    lineTo(x = 28.81f, y = 2.7f)
                    curveTo(x1 = 28.25f, y1 = 1.44f, x2 = 27.56f, y2 = 1.0f, x3 = 26.66f, y3 = 1.25f)
                    curveToRelative(dx1 = -0.97f, dy1 = 0.25f, dx2 = -1.32f, dy2 = 0.97f, dx3 = -1.13f, dy3 = 2.34f)
                    lineToRelative(dx = 0.34f, dy = 2.5f)
                    curveToRelative(dx1 = 0.16f, dy1 = 1.13f, dx2 = -0.37f, dy2 = 1.85f, dx3 = -1.4f, dy3 = 2.22f)
                    curveToRelative(dx1 = -1.06f, dy1 = 0.4f, dx2 = -1.81f, dy2 = 0.25f, dx3 = -2.5f, dy3 = -0.6f)
                    lineTo(x = 20.4f, y = 5.7f)
                    curveTo(x1 = 19.5f, y1 = 4.53f, x2 = 18.78f, y2 = 4.4f, x3 = 18.0f, y3 = 4.84f)
                    curveToRelative(dx1 = -0.78f, dy1 = 0.47f, dx2 = -1.1f, dy2 = 1.07f, dx3 = -0.5f, dy3 = 2.5f)
                    lineToRelative(dx = 0.97f, dy = 2.38f)
                    quadToRelative(dx1 = 0.6f, dy1 = 1.5f, dx2 = -0.72f, dy2 = 2.5f)
                    curveToRelative(dx1 = -0.87f, dy1 = 0.65f, dx2 = -1.75f, dy2 = 0.75f, dx3 = -2.66f, dy3 = 0.1f)
                    lineToRelative(dx = -2.0f, dy = -1.6f)
                    curveToRelative(dx1 = -1.12f, dy1 = -0.85f, dx2 = -1.93f, dy2 = -0.85f, dx3 = -2.59f, dy3 = -0.19f)
                    curveToRelative(dx1 = -0.66f, dy1 = 0.69f, dx2 = -0.62f, dy2 = 1.47f, dx3 = 0.22f, dy3 = 2.56f)
                    lineToRelative(dx = 1.6f, dy = 2.0f)
                    curveToRelative(dx1 = 0.68f, dy1 = 0.88f, dx2 = 0.52f, dy2 = 1.85f, dx3 = -0.13f, dy3 = 2.66f)
                    curveToRelative(dx1 = -0.75f, dy1 = 0.84f, dx2 = -1.4f, dy2 = 1.16f, dx3 = -2.5f, dy3 = 0.72f)
                    lineTo(x = 7.34f, y = 17.5f)
                    curveToRelative(dx1 = -1.3f, dy1 = -0.53f, dx2 = -2.06f, dy2 = -0.34f, dx3 = -2.5f, dy3 = 0.47f)
                    curveToRelative(dx1 = -0.53f, dy1 = 0.84f, dx2 = -0.28f, dy2 = 1.6f, dx3 = 0.85f, dy3 = 2.44f)
                    lineToRelative(dx = 2.0f, dy = 1.56f)
                    curveToRelative(dx1 = 0.84f, dy1 = 0.69f, dx2 = 1.0f, dy2 = 1.6f, dx3 = 0.6f, dy3 = 2.53f)
                    curveToRelative(dx1 = -0.48f, dy1 = 1.0f, dx2 = -1.13f, dy2 = 1.47f, dx3 = -2.23f, dy3 = 1.34f)
                    lineTo(x = 3.53f, y = 25.5f)
                    curveToRelative(dx1 = -1.5f, dy1 = -0.16f, dx2 = -2.12f, dy2 = 0.34f, dx3 = -2.31f, dy3 = 1.16f)
                    curveToRelative(dx1 = -0.19f, dy1 = 0.78f, dx2 = 0.03f, dy2 = 1.53f, dx3 = 1.47f, dy3 = 2.12f)
                    lineToRelative(dx = 2.34f, dy = 0.94f)
                    curveToRelative(dx1 = 1.0f, dy1 = 0.47f, dx2 = 1.44f, dy2 = 1.22f, dx3 = 1.22f, dy3 = 2.31f)
                    curveToRelative(dx1 = -0.19f, dy1 = 1.1f, dx2 = -0.62f, dy2 = 1.72f, dx3 = -1.78f, dy3 = 1.9f)
                    lineToRelative(dx = -2.5f, dy = 0.32f)
                    curveTo(x1 = 0.44f, y1 = 34.47f, x2 = 0.0f, y2 = 35.09f, x3 = 0.0f, y3 = 35.94f)
                    curveToRelative(dx1 = 0.0f, dy1 = 0.81f, dx2 = 0.44f, dy2 = 1.47f, dx3 = 1.97f, dy3 = 1.69f)
                    lineToRelative(dx = 2.5f, dy = 0.3f)
                    curveToRelative(dx1 = 1.16f, dy1 = 0.2f, dx2 = 1.62f, dy2 = 0.82f, dx3 = 1.78f, dy3 = 1.88f)
                    reflectiveCurveTo(x1 = 6.03f, y1 = 41.7f, x2 = 5.03f, y2 = 42.1f)
                    lineToRelative(dx = -2.37f, dy = 0.97f)
                    curveTo(x1 = 1.25f, y1 = 43.7f, x2 = 1.0f, y2 = 44.44f, x3 = 1.22f, y3 = 45.22f)
                    reflectiveCurveToRelative(dx1 = 0.81f, dy1 = 1.31f, dx2 = 2.34f, dy2 = 1.1f)
                    lineToRelative(dx = 2.47f, dy = -0.35f)
                    curveToRelative(dx1 = 1.1f, dy1 = -0.13f, dx2 = 1.78f, dy2 = 0.37f, dx3 = 2.28f, dy3 = 1.44f)
                    curveToRelative(dx1 = 0.38f, dy1 = 0.93f, dx2 = 0.22f, dy2 = 1.84f, dx3 = -0.62f, dy3 = 2.5f)
                    lineToRelative(dx = -2.0f, dy = 1.56f)
                    curveToRelative(dx1 = -1.28f, dy1 = 0.97f, dx2 = -1.32f, dy2 = 1.69f, dx3 = -0.85f, dy3 = 2.44f)
                    curveToRelative(dx1 = 0.41f, dy1 = 0.75f, dx2 = 1.07f, dy2 = 1.06f, dx3 = 2.5f, dy3 = 0.5f)
                    lineToRelative(dx = 2.35f, dy = -1.03f)
                    curveToRelative(dx1 = 1.03f, dy1 = -0.44f, dx2 = 1.87f, dy2 = -0.04f, dx3 = 2.53f, dy3 = 0.78f)
                    curveToRelative(dx1 = 0.62f, dy1 = 0.78f, dx2 = 0.72f, dy2 = 1.68f, dx3 = 0.06f, dy3 = 2.56f)
                    lineToRelative(dx = -1.6f, dy = 2.03f)
                    curveToRelative(dx1 = -0.9f, dy1 = 1.22f, dx2 = -0.77f, dy2 = 1.97f, dx3 = -0.18f, dy3 = 2.6f)
                    curveToRelative(dx1 = 0.6f, dy1 = 0.56f, dx2 = 1.38f, dy2 = 0.71f, dx3 = 2.56f, dy3 = -0.26f)
                    lineToRelative(dx = 2.0f, dy = -1.56f)
                    curveToRelative(dx1 = 0.88f, dy1 = -0.66f, dx2 = 1.72f, dy2 = -0.53f, dx3 = 2.69f, dy3 = 0.13f)
                    curveToRelative(dx1 = 0.88f, dy1 = 0.65f, dx2 = 1.16f, dy2 = 1.47f, dx3 = 0.72f, dy3 = 2.5f)
                    lineTo(x = 17.5f, y = 64.5f)
                    curveToRelative(dx1 = -0.6f, dy1 = 1.44f, dx2 = -0.28f, dy2 = 2.1f, dx3 = 0.5f, dy3 = 2.5f)
                    curveToRelative(dx1 = 0.72f, dy1 = 0.47f, dx2 = 1.44f, dy2 = 0.44f, dx3 = 2.4f, dy3 = -0.84f)
                    lineToRelative(dx = 1.57f, dy = -2.0f)
                    curveToRelative(dx1 = 0.72f, dy1 = -0.91f, dx2 = 1.53f, dy2 = -1.03f, dx3 = 2.47f, dy3 = -0.63f)
                    curveToRelative(dx1 = 1.0f, dy1 = 0.4f, dx2 = 1.56f, dy2 = 1.13f, dx3 = 1.4f, dy3 = 2.25f)
                    lineToRelative(dx = -0.37f, dy = 2.53f)
                    curveToRelative(dx1 = -0.13f, dy1 = 1.5f, dx2 = 0.34f, dy2 = 2.1f, dx3 = 1.19f, dy3 = 2.31f)
                    curveToRelative(dx1 = 0.78f, dy1 = 0.2f, dx2 = 1.53f, dy2 = -0.03f, dx3 = 2.12f, dy3 = -1.43f)
                    lineToRelative(dx = 0.94f, dy = -2.38f)
                    curveToRelative(dx1 = 0.37f, dy1 = -0.94f, dx2 = 1.12f, dy2 = -1.37f, dx3 = 2.34f, dy3 = -1.25f)
                    curveToRelative(dx1 = 1.13f, dy1 = 0.13f, dx2 = 1.72f, dy2 = 0.72f, dx3 = 1.85f, dy3 = 1.81f)
                    lineToRelative(dx = 0.34f, dy = 2.5f)
                    curveToRelative(dx1 = 0.19f, dy1 = 1.54f, dx2 = 0.84f, dy2 = 1.97f, dx3 = 1.69f, dy3 = 1.97f)
                    moveTo(x = 17.8f, y = 54.2f)
                    arcToRelative(a = 25.3f, b = 25.3f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -7.72f, dy1 = -18.25f)
                    curveToRelative(dx1 = 0.0f, dy1 = -7.19f, dx2 = 2.94f, dy2 = -13.63f, dx3 = 7.72f, dy3 = -18.28f)
                    curveToRelative(dx1 = 1.9f, dy1 = -1.94f, dx2 = 3.97f, dy2 = -1.5f, dx3 = 5.35f, dy3 = 0.87f)
                    lineToRelative(dx = 8.47f, dy = 14.6f)
                    arcToRelative(a = 5.2f, b = 5.2f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -0.04f, dy1 = 5.68f)
                    lineToRelative(dx = -8.4f, dy = 14.5f)
                    curveToRelative(dx1 = -1.38f, dy1 = 2.4f, dx2 = -3.44f, dy2 = 2.81f, dx3 = -5.38f, dy3 = 0.88f)
                    moveToRelative(dx = 17.97f, dy = 7.4f)
                    curveToRelative(dx1 = -2.34f, dy1 = 0.0f, dx2 = -4.66f, dy2 = -0.34f, dx3 = -6.84f, dy3 = -0.97f)
                    curveToRelative(dx1 = -2.66f, dy1 = -0.71f, dx2 = -3.32f, dy2 = -2.71f, dx3 = -1.9f, dy3 = -5.12f)
                    lineTo(x = 35.4f, y = 41.0f)
                    arcToRelative(a = 5.1f, b = 5.1f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 4.97f, dy1 = -2.84f)
                    horizontalLineToRelative(dx = 16.71f)
                    curveToRelative(dx1 = 2.78f, dy1 = 0.0f, dx2 = 4.16f, dy2 = 1.56f, dx3 = 3.41f, dy3 = 4.22f)
                    arcToRelative(a = 25.5f, b = 25.5f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -24.72f, dy1 = 19.21f)
                    moveToRelative(dx = -0.1f, dy = -23.8f)
                    curveToRelative(dx1 = -1.0f, dy1 = 0.0f, dx2 = -1.77f, dy2 = -0.82f, dx3 = -1.77f, dy3 = -1.79f)
                    curveToRelative(dx1 = 0.0f, dy1 = -1.0f, dx2 = 0.78f, dy2 = -1.81f, dx3 = 1.78f, dy3 = -1.81f)
                    reflectiveCurveTo(x1 = 37.47f, y1 = 35.0f, x2 = 37.47f, y2 = 36.0f)
                    arcToRelative(a = 1.8f, b = 1.8f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -1.78f, dy1 = 1.78f)
                    moveToRelative(dx = 4.7f, dy = -4.07f)
                    curveToRelative(dx1 = -2.35f, dy1 = 0.0f, dx2 = -3.82f, dy2 = -0.88f, dx3 = -4.91f, dy3 = -2.81f)
                    lineTo(x = 27.0f, y = 16.3f)
                    curveToRelative(dx1 = -1.34f, dy1 = -2.37f, dx2 = -0.72f, dy2 = -4.37f, dx3 = 1.94f, dy3 = -5.1f)
                    arcTo(horizontalEllipseRadius = 25.42f, verticalEllipseRadius = 25.42f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 60.5f, y1 = 29.5f)
                    curveToRelative(dx1 = 0.75f, dy1 = 2.67f, dx2 = -0.62f, dy2 = 4.23f, dx3 = -3.37f, dy3 = 4.23f)
                    close()
                }
            }.build().also { _gear = it }
        }

    private var _gear: ImageVector? = null

    val PaperplaneFill: ImageVector
        get() {
            val current = _paperplaneFill
            if (current != null) return current

            return ImageVector.Builder(
                name = "PaperplaneFill",
                defaultWidth = 68.875.dp,
                defaultHeight = 68.46900177001953.dp,
                viewportWidth = 68.875f,
                viewportHeight = 68.469f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFFFFFFFF)),
                    fillAlpha = 0.85f,
                ) {
                    moveTo(x = 39.13f, y = 68.47f)
                    curveToRelative(dx1 = 2.24f, dy1 = 0.0f, dx2 = 3.84f, dy2 = -1.94f, dx3 = 5.0f, dy3 = -4.94f)
                    lineToRelative(dx = 20.46f, dy = -53.47f)
                    arcToRelative(a = 11.0f, b = 11.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 0.88f, dy1 = -3.78f)
                    curveToRelative(dx1 = 0.0f, dy1 = -2.03f, dx2 = -1.25f, dy2 = -3.28f, dx3 = -3.28f, dy3 = -3.28f)
                    curveToRelative(dx1 = -1.06f, dy1 = 0.0f, dx2 = -2.35f, dy2 = 0.31f, dx3 = -3.78f, dy3 = 0.88f)
                    lineTo(x = 4.66f, y = 24.47f)
                    curveToRelative(dx1 = -2.63f, dy1 = 1.0f, dx2 = -4.66f, dy2 = 2.6f, dx3 = -4.66f, dy3 = 4.87f)
                    curveToRelative(dx1 = 0.0f, dy1 = 2.88f, dx2 = 2.19f, dy2 = 3.85f, dx3 = 5.19f, dy3 = 4.75f)
                    lineToRelative(dx = 16.87f, dy = 5.13f)
                    curveToRelative(dx1 = 2.0f, dy1 = 0.62f, dx2 = 3.13f, dy2 = 0.56f, dx3 = 4.47f, dy3 = -0.69f)
                    lineTo(x = 60.81f, y = 6.5f)
                    curveToRelative(dx1 = 0.4f, dy1 = -0.37f, dx2 = 0.88f, dy2 = -0.31f, dx3 = 1.19f, dy3 = -0.03f)
                    curveToRelative(dx1 = 0.31f, dy1 = 0.31f, dx2 = 0.34f, dy2 = 0.78f, dx3 = -0.03f, dy3 = 1.19f)
                    lineToRelative(dx = -31.9f, dy = 34.4f)
                    curveToRelative(dx1 = -1.23f, dy1 = 1.28f, dx2 = -1.32f, dy2 = 2.35f, dx3 = -0.73f, dy3 = 4.44f)
                    lineTo(x = 34.31f, y = 63.0f)
                    curveToRelative(dx1 = 0.94f, dy1 = 3.16f, dx2 = 1.9f, dy2 = 5.47f, dx3 = 4.81f, dy3 = 5.47f)
                }
            }.build().also { _paperplaneFill = it }
        }

    private var _paperplaneFill: ImageVector? = null

    val XMark: ImageVector
        get() {
            val current = _xMark
            if (current != null) return current

            return ImageVector.Builder(
                name = "XMark",
                defaultWidth = 49.742000579833984.dp,
                defaultHeight = 49.58599853515625.dp,
                viewportWidth = 49.742f,
                viewportHeight = 49.586f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFFFFFFFF)),
                    fillAlpha = 0.85f,
                ) {
                    moveTo(x = 44.78f, y = 0.87f)
                    lineTo(x = 0.8f, y = 44.84f)
                    arcToRelative(a = 2.8f, b = 2.8f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 0.0f, dy1 = 3.94f)
                    arcToRelative(a = 2.87f, b = 2.87f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 3.97f, dy1 = 0.0f)
                    lineTo(x = 48.75f, y = 4.8f)
                    arcToRelative(a = 2.8f, b = 2.8f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -3.97f, dy1 = -3.94f)
                    moveToRelative(dx = 3.97f, dy = 43.97f)
                    lineTo(x = 4.78f, y = 0.87f)
                    arcToRelative(a = 2.8f, b = 2.8f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -3.97f, dy1 = 0.0f)
                    arcToRelative(a = 2.83f, b = 2.83f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 0.0f, dy1 = 3.94f)
                    lineToRelative(dx = 43.97f, dy = 43.97f)
                    arcToRelative(a = 2.84f, b = 2.84f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 3.97f, dy1 = 0.0f)
                    arcToRelative(a = 2.83f, b = 2.83f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 0.0f, dy1 = -3.94f)
                }
            }.build().also { _xMark = it }
        }

    private var _xMark: ImageVector? = null

    val SpeakerFill: ImageVector
        get() {
            val current = _speakerFill
            if (current != null) return current

            return ImageVector.Builder(
                name = "SpeakerFill",
                defaultWidth = 44.65599822998047.dp,
                defaultHeight = 55.15599822998047.dp,
                viewportWidth = 44.656f,
                viewportHeight = 55.156f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFFFFFFFF)),
                    fillAlpha = 0.85f,
                ) {
                    moveTo(x = 33.69f, y = 55.16f)
                    arcToRelative(a = 3.4f, b = 3.4f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 3.53f, dy1 = -3.5f)
                    verticalLineTo(y = 3.72f)
                    arcToRelative(a = 3.56f, b = 3.56f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -3.6f, dy1 = -3.69f)
                    curveToRelative(dx1 = -1.43f, dy1 = 0.0f, dx2 = -2.43f, dy2 = 0.63f, dx3 = -4.03f, dy3 = 2.16f)
                    lineToRelative(dx = -13.3f, dy = 12.5f)
                    arcToRelative(a = 1.1f, b = 1.1f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -0.79f, dy1 = 0.28f)
                    horizontalLineTo(x = 6.53f)
                    curveTo(x1 = 2.31f, y1 = 14.97f, x2 = 0.0f, y2 = 17.3f, x3 = 0.0f, y3 = 21.8f)
                    verticalLineToRelative(dy = 11.63f)
                    curveToRelative(dx1 = 0.0f, dy1 = 4.53f, dx2 = 2.31f, dy2 = 6.84f, dx3 = 6.53f, dy3 = 6.84f)
                    horizontalLineToRelative(dx = 8.97f)
                    quadToRelative(dx1 = 0.46f, dy1 = 0.0f, dx2 = 0.78f, dy2 = 0.28f)
                    lineTo(x = 29.6f, y = 53.2f)
                    curveToRelative(dx1 = 1.44f, dy1 = 1.37f, dx2 = 2.66f, dy2 = 1.97f, dx3 = 4.1f, dy3 = 1.97f)
                }
            }.build().also { _speakerFill = it }
        }

    private var _speakerFill: ImageVector? = null

    val SpeakerWave3Fill: ImageVector
        get() {
            val current = _speakerWave3Fill
            if (current != null) return current

            return ImageVector.Builder(
                name = "SpeakerWave3Fill",
                defaultWidth = 84.06300354003906.dp,
                defaultHeight = 60.39899826049805.dp,
                viewportWidth = 84.063f,
                viewportHeight = 60.399f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFFFFFFFF)),
                    fillAlpha = 0.85f,
                ) {
                    moveTo(x = 71.69f, y = 59.93f)
                    curveToRelative(dx1 = 1.06f, dy1 = 0.79f, dx2 = 2.6f, dy2 = 0.47f, dx3 = 3.44f, dy3 = -0.78f)
                    curveToRelative(dx1 = 5.46f, dy1 = -7.9f, dx2 = 8.75f, dy2 = -17.93f, dx3 = 8.75f, dy3 = -28.97f)
                    arcToRelative(a = 51.5f, b = 51.5f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -8.75f, dy1 = -28.96f)
                    curveToRelative(dx1 = -0.85f, dy1 = -1.29f, dx2 = -2.38f, dy2 = -1.57f, dx3 = -3.44f, dy3 = -0.79f)
                    curveToRelative(dx1 = -1.19f, dy1 = 0.82f, dx2 = -1.35f, dy2 = 2.29f, dx3 = -0.5f, dy3 = 3.57f)
                    arcToRelative(a = 46.6f, b = 46.6f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 7.9f, dy1 = 26.18f)
                    curveToRelative(dx1 = 0.0f, dy1 = 10.0f, dx2 = -3.06f, dy2 = 19.07f, dx3 = -7.9f, dy3 = 26.2f)
                    curveToRelative(dx1 = -0.85f, dy1 = 1.27f, dx2 = -0.69f, dy2 = 2.74f, dx3 = 0.5f, dy3 = 3.55f)
                    moveToRelative(dx = -11.72f, dy = -8.28f)
                    curveToRelative(dx1 = 1.16f, dy1 = 0.78f, dx2 = 2.6f, dy2 = 0.5f, dx3 = 3.44f, dy3 = -0.68f)
                    curveToRelative(dx1 = 4.0f, dy1 = -5.5f, dx2 = 6.34f, dy2 = -13.07f, dx3 = 6.34f, dy3 = -20.79f)
                    reflectiveCurveTo(x1 = 67.44f, y1 = 14.84f, x2 = 63.41f, y2 = 9.4f)
                    curveToRelative(dx1 = -0.85f, dy1 = -1.18f, dx2 = -2.28f, dy2 = -1.47f, dx3 = -3.44f, dy3 = -0.68f)
                    reflectiveCurveToRelative(dx1 = -1.34f, dy1 = 2.25f, dx2 = -0.44f, dy2 = 3.53f)
                    curveToRelative(dx1 = 3.4f, dy1 = 4.8f, dx2 = 5.38f, dy2 = 11.28f, dx3 = 5.38f, dy3 = 17.93f)
                    reflectiveCurveToRelative(dx1 = -2.03f, dy1 = 13.07f, dx2 = -5.38f, dy2 = 17.94f)
                    curveToRelative(dx1 = -0.87f, dy1 = 1.28f, dx2 = -0.72f, dy2 = 2.75f, dx3 = 0.44f, dy3 = 3.53f)
                    moveToRelative(dx = -11.6f, dy = -8.15f)
                    curveToRelative(dx1 = 1.04f, dy1 = 0.72f, dx2 = 2.5f, dy2 = 0.5f, dx3 = 3.35f, dy3 = -0.72f)
                    curveToRelative(dx1 = 2.4f, dy1 = -3.16f, dx2 = 3.84f, dy2 = -7.81f, dx3 = 3.84f, dy3 = -12.6f)
                    reflectiveCurveToRelative(dx1 = -1.44f, dy1 = -9.4f, dx2 = -3.84f, dy2 = -12.59f)
                    curveToRelative(dx1 = -0.84f, dy1 = -1.22f, dx2 = -2.31f, dy2 = -1.47f, dx3 = -3.34f, dy3 = -0.72f)
                    curveToRelative(dx1 = -1.29f, dy1 = 0.88f, dx2 = -1.47f, dy2 = 2.44f, dx3 = -0.5f, dy3 = 3.72f)
                    curveToRelative(dx1 = 1.8f, dy1 = 2.5f, dx2 = 2.84f, dy2 = 5.97f, dx3 = 2.84f, dy3 = 9.6f)
                    reflectiveCurveToRelative(dx1 = -1.06f, dy1 = 7.06f, dx2 = -2.84f, dy2 = 9.59f)
                    curveToRelative(dx1 = -0.94f, dy1 = 1.31f, dx2 = -0.79f, dy2 = 2.81f, dx3 = 0.5f, dy3 = 3.72f)
                    moveTo(x = 33.7f, y = 57.78f)
                    arcToRelative(a = 3.4f, b = 3.4f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 3.53f, dy1 = -3.5f)
                    verticalLineTo(y = 6.34f)
                    arcToRelative(a = 3.56f, b = 3.56f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -3.6f, dy1 = -3.69f)
                    curveToRelative(dx1 = -1.46f, dy1 = 0.0f, dx2 = -2.46f, dy2 = 0.63f, dx3 = -4.03f, dy3 = 2.16f)
                    lineTo(x = 16.25f, y = 17.3f)
                    arcToRelative(a = 1.0f, b = 1.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -0.78f, dy1 = 0.28f)
                    horizontalLineTo(x = 6.53f)
                    curveTo(x1 = 2.28f, y1 = 17.59f, x2 = 0.0f, y2 = 19.93f, x3 = 0.0f, y3 = 24.43f)
                    verticalLineToRelative(dy = 11.63f)
                    curveToRelative(dx1 = 0.0f, dy1 = 4.53f, dx2 = 2.28f, dy2 = 6.84f, dx3 = 6.53f, dy3 = 6.84f)
                    horizontalLineToRelative(dx = 8.94f)
                    curveToRelative(dx1 = 0.31f, dy1 = 0.0f, dx2 = 0.6f, dy2 = 0.1f, dx3 = 0.78f, dy3 = 0.28f)
                    lineToRelative(dx = 13.34f, dy = 12.63f)
                    curveToRelative(dx1 = 1.41f, dy1 = 1.37f, dx2 = 2.63f, dy2 = 1.97f, dx3 = 4.1f, dy3 = 1.97f)
                }
            }.build().also { _speakerWave3Fill = it }
        }

    private var _speakerWave3Fill: ImageVector? = null

    val BoltFill: ImageVector
        get() {
            val current = _boltFill
            if (current != null) return current

            return ImageVector.Builder(
                name = "BoltFill",
                defaultWidth = 44.21900177001953.dp,
                defaultHeight = 70.3290023803711.dp,
                viewportWidth = 44.219f,
                viewportHeight = 70.329f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFFFFFFFF)),
                    fillAlpha = 0.85f,
                ) {
                    moveTo(x = 0.0f, y = 38.77f)
                    curveTo(x1 = 0.0f, y1 = 40.0f, x2 = 0.94f, y2 = 40.9f, x3 = 2.25f, y3 = 40.9f)
                    horizontalLineToRelative(dx = 17.66f)
                    lineTo(x = 10.59f, y = 66.2f)
                    curveToRelative(dx1 = -1.21f, dy1 = 3.22f, dx2 = 2.13f, dy2 = 4.94f, dx3 = 4.22f, dy3 = 2.31f)
                    lineToRelative(dx = 28.4f, dy = -35.5f)
                    arcToRelative(a = 3.0f, b = 3.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 0.82f, dy1 = -2.0f)
                    curveToRelative(dx1 = 0.0f, dy1 = -1.18f, dx2 = -0.94f, dy2 = -2.12f, dx3 = -2.25f, dy3 = -2.12f)
                    horizontalLineTo(x = 24.13f)
                    lineToRelative(dx = 9.3f, dy = -25.31f)
                    curveToRelative(dx1 = 1.23f, dy1 = -3.22f, dx2 = -2.12f, dy2 = -4.94f, dx3 = -4.21f, dy3 = -2.28f)
                    lineTo(x = 0.82f, y = 36.77f)
                    arcToRelative(a = 3.2f, b = 3.2f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -0.82f, dy1 = 2.0f)
                }
            }.build().also { _boltFill = it }
        }

    private var _boltFill: ImageVector? = null

    val XMarkCircleFill: ImageVector
        get() {
            val current = _xMarkCircleFill
            if (current != null) return current

            return ImageVector.Builder(
                name = "XMarkCircleFill",
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
                    moveTo(x = 41.13f, y = 19.13f)
                    lineToRelative(dx = -9.22f, dy = 9.15f)
                    lineToRelative(dx = -9.2f, dy = -9.16f)
                    arcToRelative(a = 2.5f, b = 2.5f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -1.8f, dy1 = -0.75f)
                    arcToRelative(a = 2.55f, b = 2.55f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -1.85f, dy1 = 4.38f)
                    lineToRelative(dx = 9.18f, dy = 9.17f)
                    lineToRelative(dx = -9.18f, dy = 9.11f)
                    arcToRelative(a = 2.7f, b = 2.7f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -0.75f, dy1 = 1.84f)
                    arcToRelative(a = 2.6f, b = 2.6f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 2.6f, dy1 = 2.63f)
                    arcToRelative(a = 2.7f, b = 2.7f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 1.87f, dy1 = -0.78f)
                    lineToRelative(dx = 9.12f, dy = -9.13f)
                    lineToRelative(dx = 9.13f, dy = 9.13f)
                    arcToRelative(a = 2.7f, b = 2.7f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 1.88f, dy1 = 0.78f)
                    arcToRelative(a = 2.6f, b = 2.6f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 2.56f, dy1 = -2.62f)
                    arcToRelative(a = 2.6f, b = 2.6f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -0.75f, dy1 = -1.85f)
                    lineToRelative(dx = -9.15f, dy = -9.11f)
                    lineToRelative(dx = 9.15f, dy = -9.17f)
                    arcToRelative(a = 2.5f, b = 2.5f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 0.75f, dy1 = -1.84f)
                    curveToRelative(dx1 = 0.0f, dy1 = -1.41f, dx2 = -1.16f, dy2 = -2.53f, dx3 = -2.56f, dy3 = -2.53f)
                    arcToRelative(a = 2.3f, b = 2.3f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -1.78f, dy1 = 0.75f)
                }
            }.build().also { _xMarkCircleFill = it }
        }

    private var _xMarkCircleFill: ImageVector? = null

    val Checkmark: ImageVector
        get() {
            val current = _checkmark
            if (current != null) return current

            return ImageVector.Builder(
                name = "Checkmark",
                defaultWidth = 54.03099822998047.dp,
                defaultHeight = 55.15599822998047.dp,
                viewportWidth = 54.031f,
                viewportHeight = 55.156f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFFFFFFFF)),
                    fillAlpha = 0.85f,
                ) {
                    moveTo(x = 20.38f, y = 55.16f)
                    quadToRelative(dx1 = 2.02f, dy1 = -0.01f, dx2 = 3.15f, dy2 = -1.75f)
                    lineTo(x = 53.06f, y = 6.9f)
                    arcToRelative(a = 4.2f, b = 4.2f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 0.78f, dy1 = -2.32f)
                    curveToRelative(dx1 = 0.0f, dy1 = -1.71f, dx2 = -1.12f, dy2 = -2.84f, dx3 = -2.84f, dy3 = -2.84f)
                    curveToRelative(dx1 = -1.25f, dy1 = 0.0f, dx2 = -1.94f, dy2 = 0.4f, dx3 = -2.69f, dy3 = 1.6f)
                    lineTo(x = 20.25f, y = 48.05f)
                    lineTo(x = 5.69f, y = 29.0f)
                    curveTo(x1 = 4.9f, y1 = 27.9f, x2 = 4.12f, y2 = 27.47f, x3 = 3.0f, y3 = 27.47f)
                    curveToRelative(dx1 = -1.78f, dy1 = 0.0f, dx2 = -3.0f, dy2 = 1.22f, dx3 = -3.0f, dy3 = 2.94f)
                    arcToRelative(a = 3.8f, b = 3.8f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 0.9f, dy1 = 2.28f)
                    lineToRelative(dx = 16.23f, dy = 20.65f)
                    curveToRelative(dx1 = 0.93f, dy1 = 1.22f, dx2 = 1.9f, dy2 = 1.82f, dx3 = 3.25f, dy3 = 1.82f)
                }
            }.build().also { _checkmark = it }
        }

    private var _checkmark: ImageVector? = null

    val SquareAndArrowDown: ImageVector
        get() {
            val current = _squareAndArrowDown
            if (current != null) return current

            return ImageVector.Builder(
                name = "SquareAndArrowDown",
                defaultWidth = 55.65599822998047.dp,
                defaultHeight = 81.09400177001953.dp,
                viewportWidth = 55.656f,
                viewportHeight = 81.094f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFFFFFFFF)),
                    fillAlpha = 0.85f,
                ) {
                    moveTo(x = 55.47f, y = 35.56f)
                    verticalLineToRelative(dy = 24.07f)
                    curveToRelative(dx1 = 0.0f, dy1 = 8.4f, dx2 = -4.69f, dy2 = 13.06f, dx3 = -13.1f, dy3 = 13.06f)
                    horizontalLineToRelative(dx = -29.3f)
                    curveTo(x1 = 4.65f, y1 = 72.69f, x2 = 0.0f, y2 = 68.03f, x3 = 0.0f, y3 = 59.63f)
                    verticalLineTo(y = 35.56f)
                    curveTo(x1 = 0.0f, y1 = 27.2f, x2 = 4.66f, y2 = 22.5f, x3 = 13.06f, y3 = 22.5f)
                    horizontalLineToRelative(dx = 6.32f)
                    verticalLineToRelative(dy = 5.03f)
                    horizontalLineToRelative(dx = -6.32f)
                    curveToRelative(dx1 = -5.12f, dy1 = 0.0f, dx2 = -8.03f, dy2 = 2.9f, dx3 = -8.03f, dy3 = 8.03f)
                    verticalLineToRelative(dy = 24.07f)
                    curveToRelative(dx1 = 0.0f, dy1 = 5.15f, dx2 = 2.9f, dy2 = 8.03f, dx3 = 8.03f, dy3 = 8.03f)
                    horizontalLineToRelative(dx = 29.32f)
                    curveToRelative(dx1 = 5.15f, dy1 = 0.0f, dx2 = 8.06f, dy2 = -2.88f, dx3 = 8.06f, dy3 = -8.03f)
                    verticalLineTo(y = 35.56f)
                    curveToRelative(dx1 = 0.0f, dy1 = -5.12f, dx2 = -2.9f, dy2 = -8.03f, dx3 = -8.06f, dy3 = -8.03f)
                    horizontalLineToRelative(dx = -6.29f)
                    verticalLineTo(y = 22.5f)
                    horizontalLineToRelative(dx = 6.28f)
                    curveToRelative(dx1 = 8.41f, dy1 = 0.0f, dx2 = 13.1f, dy2 = 4.69f, dx3 = 13.1f, dy3 = 13.06f)
                }
                path(
                    fill = SolidColor(Color(0xFFFFFFFF)),
                    fillAlpha = 0.85f,
                ) {
                    moveTo(x = 27.75f, y = 5.88f)
                    curveToRelative(dx1 = -1.34f, dy1 = 0.0f, dx2 = -2.5f, dy2 = 1.09f, dx3 = -2.5f, dy3 = 2.4f)
                    verticalLineTo(y = 40.1f)
                    lineToRelative(dx = 0.38f, dy = 8.41f)
                    arcToRelative(a = 2.16f, b = 2.16f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 2.12f, dy1 = 2.1f)
                    arcToRelative(a = 2.16f, b = 2.16f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 2.1f, dy1 = -2.1f)
                    lineToRelative(dx = 0.37f, dy = -8.4f)
                    verticalLineTo(y = 8.27f)
                    curveToRelative(dx1 = 0.0f, dy1 = -1.31f, dx2 = -1.13f, dy2 = -2.4f, dx3 = -2.47f, dy3 = -2.4f)
                    moveTo(x = 17.13f, y = 37.0f)
                    arcToRelative(a = 2.15f, b = 2.15f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -2.25f, dy1 = 2.19f)
                    arcToRelative(a = 2.2f, b = 2.2f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 0.74f, dy1 = 1.65f)
                    lineToRelative(dx = 10.32f, dy = 9.94f)
                    curveToRelative(dx1 = 0.62f, dy1 = 0.63f, dx2 = 1.15f, dy2 = 0.84f, dx3 = 1.81f, dy3 = 0.84f)
                    curveToRelative(dx1 = 0.63f, dy1 = 0.0f, dx2 = 1.16f, dy2 = -0.21f, dx3 = 1.78f, dy3 = -0.84f)
                    lineToRelative(dx = 10.31f, dy = -9.94f)
                    arcToRelative(a = 2.2f, b = 2.2f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 0.75f, dy1 = -1.65f)
                    curveToRelative(dx1 = 0.0f, dy1 = -1.25f, dx2 = -1.0f, dy2 = -2.19f, dx3 = -2.28f, dy3 = -2.19f)
                    arcToRelative(a = 2.2f, b = 2.2f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -1.69f, dy1 = 0.75f)
                    lineToRelative(dx = -4.84f, dy = 5.16f)
                    lineToRelative(dx = -4.03f, dy = 4.3f)
                    lineToRelative(dx = -4.06f, dy = -4.3f)
                    lineToRelative(dx = -4.85f, dy = -5.16f)
                    arcTo(horizontalEllipseRadius = 2.3f, verticalEllipseRadius = 2.3f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 17.13f, y1 = 37.0f)
                }
            }.build().also { _squareAndArrowDown = it }
        }

    private var _squareAndArrowDown: ImageVector? = null

    val SquareAndArrowUp: ImageVector
        get() {
            val current = _squareAndArrowUp
            if (current != null) return current

            return ImageVector.Builder(
                name = "SquareAndArrowUp",
                defaultWidth = 55.65599822998047.dp,
                defaultHeight = 85.84400177001953.dp,
                viewportWidth = 55.656f,
                viewportHeight = 85.844f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFFFFFFFF)),
                    fillAlpha = 0.85f,
                ) {
                    moveTo(x = 55.47f, y = 37.94f)
                    verticalLineTo(y = 62.0f)
                    curveToRelative(dx1 = 0.0f, dy1 = 8.4f, dx2 = -4.69f, dy2 = 13.06f, dx3 = -13.1f, dy3 = 13.06f)
                    horizontalLineToRelative(dx = -29.3f)
                    curveTo(x1 = 4.65f, y1 = 75.06f, x2 = 0.0f, y2 = 70.41f, x3 = 0.0f, y3 = 62.0f)
                    verticalLineTo(y = 37.94f)
                    curveToRelative(dx1 = 0.0f, dy1 = -8.38f, dx2 = 4.66f, dy2 = -13.06f, dx3 = 13.06f, dy3 = -13.06f)
                    horizontalLineToRelative(dx = 6.32f)
                    verticalLineToRelative(dy = 5.03f)
                    horizontalLineToRelative(dx = -6.32f)
                    curveToRelative(dx1 = -5.12f, dy1 = 0.0f, dx2 = -8.03f, dy2 = 2.9f, dx3 = -8.03f, dy3 = 8.03f)
                    verticalLineTo(y = 62.0f)
                    curveToRelative(dx1 = 0.0f, dy1 = 5.16f, dx2 = 2.9f, dy2 = 8.03f, dx3 = 8.03f, dy3 = 8.03f)
                    horizontalLineToRelative(dx = 29.32f)
                    curveToRelative(dx1 = 5.15f, dy1 = 0.0f, dx2 = 8.06f, dy2 = -2.87f, dx3 = 8.06f, dy3 = -8.03f)
                    verticalLineTo(y = 37.94f)
                    curveToRelative(dx1 = 0.0f, dy1 = -5.13f, dx2 = -2.9f, dy2 = -8.03f, dx3 = -8.06f, dy3 = -8.03f)
                    horizontalLineToRelative(dx = -6.32f)
                    verticalLineToRelative(dy = -5.03f)
                    horizontalLineToRelative(dx = 6.31f)
                    curveToRelative(dx1 = 8.41f, dy1 = 0.0f, dx2 = 13.1f, dy2 = 4.68f, dx3 = 13.1f, dy3 = 13.06f)
                }
                path(
                    fill = SolidColor(Color(0xFFFFFFFF)),
                    fillAlpha = 0.85f,
                ) {
                    moveTo(x = 17.13f, y = 19.47f)
                    curveToRelative(dx1 = 0.59f, dy1 = 0.0f, dx2 = 1.28f, dy2 = -0.25f, dx3 = 1.71f, dy3 = -0.75f)
                    lineToRelative(dx = 4.85f, dy = -5.16f)
                    lineToRelative(dx = 4.03f, dy = -4.28f)
                    lineToRelative(dx = 4.06f, dy = 4.28f)
                    lineToRelative(dx = 4.81f, dy = 5.16f)
                    arcToRelative(a = 2.3f, b = 2.3f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 1.7f, dy1 = 0.75f)
                    curveToRelative(dx1 = 1.3f, dy1 = 0.0f, dx2 = 2.27f, dy2 = -0.9f, dx3 = 2.27f, dy3 = -2.19f)
                    arcToRelative(a = 2.2f, b = 2.2f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -0.72f, dy1 = -1.66f)
                    lineTo(x = 29.54f, y = 5.7f)
                    curveToRelative(dx1 = -0.63f, dy1 = -0.63f, dx2 = -1.16f, dy2 = -0.82f, dx3 = -1.82f, dy3 = -0.82f)
                    curveToRelative(dx1 = -0.63f, dy1 = 0.0f, dx2 = -1.16f, dy2 = 0.2f, dx3 = -1.81f, dy3 = 0.82f)
                    lineToRelative(dx = -10.28f, dy = 9.93f)
                    arcToRelative(a = 2.2f, b = 2.2f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -0.76f, dy1 = 1.66f)
                    curveToRelative(dx1 = 0.0f, dy1 = 1.28f, dx2 = 0.94f, dy2 = 2.19f, dx3 = 2.26f, dy3 = 2.19f)
                    moveToRelative(dx = 10.59f, dy = 31.12f)
                    curveToRelative(dx1 = 1.34f, dy1 = 0.0f, dx2 = 2.5f, dy2 = -1.09f, dx3 = 2.5f, dy3 = -2.4f)
                    verticalLineTo(y = 16.4f)
                    lineToRelative(dx = -0.38f, dy = -8.44f)
                    arcToRelative(a = 2.2f, b = 2.2f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -2.12f, dy1 = -2.1f)
                    curveToRelative(dx1 = -1.13f, dy1 = 0.0f, dx2 = -2.03f, dy2 = 0.97f, dx3 = -2.1f, dy3 = 2.1f)
                    lineToRelative(dx = -0.37f, dy = 8.44f)
                    verticalLineToRelative(dy = 31.78f)
                    curveToRelative(dx1 = 0.0f, dy1 = 1.31f, dx2 = 1.13f, dy2 = 2.4f, dx3 = 2.47f, dy3 = 2.4f)
                }
            }.build().also { _squareAndArrowUp = it }
        }

    private var _squareAndArrowUp: ImageVector? = null

    override val BoltCircle: ImageVector
        get() {
            val current = _boltCircle
            if (current != null) return current

            return ImageVector.Builder(
                name = "BoltCircle",
                defaultWidth = 63.9379997253418.dp,
                defaultHeight = 63.78099822998047.dp,
                viewportWidth = 63.938f,
                viewportHeight = 63.781f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFFFFFFFF)),
                    fillAlpha = 0.85f,
                ) {
                    moveTo(x = 31.88f, y = 63.75f)
                    arcToRelative(a = 31.9f, b = 31.9f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 31.87f, dy1 = -31.87f)
                    arcTo(horizontalEllipseRadius = 31.9f, verticalEllipseRadius = 31.9f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 31.88f, y1 = 0.0f)
                    arcTo(horizontalEllipseRadius = 31.9f, verticalEllipseRadius = 31.9f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 0.0f, y1 = 31.88f)
                    arcToRelative(a = 31.9f, b = 31.9f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 31.88f, dy1 = 31.87f)
                    moveToRelative(dx = 0.0f, dy = -5.31f)
                    arcTo(horizontalEllipseRadius = 26.54f, verticalEllipseRadius = 26.54f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 5.3f, y1 = 31.88f)
                    arcTo(horizontalEllipseRadius = 26.54f, verticalEllipseRadius = 26.54f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 31.88f, y1 = 5.3f)
                    arcToRelative(a = 26.54f, b = 26.54f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 26.56f, dy1 = 26.57f)
                    arcToRelative(a = 26.54f, b = 26.54f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -26.56f, dy1 = 26.56f)
                }
                path(
                    fill = SolidColor(Color(0xFFFFFFFF)),
                    fillAlpha = 0.85f,
                ) {
                    moveTo(x = 18.16f, y = 34.03f)
                    curveToRelative(dx1 = 0.0f, dy1 = 0.78f, dx2 = 0.62f, dy2 = 1.34f, dx3 = 1.43f, dy3 = 1.34f)
                    horizontalLineToRelative(dx = 10.6f)
                    lineToRelative(dx = -5.66f, dy = 15.2f)
                    curveToRelative(dx1 = -0.75f, dy1 = 2.0f, dx2 = 1.38f, dy2 = 3.09f, dx3 = 2.69f, dy3 = 1.46f)
                    lineToRelative(dx = 17.12f, dy = -21.5f)
                    arcToRelative(a = 2.0f, b = 2.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 0.53f, dy1 = -1.22f)
                    curveToRelative(dx1 = 0.0f, dy1 = -0.78f, dx2 = -0.62f, dy2 = -1.34f, dx3 = -1.43f, dy3 = -1.34f)
                    horizontalLineToRelative(dx = -10.6f)
                    lineToRelative(dx = 5.66f, dy = -15.19f)
                    curveToRelative(dx1 = 0.75f, dy1 = -2.0f, dx2 = -1.37f, dy2 = -3.1f, dx3 = -2.69f, dy3 = -1.5f)
                    lineTo(x = 18.7f, y = 32.78f)
                    arcToRelative(a = 2.0f, b = 2.0f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -0.53f, dy1 = 1.25f)
                }
            }.build().also { _boltCircle = it }
        }

    private var _boltCircle: ImageVector? = null

    override val Circle: ImageVector
        get() {
            val current = _circle
            if (current != null) return current

            return ImageVector.Builder(
                name = "Circle",
                defaultWidth = 63.9379997253418.dp,
                defaultHeight = 63.78099822998047.dp,
                viewportWidth = 63.938f,
                viewportHeight = 63.781f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFFFFFFFF)),
                    fillAlpha = 0.85f,
                ) {
                    moveTo(x = 31.88f, y = 63.75f)
                    arcToRelative(a = 31.9f, b = 31.9f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 31.87f, dy1 = -31.87f)
                    arcTo(horizontalEllipseRadius = 31.9f, verticalEllipseRadius = 31.9f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 31.88f, y1 = 0.0f)
                    arcTo(horizontalEllipseRadius = 31.9f, verticalEllipseRadius = 31.9f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 0.0f, y1 = 31.88f)
                    arcToRelative(a = 31.9f, b = 31.9f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 31.88f, dy1 = 31.87f)
                    moveToRelative(dx = 0.0f, dy = -5.31f)
                    arcTo(horizontalEllipseRadius = 26.54f, verticalEllipseRadius = 26.54f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 5.3f, y1 = 31.88f)
                    arcTo(horizontalEllipseRadius = 26.54f, verticalEllipseRadius = 26.54f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 31.88f, y1 = 5.3f)
                    arcToRelative(a = 26.54f, b = 26.54f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 26.56f, dy1 = 26.57f)
                    arcToRelative(a = 26.54f, b = 26.54f, theta = 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -26.56f, dy1 = 26.56f)
                }
            }.build().also { _circle = it }
        }

    private var _circle: ImageVector? = null

    override val CircleDotted: ImageVector
        get() = CommonIcons.CircleDotted
}
