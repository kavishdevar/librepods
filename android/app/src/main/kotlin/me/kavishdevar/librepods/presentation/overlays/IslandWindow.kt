/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

@file:OptIn(ExperimentalEncodingApi::class)

package me.kavishdevar.librepods.presentation.overlays

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log.e
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.VideoView
import androidx.core.net.toUri
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.devices.Battery
import me.kavishdevar.librepods.devices.BatteryComponent
import me.kavishdevar.librepods.devices.BatteryStatus
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.abs

enum class IslandType {
    CONNECTED,
    TAKING_OVER,
    MOVED_TO_REMOTE,
    MOVED_TO_OTHER_DEVICE,
}

class IslandWindow(private val context: Context) {
    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    @SuppressLint("InflateParams")
    private val islandView: View = LayoutInflater.from(context).inflate(R.layout.island_window, null)
    private var isClosing = false
    private var params: WindowManager.LayoutParams? = null

    private var initialY = 0f
    private var initialTouchY = 0f
    private var lastTouchY = 0f
    private var velocityTracker: VelocityTracker? = null
    private var isBeingDragged = false
    private var autoCloseHandler: Handler? = null
    private var autoCloseRunnable: Runnable? = null
    private var initialHeight = 0
    private var screenHeight = 0
    private var isDraggingDown = false
    private var lastMoveTime = 0L
    private var yMovement = 0f
    private var dragDistance = 0f

    private var initialConnectedTextY = 0f
    private var initialDeviceTextY = 0f
    private var initialBatteryViewY = 0f
    private var initialVideoViewY = 0f
    private var initialTextSeparation = 0f

    private val containerView = FrameLayout(context)

    private lateinit var springAnimation: SpringAnimation
    private val flingAnimator = ValueAnimator()

    val isVisible: Boolean
        get() = containerView.parent != null && containerView.visibility == View.VISIBLE

    @SuppressLint("SetTextI18n")
    fun updateBattery(batteryList: Set<Battery>) {
        if (batteryList.isEmpty()) return

        val leftBattery = batteryList.find { it.component == BatteryComponent.LEFT }
        val rightBattery = batteryList.find { it.component == BatteryComponent.RIGHT }

        val leftLevel = leftBattery?.level ?: 0
        val rightLevel = rightBattery?.level ?: 0
        leftBattery?.status ?: BatteryStatus.DISCONNECTED
        rightBattery?.status ?: BatteryStatus.DISCONNECTED

        val batteryText = islandView.findViewById<TextView>(R.id.island_battery_text)
        val batteryProgressBar = islandView.findViewById<ProgressBar>(R.id.island_battery_progress)

        val displayBatteryLevel = when {
            leftLevel > 0 && rightLevel > 0 -> minOf(leftLevel, rightLevel)
            leftLevel > 0 -> leftLevel
            rightLevel > 0 -> rightLevel
            else -> null
        }

        if (displayBatteryLevel != null) {
            batteryText.text = "$displayBatteryLevel%"
            batteryProgressBar.progress = displayBatteryLevel
            batteryProgressBar.isIndeterminate = false
        } else {
            batteryText.text = "?"
            batteryProgressBar.progress = 0
            batteryProgressBar.isIndeterminate = false
        }
    }

    @SuppressLint("SetTextI18s", "ClickableViewAccessibility", "UnspecifiedRegisterReceiverFlag",
        "SetTextI18n"
    )
    fun show(
        name: String,
        batteryPercentage: Int,
        context: Context,
        type: IslandType = IslandType.CONNECTED,
        reversed: Boolean = false,
        otherDeviceName: String? = null,
        onReverseAction: () -> Unit = {}
    ) {

        val displayMetrics = Resources.getSystem().displayMetrics
        val width = (displayMetrics.widthPixels * 0.95).toInt()
        screenHeight = displayMetrics.heightPixels

        val batteryText = islandView.findViewById<TextView>(R.id.island_battery_text)
        val batteryProgressBar = islandView.findViewById<ProgressBar>(R.id.island_battery_progress)

        if (batteryPercentage != 0) {
            batteryText.text = "$batteryPercentage%"
            batteryProgressBar.progress = batteryPercentage
        } else {
            batteryText.text = "?"
            batteryProgressBar.progress = 0
        }

        batteryProgressBar.isIndeterminate = false
        islandView.findViewById<TextView>(R.id.island_device_name).text = name

        val actionButton = islandView.findViewById<ImageButton>(R.id.island_action_button)
        val batteryBg = islandView.findViewById<ProgressBar>(R.id.island_battery_bg)
        if (type == IslandType.MOVED_TO_OTHER_DEVICE && !reversed) {
            actionButton.visibility = View.VISIBLE
            actionButton.setOnClickListener {
                onReverseAction()
                close()
            }
            batteryText.visibility = View.GONE
            batteryProgressBar.visibility = View.GONE
            batteryBg.visibility = View.GONE
        } else {
            actionButton.visibility = View.GONE
            batteryText.visibility = View.VISIBLE
            batteryProgressBar.visibility = View.VISIBLE
            batteryBg.visibility = View.VISIBLE
        }

        containerView.removeAllViews()
        val containerParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )

