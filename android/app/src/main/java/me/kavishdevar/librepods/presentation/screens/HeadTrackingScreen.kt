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
package me.kavishdevar.librepods.presentation.screens

import android.graphics.Paint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.StyledButton
import me.kavishdevar.librepods.presentation.components.StyledToggle
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsUiState
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import me.kavishdevar.librepods.services.ServiceManager
import me.kavishdevar.librepods.utils.HeadTracking
import kotlin.math.abs
import kotlin.math.ceil

@ExperimentalHazeMaterialsApi
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HeadTrackingScreen(viewModel: AirPodsViewModel, navigateToPurchase: () -> Unit) {
    val initialState = remember(viewModel) {
        viewModel.uiState.value.toHeadTrackingScreenState()
    }
    val screenStateFlow = remember(viewModel) {
        viewModel.uiState
            .map { it.toHeadTrackingScreenState() }
            .distinctUntilChanged()
    }
    val state by screenStateFlow.collectAsStateWithLifecycle(initialValue = initialState)
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(viewModel, lifecycleOwner) {
        var startedByScreen = false
        fun startTracking() {
            if (startedByScreen) return
            viewModel.startHeadTracking()
            startedByScreen = viewModel.uiState.value.headTrackingActive
        }
        fun stopTracking() {
            if (startedByScreen || viewModel.uiState.value.headTrackingActive) {
                viewModel.stopHeadTracking()
            }
            startedByScreen = false
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> startTracking()
                Lifecycle.Event.ON_STOP -> stopTracking()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            startTracking()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stopTracking()
        }
    }

    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val backdrop = rememberLayerBackdrop()
    val designSystem = LocalDesignSystem.current
    val m3eEnabled = designSystem == DesignSystem.Material
    val topPadding = if (m3eEnabled) 0.dp else WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 84.dp
    val bottomPadding = if (m3eEnabled) 0.dp else WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp
    var gestureText by remember { mutableStateOf("") }
    var testingGestures by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val gesturePrompt = stringResource(R.string.shake_your_head_or_nod)

    LaunchedEffect(gestureText, testingGestures) {
        if (gestureText.isNotEmpty() && !testingGestures) {
            delay(3_000)
            gestureText = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(
                if (designSystem == DesignSystem.Apple) {
                    Modifier.layerBackdrop(backdrop)
                } else {
                    Modifier
                }
            )
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(topPadding))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp)
        ) {
            if (!state.isPremium) {
                StyledButton(
                    onClick = navigateToPurchase,
                    backdrop = backdrop,
                    modifier = Modifier.fillMaxWidth(),
                    maxScale = 0.05f,
                    surfaceColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        stringResource(R.string.unlock_advanced_features),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            StyledToggle(
                label = stringResource(R.string.head_gestures),
                checked = state.headGesturesEnabled,
                onCheckedChange = { viewModel.setHeadGesturesEnabled(it) },
                enabled = state.isPremium || state.headGesturesEnabled,
                description = stringResource(R.string.head_gestures_details),
                header = true
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Velocity",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = textColor.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 8.dp)
            )
            Plot(isActive = state.headTrackingActive)
        }

        Spacer(modifier = Modifier.height(16.dp))

        StyledButton(
            onClick = {
                if (testingGestures) return@StyledButton
                testingGestures = true
                gestureText = gesturePrompt
                coroutineScope.launch {
                    val accepted = runCatching {
                        withTimeoutOrNull(10_000) {
                            ServiceManager.getService()?.testHeadGestures()
                        }
                    }.getOrNull()
                    gestureText = when (accepted) {
                        true -> "\"Yes\" gesture detected."
                        false -> "\"No\" gesture detected."
                        null -> "No gesture detected. Try again."
                    }
                    testingGestures = false
                }
            },
            backdrop = backdrop,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            maxScale = 0.05f,
            enabled = !testingGestures && state.headTrackingActive
        ) {
            Text(
                "Test Head Gestures",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .padding(top = 12.dp, bottom = 24.dp)
        ) {
            AnimatedContent(
                targetState = gestureText,
                label = "head gesture result",
                transitionSpec = {
                    (fadeIn(
                        animationSpec = tween(300)
                    ) + slideInVertically(
                        initialOffsetY = { 40 },
                        animationSpec = tween(300)
                    )).togetherWith(fadeOut(animationSpec = tween(150)))
                }
            ) { text ->
                if (text.isNotEmpty()) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(bottomPadding))
    }
}

@Immutable
private data class HeadTrackingScreenState(
    val isPremium: Boolean,
    val headGesturesEnabled: Boolean,
    val headTrackingActive: Boolean,
)

private fun AirPodsUiState.toHeadTrackingScreenState() = HeadTrackingScreenState(
    isPremium = isPremium,
    headGesturesEnabled = headGesturesEnabled,
    headTrackingActive = headTrackingActive,
)

@Composable
private fun Plot(isActive: Boolean) {
    val plotBuffer = remember { HeadTrackingPlotBuffer(PLOT_POINT_COUNT) }
    val redrawTick = remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    LaunchedEffect(lifecycleOwner, isActive) {
        if (!isActive) {
            plotBuffer.clear()
            redrawTick.intValue++
            return@LaunchedEffect
        }
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            HeadTracking.acceleration.collect { acceleration ->
                plotBuffer.add(
                    horizontal = acceleration.horizontal,
                    vertical = acceleration.vertical
                )
                redrawTick.intValue++
            }
        }
    }

    val horizontalColor = MaterialTheme.colorScheme.primary
    val verticalColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val zeroLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
    val labelColor = MaterialTheme.colorScheme.onSurface
    val horizontalPath = remember { Path() }
    val verticalPath = remember { Path() }
    val plotStroke = remember(density) {
        Stroke(
            width = with(density) { 2.dp.toPx() },
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    }
    val labelPaint = remember(density) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = with(density) { 11.sp.toPx() }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics {
                        contentDescription = "Live head movement velocity chart"
                    }
            ) {
                val sampleCount = redrawTick.intValue.let { plotBuffer.size }
                val chartLeft = 44.dp.toPx()
                val chartRight = size.width - 8.dp.toPx()
                val chartTop = 38.dp.toPx()
                val chartBottom = size.height - 16.dp.toPx()
                val chartWidth = (chartRight - chartLeft).coerceAtLeast(1f)
                val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)
                val zeroY = chartTop + chartHeight / 2f
                val scaleMax = plotBuffer.scaleMax
                val yScale = chartHeight / (scaleMax * 2f)

                for (i in 0..10) {
                    val x = chartLeft + chartWidth * i / 10f
                    drawLine(
                        color = gridColor,
                        start = Offset(x, chartTop),
                        end = Offset(x, chartBottom),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                for (i in -4..4) {
                    val y = zeroY - (scaleMax * i / 4f) * yScale
                    drawLine(
                        color = gridColor,
                        start = Offset(chartLeft, y),
                        end = Offset(chartRight, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                drawLine(
                    color = zeroLineColor,
                    start = Offset(chartLeft, zeroY),
                    end = Offset(chartRight, zeroY),
                    strokeWidth = 1.5f.dp.toPx()
                )

                if (sampleCount > 1) {
                    horizontalPath.reset()
                    verticalPath.reset()
                    val firstSlot = PLOT_POINT_COUNT - sampleCount

                    for (i in 0 until sampleCount) {
                        val slot = firstSlot + i
                        val x = chartLeft + chartWidth * slot / (PLOT_POINT_COUNT - 1f)
                        val horizontalY = zeroY - plotBuffer.horizontalAt(i) * yScale
                        val verticalY = zeroY - plotBuffer.verticalAt(i) * yScale
                        if (i == 0) {
                            horizontalPath.moveTo(x, horizontalY)
                            verticalPath.moveTo(x, verticalY)
                        } else {
                            horizontalPath.lineTo(x, horizontalY)
                            verticalPath.lineTo(x, verticalY)
                        }
                    }

                    drawPath(horizontalPath, color = horizontalColor, style = plotStroke)
                    drawPath(verticalPath, color = verticalColor, style = plotStroke)
                }

                labelPaint.color = labelColor.toArgb()
                drawContext.canvas.nativeCanvas.apply {
                    labelPaint.textAlign = Paint.Align.RIGHT
                    drawText(plotBuffer.scaleLabel, chartLeft - 8.dp.toPx(), chartTop + 4.dp.toPx(), labelPaint)
                    drawText("0", chartLeft - 8.dp.toPx(), zeroY + 4.dp.toPx(), labelPaint)
                    drawText(plotBuffer.negativeScaleLabel, chartLeft - 8.dp.toPx(), chartBottom, labelPaint)
                }

                val legendY = 14.dp.toPx()
                val legendTextY = legendY + 4.dp.toPx()
                val firstLegendX = chartLeft
                val secondLegendX = chartLeft + 104.dp.toPx()

                drawCircle(horizontalColor, 4.dp.toPx(), Offset(firstLegendX, legendY))
                drawCircle(verticalColor, 4.dp.toPx(), Offset(secondLegendX, legendY))
                drawContext.canvas.nativeCanvas.apply {
                    labelPaint.textAlign = Paint.Align.LEFT
                    drawText("Horizontal", firstLegendX + 8.dp.toPx(), legendTextY, labelPaint)
                    drawText("Vertical", secondLegendX + 8.dp.toPx(), legendTextY, labelPaint)
                }
            }

            if (!isActive) {
                Text(
                    text = "Connect AirPods to view live head tracking",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp)
                )
            }
        }
    }
}

private const val PLOT_POINT_COUNT = 100
private const val MIN_PLOT_SCALE = 1_000f
private const val PLOT_SCALE_STEP = 500f

private class HeadTrackingPlotBuffer(private val capacity: Int) {
    private val horizontal = FloatArray(capacity)
    private val vertical = FloatArray(capacity)
    private var nextIndex = 0
    private var scaleCooldown = 0

    var size: Int = 0
        private set

    var scaleMax: Float = MIN_PLOT_SCALE
        private set

    var scaleLabel: String = MIN_PLOT_SCALE.toInt().toString()
        private set

    var negativeScaleLabel: String = "-${MIN_PLOT_SCALE.toInt()}"
        private set

    fun add(horizontal: Float, vertical: Float) {
        this.horizontal[nextIndex] = horizontal
        this.vertical[nextIndex] = vertical
        nextIndex = (nextIndex + 1) % capacity
        if (size < capacity) size++

        var peak = MIN_PLOT_SCALE / 1.2f
        for (index in 0 until size) {
            peak = maxOf(peak, abs(horizontalAt(index)), abs(verticalAt(index)))
        }
        val targetScale = maxOf(
            MIN_PLOT_SCALE,
            ceil(((peak * 1.2f) / PLOT_SCALE_STEP).toDouble()).toFloat() * PLOT_SCALE_STEP
        )

        when {
            targetScale > scaleMax -> updateScale(targetScale)
            targetScale < scaleMax - PLOT_SCALE_STEP && ++scaleCooldown >= 10 -> {
                updateScale(maxOf(targetScale, scaleMax - PLOT_SCALE_STEP))
            }
        }
    }

    fun horizontalAt(position: Int): Float = horizontal[indexAt(position)]

    fun verticalAt(position: Int): Float = vertical[indexAt(position)]

    fun clear() {
        nextIndex = 0
        size = 0
        updateScale(MIN_PLOT_SCALE)
    }

    private fun indexAt(position: Int): Int {
        val oldestIndex = if (size == capacity) nextIndex else 0
        return (oldestIndex + position) % capacity
    }

    private fun updateScale(value: Float) {
        scaleMax = value
        scaleCooldown = 0
        scaleLabel = value.toInt().toString()
        negativeScaleLabel = "-${value.toInt()}"
    }
}
