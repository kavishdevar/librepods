/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package me.kavishdevar.librepods.presentation.overlays

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.AirPodsNotifications
import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.services.ServiceManager
import me.kavishdevar.librepods.utils.BatteryDisplay
import me.kavishdevar.librepods.utils.BatteryDisplaySource
import me.kavishdevar.librepods.utils.BatteryLevels
import me.kavishdevar.librepods.utils.OverlayMedia
import kotlin.math.abs
import kotlin.math.min

enum class IslandType {
    CONNECTED,
    TAKING_OVER,
    MOVED_TO_REMOTE,
    MOVED_TO_OTHER_DEVICE,
}

/** Lightweight fallback for phones that cannot publish a native Live Alert. */
class IslandWindow(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    @SuppressLint("InflateParams")
    private val islandView = LayoutInflater.from(context).inflate(R.layout.island_window, null)
    private val containerView = FrameLayout(context)

    private val artworkView: ImageView = islandView.findViewById(R.id.island_fallback_image)
    private val connectedText: TextView = islandView.findViewById(R.id.island_connected_text)
    private val deviceText: TextView = islandView.findViewById(R.id.island_device_name)
    private val batteryText: TextView = islandView.findViewById(R.id.island_battery_text)
    private val batteryProgress: ProgressBar = islandView.findViewById(R.id.island_battery_progress)
    private val batteryBackground: ProgressBar = islandView.findViewById(R.id.island_battery_bg)
    private val actionButton: ImageButton = islandView.findViewById(R.id.island_action_button)

    private var returnSpring: SpringAnimation? = null
    private var velocityTracker: VelocityTracker? = null
    private var receiverRegistered = false
    private var isClosing = false
    private var currentType = IslandType.CONNECTED
    private var displayedBatterySource: BatteryDisplaySource? = null
    private var downRawY = 0f
    private var startTranslationY = 0f
    private var isDragging = false

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val density = context.resources.displayMetrics.density
    private val autoCloseRunnable = Runnable { close() }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(receiverContext: Context?, intent: Intent?) {
            when (intent?.action) {
                AirPodsNotifications.BATTERY_DATA -> {
                    val batteries = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableArrayListExtra("data", Battery::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableArrayListExtra("data")
                    }
                    updateBatteryDisplay(batteries)
                }

                AirPodsNotifications.DISCONNECT_RECEIVERS -> close()
            }
        }
    }

    val isVisible: Boolean
        get() = containerView.parent != null && containerView.visibility == View.VISIBLE

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    fun show(
        name: String,
        batteryPercentage: Int,
        context: Context,
        type: IslandType = IslandType.CONNECTED,
        reversed: Boolean = false,
        otherDeviceName: String? = null,
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { show(name, batteryPercentage, context, type, reversed, otherDeviceName) }
            return
        }
        if (isVisible || ServiceManager.getService()?.islandOpen == true) return

        isClosing = false
        currentType = type
        displayedBatterySource = null
        ServiceManager.getService()?.islandOpen = true

        configureContent(name, batteryPercentage, type, reversed, otherDeviceName)
        attachToWindow()
        if (!isVisible) return

        registerBatteryReceiver()
        ServiceManager.getService()?.sendBatteryBroadcast()
        installGestures()
        animateEntrance()
        resetAutoCloseTimer()
    }

    private fun configureContent(
        name: String,
        fallbackBatteryPercentage: Int,
        type: IslandType,
        reversed: Boolean,
        otherDeviceName: String?,
    ) {
        deviceText.text = name
        connectedText.text = when (type) {
            IslandType.CONNECTED -> context.getString(R.string.island_connected_text)
            IslandType.TAKING_OVER -> context.getString(R.string.island_taking_over_text)
            IslandType.MOVED_TO_REMOTE -> context.getString(R.string.island_moved_to_remote_text)
            IslandType.MOVED_TO_OTHER_DEVICE -> if (reversed) {
                context.getString(R.string.island_moved_to_other_device_reversed_text)
            } else {
                context.getString(
                    R.string.island_moved_to_other_device_text,
                    otherDeviceName.orEmpty(),
                )
            }
        }

        val showTakeBack = type == IslandType.MOVED_TO_OTHER_DEVICE && !reversed
        actionButton.visibility = if (showTakeBack) View.VISIBLE else View.GONE
        batteryText.visibility = if (showTakeBack) View.GONE else View.VISIBLE
        batteryProgress.visibility = if (showTakeBack) View.GONE else View.VISIBLE
        batteryBackground.visibility = if (showTakeBack) View.GONE else View.VISIBLE
        actionButton.setOnClickListener(if (showTakeBack) {
            View.OnClickListener {
                ServiceManager.getService()?.takeBackAudio()
                close()
            }
        } else null)

        val selection = BatteryDisplay.select(ServiceManager.getService()?.getBattery().orEmpty())
        renderBatteryLevel(
            selection?.level ?: fallbackBatteryPercentage.takeIf(BatteryLevels::isKnown),
        )
        updateBatteryArtwork(selection?.source, animate = false)
    }

    private fun attachToWindow() {
        containerView.removeAllViews()
        containerView.addView(
            islandView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        val metrics = context.resources.displayMetrics
        val width = min((metrics.widthPixels * 0.94f).toInt(), (420f * density).toInt())
        val params = WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (8f * density).toInt()
        }

        resetVisualProperties()
        islandView.visibility = View.VISIBLE
        containerView.visibility = View.VISIBLE
        try {
            windowManager.addView(containerView, params)
        } catch (error: Exception) {
            Log.e(TAG, "Unable to show fallback capsule", error)
            ServiceManager.getService()?.islandOpen = false
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerBatteryReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter(AirPodsNotifications.BATTERY_DATA).apply {
            addAction(AirPodsNotifications.DISCONNECT_RECEIVERS)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(batteryReceiver, filter)
            }
            receiverRegistered = true
        } catch (error: Exception) {
            Log.w(TAG, "Unable to register fallback capsule receiver", error)
        }
    }

    private fun unregisterBatteryReceiver() {
        if (!receiverRegistered) return
        receiverRegistered = false
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (error: IllegalArgumentException) {
            Log.d(TAG, "Fallback capsule receiver was already removed")
        }
    }

    private fun updateBatteryDisplay(batteries: ArrayList<Battery>?) {
        val selection = BatteryDisplay.select(batteries.orEmpty())
        renderBatteryLevel(selection?.level)
        updateBatteryArtwork(selection?.source, animate = true)
    }

    @SuppressLint("SetTextI18n")
    private fun renderBatteryLevel(level: Int?) {
        batteryText.text = level?.let(BatteryLevels::displayPercent) ?: "—"
        batteryProgress.progress = level ?: 0
        batteryProgress.isIndeterminate = false
    }

    private fun updateBatteryArtwork(source: BatteryDisplaySource?, animate: Boolean) {
        val targetSource = if (
            currentType == IslandType.CONNECTED && source == BatteryDisplaySource.CASE
        ) BatteryDisplaySource.CASE else BatteryDisplaySource.EARBUDS
        if (displayedBatterySource == targetSource) return
        displayedBatterySource = targetSource

        val model = ServiceManager.getService()?.airpodsInstance?.model
        val imageRes = when (targetSource) {
            BatteryDisplaySource.CASE -> OverlayMedia.caseImageRes(model)
            BatteryDisplaySource.EARBUDS -> OverlayMedia.fallbackImageRes(model)
        }
        artworkView.animate().cancel()
        if (!animate || !artworkView.isLaidOut) {
            artworkView.setImageResource(imageRes)
            artworkView.alpha = 1f
            artworkView.scaleX = 1f
            artworkView.scaleY = 1f
            return
        }
        artworkView.animate()
            .alpha(0f)
            .scaleX(0.94f)
            .scaleY(0.94f)
            .setDuration(80L)
            .withEndAction {
                artworkView.setImageResource(imageRes)
                artworkView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(140L)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            .start()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun installGestures() {
        containerView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    pauseAutoCloseTimer()
                    cancelMotion()
                    velocityTracker = VelocityTracker.obtain().also { it.addMovement(event) }
                    downRawY = event.rawY
                    startTranslationY = containerView.translationY
                    isDragging = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    velocityTracker?.addMovement(event)
                    val deltaY = event.rawY - downRawY
                    if (!isDragging && abs(deltaY) > touchSlop) isDragging = true
                    if (isDragging) {
                        val translation = if (deltaY > 0f) deltaY * 0.32f else deltaY * 0.9f
                        containerView.translationY = startTranslationY + translation
                        val upwardProgress = (-containerView.translationY / (96f * density)).coerceIn(0f, 1f)
                        containerView.alpha = 1f - upwardProgress * 0.35f
                        val scale = 1f - min(abs(translation) / (900f * density), 0.035f)
                        containerView.scaleX = scale
                        containerView.scaleY = scale
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    velocityTracker?.addMovement(event)
                    velocityTracker?.computeCurrentVelocity(1000)
                    val velocityY = velocityTracker?.yVelocity ?: 0f
                    val deltaY = event.rawY - downRawY
                    recycleVelocityTracker()
                    when {
                        !isDragging -> openAppAndDismiss()
                        velocityY < -1000f || deltaY < -48f * density -> animateDismiss(upward = true)
                        velocityY > 1000f || deltaY > 56f * density -> openAppAndDismiss()
                        else -> springBack(velocityY)
                    }
                    isDragging = false
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    recycleVelocityTracker()
                    isDragging = false
                    springBack(0f)
                    true
                }

                else -> false
            }
        }
    }

    private fun animateEntrance() {
        containerView.alpha = 0f
        containerView.scaleX = 0.94f
        containerView.scaleY = 0.94f
        containerView.translationY = -24f * density
        containerView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(260L)
            .setInterpolator(DecelerateInterpolator(1.7f))
            .setListener(null)
            .start()
    }

    private fun springBack(velocity: Float) {
        containerView.animate().cancel()
        containerView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(160L)
            .setListener(null)
            .start()
        returnSpring?.cancel()
        returnSpring = SpringAnimation(containerView, DynamicAnimation.TRANSLATION_Y, 0f).apply {
            setStartVelocity(velocity)
            spring = SpringForce(0f)
                .setDampingRatio(0.82f)
                .setStiffness(SpringForce.STIFFNESS_MEDIUM)
            addEndListener { _, canceled, _, _ ->
                if (!canceled && !isClosing) resetAutoCloseTimer()
            }
            start()
        }
    }

    private fun openAppAndDismiss() {
        if (isClosing) return
        ServiceManager.getService()?.startMainActivity()
        animateDismiss(upward = false)
    }

    private fun animateDismiss(upward: Boolean) {
        if (isClosing) return
        isClosing = true
        pauseAutoCloseTimer()
        unregisterBatteryReceiver()
        cancelMotion()
        val targetY = if (upward) -96f * density else 32f * density
        containerView.animate()
            .translationY(targetY)
            .alpha(0f)
            .scaleX(0.94f)
            .scaleY(0.94f)
            .setDuration(if (upward) 190L else 150L)
            .setInterpolator(AccelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) = cleanupAndRemoveView()
                override fun onAnimationCancel(animation: Animator) {
                    if (isClosing) cleanupAndRemoveView()
                }
            })
            .start()
    }

    fun close() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { close() }
            return
        }
        animateDismiss(upward = true)
    }

    fun forceClose() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { forceClose() }
            return
        }
        if (isClosing && containerView.parent == null) return
        isClosing = true
        pauseAutoCloseTimer()
        unregisterBatteryReceiver()
        cancelMotion()
        cleanupAndRemoveView()
    }

    private fun cleanupAndRemoveView() {
        pauseAutoCloseTimer()
        unregisterBatteryReceiver()
        recycleVelocityTracker()
        returnSpring?.cancel()
        returnSpring = null
        artworkView.animate().cancel()
        containerView.animate().setListener(null).cancel()
        containerView.setOnTouchListener(null)
        try {
            if (containerView.parent != null) windowManager.removeViewImmediate(containerView)
        } catch (error: Exception) {
            Log.w(TAG, "Unable to remove fallback capsule", error)
        }
        containerView.removeAllViews()
        ServiceManager.getService()?.islandOpen = false
        isClosing = false
        resetVisualProperties()
    }

    private fun resetVisualProperties() {
        containerView.alpha = 1f
        containerView.scaleX = 1f
        containerView.scaleY = 1f
        containerView.translationY = 0f
        artworkView.alpha = 1f
        artworkView.scaleX = 1f
        artworkView.scaleY = 1f
    }

    private fun resetAutoCloseTimer() {
        mainHandler.removeCallbacks(autoCloseRunnable)
        mainHandler.postDelayed(autoCloseRunnable, AUTO_CLOSE_DELAY_MS)
    }

    private fun pauseAutoCloseTimer() = mainHandler.removeCallbacks(autoCloseRunnable)

    private fun cancelMotion() {
        returnSpring?.cancel()
        returnSpring = null
        containerView.animate().setListener(null).cancel()
    }

    private fun recycleVelocityTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
    }

    private companion object {
        const val TAG = "IslandWindow"
        const val AUTO_CLOSE_DELAY_MS = 4_500L
    }
}