        containerView.addView(islandView, containerParams)

        params = WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }

        islandView.visibility = View.VISIBLE
        containerView.visibility = View.VISIBLE

        containerView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    autoCloseHandler?.removeCallbacks(autoCloseRunnable ?: return@setOnTouchListener false)
                    flingAnimator.cancel()

                    velocityTracker?.recycle()
                    velocityTracker = VelocityTracker.obtain()
                    velocityTracker?.addMovement(event)

                    initialY = containerView.translationY
                    initialTouchY = event.rawY
                    lastTouchY = event.rawY
                    initialHeight = islandView.height
                    isBeingDragged = false
                    isDraggingDown = false
                    lastMoveTime = System.currentTimeMillis()
                    dragDistance = 0f

                    captureInitialPositions()

                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    velocityTracker?.addMovement(event)
                    val deltaY = event.rawY - initialTouchY
                    val moveDelta = event.rawY - lastTouchY
                    dragDistance += abs(moveDelta)

                    isDraggingDown = moveDelta > 0

                    val currentTime = System.currentTimeMillis()
                    val timeDelta = currentTime - lastMoveTime
                    if (timeDelta > 0) {
                        yMovement = moveDelta / timeDelta * 10
                    }
                    lastMoveTime = currentTime

                    if (abs(deltaY) > 5 || isBeingDragged) {
                        isBeingDragged = true

                        // Cancel auto close timer when dragging starts
                        autoCloseHandler?.removeCallbacks(autoCloseRunnable ?: return@setOnTouchListener false)

                        val dampedDeltaY = if (deltaY > 0) {
                            initialY + (deltaY * 0.6f)
                        } else {
                            initialY + (deltaY * 0.9f)
                        }
                        containerView.translationY = dampedDeltaY

                        if (isDraggingDown && deltaY > 0) {
                            val stretchAmount = (deltaY * 0.5f).coerceAtMost(200f)
                            applyCustomStretchEffect(stretchAmount)
                        }
                    }

                    lastTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    velocityTracker?.addMovement(event)
                    velocityTracker?.computeCurrentVelocity(1000)
                    val yVelocity = velocityTracker?.yVelocity ?: 0f

                    if (isBeingDragged) {
                        val currentTranslationY = containerView.translationY

                        if (isDraggingDown && (currentTranslationY > 200 || yVelocity > 1000)) {
                            flingAnimator.cancel()
                            flingAnimator.setFloatValues(currentTranslationY, screenHeight.toFloat())
                            flingAnimator.duration = 300
                            flingAnimator.interpolator = AccelerateInterpolator()
                            flingAnimator.addUpdateListener { animation ->
                                val value = animation.animatedValue as Float
                                containerView.translationY = value
                            }
                            flingAnimator.addListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    close()
                                }
                            })
                            flingAnimator.start()
                        } else {
                            springAnimation.cancel()
                            springAnimation.setStartValue(currentTranslationY)
                            springAnimation.start()

                            applyCustomStretchEffect(0f)
                        }

                    } else if (dragDistance < 10) {
                        resetAutoCloseTimer()
                    }

                    velocityTracker?.recycle()
                    velocityTracker = null
                    isBeingDragged = false
                    true
                }
                else -> false
            }
        }

        when (type) {
            IslandType.CONNECTED -> {
                islandView.findViewById<TextView>(R.id.island_connected_text).text = context.getString(R.string.island_connected_text)
            }
            IslandType.TAKING_OVER -> {
                islandView.findViewById<TextView>(R.id.island_connected_text).text = context.getString(R.string.island_taking_over_text)
            }
            IslandType.MOVED_TO_REMOTE -> {
                islandView.findViewById<TextView>(R.id.island_connected_text).text = context.getString(R.string.island_moved_to_remote_text)
            }
            IslandType.MOVED_TO_OTHER_DEVICE -> {
                if (otherDeviceName == null || otherDeviceName.isEmpty()) {
                    e("IslandWindow", "Other device name is null or empty for MOVED_TO_OTHER_DEVICE type")
                }
                if (reversed) {
                    islandView.findViewById<TextView>(R.id.island_connected_text).text = context.getString(R.string.island_moved_to_other_device_reversed_text)
                } else {
                    islandView.findViewById<TextView>(R.id.island_connected_text).text = context.getString(R.string.island_moved_to_other_device_text, otherDeviceName)
                }
            }
        }

        val videoView = islandView.findViewById<VideoView>(R.id.island_video_view)
        val videoUri = "android.resource://me.kavishdevar.librepods/${R.raw.island}".toUri()
        videoView.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
        videoView.setVideoURI(videoUri)
        videoView.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.isLooping = true
            videoView.start()
        }

        try {
            windowManager.addView(containerView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        islandView.post {
            initialHeight = islandView.height
            captureInitialPositions()
        }

        springAnimation = SpringAnimation(containerView, DynamicAnimation.TRANSLATION_Y, 0f).apply {
            spring = SpringForce(0f)
                .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY)
                .setStiffness(SpringForce.STIFFNESS_MEDIUM)
        }

        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.5f, 1f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.5f, 1f)
        val translationY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, -200f, 0f)

        Looper.getMainLooper().let { mainLooper ->
            if (Looper.myLooper() == mainLooper) {
                ObjectAnimator.ofPropertyValuesHolder(containerView, scaleX, scaleY, translationY).apply {
                    duration = 700
                    interpolator = AnticipateOvershootInterpolator()
                    start()
                }
            } else {
                Handler(mainLooper).post {
                    ObjectAnimator.ofPropertyValuesHolder(containerView, scaleX, scaleY, translationY).apply {
                        duration = 700
                        interpolator = AnticipateOvershootInterpolator()
                        start()
                    }
                }
            }
        }

        resetAutoCloseTimer()
    }

    private fun captureInitialPositions() {
        val connectedText = islandView.findViewById<TextView>(R.id.island_connected_text)
        val deviceText = islandView.findViewById<TextView>(R.id.island_device_name)
        val batteryView = islandView.findViewById<FrameLayout>(R.id.island_battery_container)
        val videoView = islandView.findViewById<VideoView>(R.id.island_video_view)

        connectedText.post {
            initialConnectedTextY = connectedText.y
            initialDeviceTextY = deviceText.y
            initialTextSeparation = deviceText.y - (connectedText.y + connectedText.height)

            if (batteryView != null) initialBatteryViewY = batteryView.y
            initialVideoViewY = videoView.y
        }
    }

    private fun applyCustomStretchEffect(stretchAmount: Float) {
        try {
            val mainLayout = islandView.findViewById<LinearLayout>(R.id.island_window_layout)
            islandView.findViewById<TextView>(R.id.island_connected_text)
            val deviceText = islandView.findViewById<TextView>(R.id.island_device_name)
            islandView.findViewById<FrameLayout>(R.id.island_battery_container)
            islandView.findViewById<VideoView>(R.id.island_video_view)

            val stretchFactor = 1f + (stretchAmount / 300f).coerceAtMost(4.0f)
            val newMinHeight = (initialHeight * stretchFactor).toInt()
            mainLayout.minimumHeight = newMinHeight

            val textMarginIncrease = (stretchAmount * 0.8f).toInt()

            val deviceTextParams = deviceText.layoutParams as LinearLayout.LayoutParams
            deviceTextParams.topMargin = textMarginIncrease
            deviceText.layoutParams = deviceTextParams

            val background = mainLayout.background
            if (background is GradientDrawable) {
                val cornerRadius = 56f
                background.cornerRadius = cornerRadius
            }

            if (params != null) {
                params!!.height = screenHeight

                val containerParams = containerView.layoutParams
                containerParams.height = screenHeight
                containerView.layoutParams = containerParams

                try {
                    windowManager.updateViewLayout(containerView, params)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun resetAutoCloseTimer() {
        autoCloseHandler?.removeCallbacks(autoCloseRunnable ?: return)
        autoCloseHandler = Handler(Looper.getMainLooper())
        autoCloseRunnable = Runnable { close() }
        autoCloseHandler?.postDelayed(autoCloseRunnable!!, 4500)
    }

    fun close() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post { close() }
            return
        }
        try {
            if (isClosing) return
            isClosing = true

            autoCloseHandler?.removeCallbacks(autoCloseRunnable ?: return)

            val videoView = islandView.findViewById<VideoView>(R.id.island_video_view)
            try {
                videoView.stopPlayback()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, containerView.scaleX, 0.5f)
            val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, containerView.scaleY, 0.5f)
            val translationY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, containerView.translationY, -200f)
            ObjectAnimator.ofPropertyValuesHolder(containerView, scaleX, scaleY, translationY).apply {
                duration = 700
                interpolator = AnticipateOvershootInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        cleanupAndRemoveView()
                    }
                })
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Even if animation fails, ensure we cleanup
            cleanupAndRemoveView()
        }
    }

    private fun cleanupAndRemoveView() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post { cleanupAndRemoveView() }
            return
        }
        try {
            containerView.visibility = View.GONE
        } catch (e: Exception) {
            e("IslandWindow", "Error setting visibility: $e")
        }
        try {
            if (containerView.parent != null) {
                windowManager.removeView(containerView)
            }
        } catch (e: Exception) {
            e("IslandWindow", "Error removing view: $e")
        }
        isClosing = false

        try {
            springAnimation.cancel()
        } catch (e: Exception) {
            e("IslandWindow", "Error cancelling spring animation $e")
        }
        try {
            flingAnimator.cancel()
        } catch (e: Exception) {
            e("IslandWindow", "Error cancelling fling animation $e")
        }
    }

    fun forceClose() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post { forceClose() }
            return
        }
        try {
            if (isClosing) return
            isClosing = true

            autoCloseHandler?.removeCallbacks(autoCloseRunnable ?: return)

            springAnimation.cancel()
            flingAnimator.cancel()

            cleanupAndRemoveView()
        } catch (e: Exception) {
            e.printStackTrace()
            isClosing = false
        }
    }
}
