/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.
*/

package me.kavishdevar.librepods.presentation.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import me.kavishdevar.librepods.bluetooth.HeartRateSample
import me.kavishdevar.librepods.health.HealthConnectExportStatus
import me.kavishdevar.librepods.health.HealthConnectHeartRateExporter
import me.kavishdevar.librepods.presentation.components.StyledToggle
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import java.text.DateFormat
import java.util.Date
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.round

@Composable
fun HeartRateTestScreen(viewModel: AirPodsViewModel) {
    val state by viewModel.uiState.collectAsState()
    val healthConnectPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions: Set<String> ->
        if (HealthConnectHeartRateExporter.WRITE_HEART_RATE_PERMISSION in grantedPermissions) {
            viewModel.setHealthConnectExportEnabled(true)
        } else {
            viewModel.markHealthConnectPermissionDenied()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshHealthConnectExportState()
    }

    val materialDesign = LocalDesignSystem.current == DesignSystem.Material
    val topPadding = if (materialDesign) {
        16.dp
    } else {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 84.dp
    }
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp

    val latestSample = state.heartRateSamples.lastOrNull()
    val monitoringStatus = monitoringStatus(
        enabled = state.heartRateMonitoringEnabled,
        connected = state.isLocallyConnected,
        streaming = state.heartRateStreaming
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(topPadding))

        HeartRateSummaryCard(
            latestSample = latestSample,
            connected = state.isLocallyConnected,
            monitoringStatus = monitoringStatus
        )

        Spacer(modifier = Modifier.height(16.dp))

        HealthConnectControls(
            status = state.healthConnectExportStatus,
            exportEnabled = state.healthConnectExportEnabled,
            detailedSamples = state.healthConnectDetailedSamples,
            onExportChanged = { enabled ->
                when {
                    !enabled -> viewModel.setHealthConnectExportEnabled(false)
                    state.healthConnectExportStatus.canEnableExport ->
                        viewModel.setHealthConnectExportEnabled(true)

                    state.healthConnectExportStatus.requiresPermissionRequest ->
                        healthConnectPermissionLauncher.launch(
                            HealthConnectHeartRateExporter.REQUIRED_PERMISSIONS
                        )
                }
            },
            onDetailedSamplesChanged = viewModel::setHealthConnectDetailedSamples
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Recent samples",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        HeartRateGraph(samples = state.heartRateSamples)

        Spacer(modifier = Modifier.height(bottomPadding))
    }
}

@Composable
private fun HeartRateSummaryCard(
    latestSample: HeartRateSample?,
    connected: Boolean,
    monitoringStatus: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = latestSample?.bpm?.toString() ?: EM_DASH,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "BPM",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (connected) "Connected" else "Disconnected",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (connected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = monitoringStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
            }

            Text(
                text = "Last update: ${formatLastUpdate(latestSample)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HealthConnectControls(
    status: HealthConnectExportStatus,
    exportEnabled: Boolean,
    detailedSamples: Boolean,
    onExportChanged: (Boolean) -> Unit,
    onDetailedSamplesChanged: (Boolean) -> Unit
) {
    val available = status.isAvailable

    StyledToggle(
        title = "Health Connect",
        label = "Save heart-rate samples",
        description = healthConnectDescription(status, detailedSamples),
        checked = exportEnabled,
        enabled = available,
        onCheckedChange = onExportChanged
    )

    Spacer(modifier = Modifier.height(8.dp))

    StyledToggle(
        title = null,
        label = "Detailed samples",
        description = if (detailedSamples) {
            "Save heart-rate data every second."
        } else {
            "Export one average BPM for each minute. AirPods sampling is unchanged."
        },
        checked = detailedSamples,
        enabled = available,
        onCheckedChange = onDetailedSamplesChanged
    )
}

private fun monitoringStatus(
    enabled: Boolean,
    connected: Boolean,
    streaming: Boolean
): String = when {
    !enabled -> "Disabled"
    !connected -> "Enabled — waiting for connection"
    streaming -> "Streaming"
    else -> "Enabled — awaiting valid sample"
}

private val HealthConnectExportStatus.isAvailable: Boolean
    get() = this != HealthConnectExportStatus.UNAVAILABLE &&
        this != HealthConnectExportStatus.UPDATE_REQUIRED

private val HealthConnectExportStatus.canEnableExport: Boolean
    get() = this == HealthConnectExportStatus.READY ||
        this == HealthConnectExportStatus.ENABLED

private val HealthConnectExportStatus.requiresPermissionRequest: Boolean
    get() = this == HealthConnectExportStatus.PERMISSION_REQUIRED ||
        this == HealthConnectExportStatus.PERMISSION_DENIED ||
        this == HealthConnectExportStatus.ERROR

private fun healthConnectDescription(
    status: HealthConnectExportStatus,
    detailedSamples: Boolean
): String = when (status) {
    HealthConnectExportStatus.UNAVAILABLE ->
        "Health Connect is not available on this device."

    HealthConnectExportStatus.UPDATE_REQUIRED ->
        "Install or update Health Connect to save heart-rate samples."

    HealthConnectExportStatus.PERMISSION_REQUIRED ->
        "Write permission is required before samples can be saved."

    HealthConnectExportStatus.PERMISSION_DENIED ->
        "Permission was denied. Turn this on to request it again."

    HealthConnectExportStatus.READY ->
        "Available. Enable this to save validated samples on this device."

    HealthConnectExportStatus.ENABLED -> if (detailedSamples) {
        "Validated heart-rate data is saved every second."
    } else {
        "Validated samples are averaged into one Health Connect record per minute."
    }

    HealthConnectExportStatus.ERROR ->
        "A write failed. Buffered samples will be retried without creating duplicates."
}

@Composable
private fun HeartRateGraph(samples: List<HeartRateSample>) {
    val chartScale = remember(samples) {
        calculateHeartRateChartScale(samples.map { it.bpm.toFloat() })
    }
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
    val axisLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)
    val pointColor = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current
    val axisLabelPaint = remember(axisColor, density) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = axisColor.toArgb()
            textSize = with(density) { 11.sp.toPx() }
            textAlign = Paint.Align.RIGHT
        }
    }
    val axisTitlePaint = remember(axisColor, density) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = axisColor.toArgb()
            textSize = with(density) { 9.sp.toPx() }
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val plotLeft = CHART_AXIS_WIDTH.toPx()
                val plotRight = size.width
                val plotTop = CHART_TOP_INSET.toPx()
                val plotBottom = size.height - CHART_BOTTOM_INSET.toPx()
                val plotWidth = (plotRight - plotLeft).coerceAtLeast(0f)
                val plotHeight = (plotBottom - plotTop).coerceAtLeast(0f)
                val labelX = plotLeft - CHART_AXIS_LABEL_GAP.toPx()
                val labelMetrics = axisLabelPaint.fontMetrics
                val labelBaselineOffset = -(labelMetrics.ascent + labelMetrics.descent) / 2f
                val titleMetrics = axisTitlePaint.fontMetrics

                drawContext.canvas.nativeCanvas.drawText(
                    "BPM",
                    plotLeft / 2f,
                    -titleMetrics.ascent,
                    axisTitlePaint
                )

                drawLine(
                    color = axisLineColor,
                    start = Offset(plotLeft, plotTop),
                    end = Offset(plotLeft, plotBottom),
                    strokeWidth = 1.dp.toPx()
                )

                chartScale.gridLines.forEach { bpm ->
                    val normalized =
                        (bpm - chartScale.minBpm) / chartScale.spanBpm
                    val y = plotBottom - normalized * plotHeight

                    drawLine(
                        color = gridColor,
                        start = Offset(plotLeft, y),
                        end = Offset(plotRight, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        bpm.toInt().toString(),
                        labelX,
                        y + labelBaselineOffset,
                        axisLabelPaint
                    )
                }

                if (samples.isNotEmpty()) {
                    val path = Path()
                    samples.forEachIndexed { index, sample ->
                        val x = sampleX(
                            index = index,
                            sampleCount = samples.size,
                            plotLeft = plotLeft,
                            plotWidth = plotWidth
                        )
                        val y = chartScale.bpmY(
                            bpm = sample.bpm.toFloat(),
                            plotBottom = plotBottom,
                            plotHeight = plotHeight
                        )

                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        if (index == samples.lastIndex) {
                            drawCircle(
                                color = pointColor,
                                radius = 4.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }
                    }
                    if (samples.size > 1) {
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }
            }

            if (samples.isEmpty()) {
                Text(
                    text = "Waiting for validated heart-rate samples",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = CHART_AXIS_WIDTH)
                )
            }
        }
    }
}

private data class HeartRateChartScale(
    val minBpm: Float,
    val maxBpm: Float,
    val gridLines: List<Float>
) {
    val spanBpm: Float
        get() = maxBpm - minBpm

    fun bpmY(bpm: Float, plotBottom: Float, plotHeight: Float): Float {
        val normalized = ((bpm - minBpm) / spanBpm).coerceIn(0f, 1f)
        return plotBottom - normalized * plotHeight
    }
}

private fun sampleX(
    index: Int,
    sampleCount: Int,
    plotLeft: Float,
    plotWidth: Float
): Float = if (sampleCount == 1) {
    plotLeft + plotWidth / 2f
} else {
    plotLeft + index.toFloat() / (sampleCount - 1).toFloat() * plotWidth
}

private fun calculateHeartRateChartScale(bpms: List<Float>): HeartRateChartScale {
    if (bpms.isEmpty()) {
        return createHeartRateChartScale(
            minBpm = CHART_DEFAULT_MIN_BPM,
            maxBpm = CHART_DEFAULT_MAX_BPM
        )
    }

    val dataMin = bpms.minOrNull()!!
    val dataMax = bpms.maxOrNull()!!
    val dataRange = dataMax - dataMin
    val margin = max(CHART_MIN_MARGIN_BPM, dataRange * CHART_MARGIN_FRACTION)
    val requiredSpan = dataRange + margin * 2f

    val initialBounds = if (requiredSpan <= CHART_MIN_SPAN_BPM) {
        val center = (dataMin + dataMax) / 2f
        val roundedCenter = roundToIncrement(center, CHART_NARROW_CENTER_INCREMENT_BPM)
        val halfSpan = CHART_MIN_SPAN_BPM / 2f
        roundedCenter - halfSpan to roundedCenter + halfSpan
    } else {
        val boundIncrement = if (requiredSpan <= CHART_FINE_BOUND_THRESHOLD_BPM) {
            CHART_FINE_BOUND_INCREMENT_BPM
        } else {
            CHART_COARSE_BOUND_INCREMENT_BPM
        }
        floorToIncrement(dataMin - margin, boundIncrement) to
            ceilToIncrement(dataMax + margin, boundIncrement)
    }

    val constrainedBounds = constrainHeartRateBounds(
        minBpm = initialBounds.first,
        maxBpm = initialBounds.second,
        dataMin = dataMin,
        dataMax = dataMax
    )

    return createHeartRateChartScale(
        minBpm = constrainedBounds.first,
        maxBpm = constrainedBounds.second
    )
}

private fun constrainHeartRateBounds(
    minBpm: Float,
    maxBpm: Float,
    dataMin: Float,
    dataMax: Float
): Pair<Float, Float> {
    val preferredBounds = fitBoundsWithinLimits(
        minBpm = minBpm,
        maxBpm = maxBpm,
        dataMin = dataMin,
        dataMax = dataMax,
        limitMin = CHART_SAFETY_MIN_BPM,
        limitMax = CHART_SAFETY_MAX_BPM
    )
    return fitBoundsWithinLimits(
        minBpm = preferredBounds.first,
        maxBpm = preferredBounds.second,
        dataMin = dataMin,
        dataMax = dataMax,
        limitMin = CHART_OUTER_MIN_BPM,
        limitMax = CHART_OUTER_MAX_BPM
    )
}

private fun fitBoundsWithinLimits(
    minBpm: Float,
    maxBpm: Float,
    dataMin: Float,
    dataMax: Float,
    limitMin: Float,
    limitMax: Float
): Pair<Float, Float> {
    val safetyInset = CHART_MIN_MARGIN_BPM
    if (dataMin < limitMin + safetyInset || dataMax > limitMax - safetyInset) {
        return minBpm to maxBpm
    }

    val span = maxBpm - minBpm
    val limitSpan = limitMax - limitMin
    if (span >= limitSpan) {
        return limitMin to limitMax
    }

    var adjustedMin = minBpm
    var adjustedMax = maxBpm
    if (adjustedMin < limitMin) {
        val shift = limitMin - adjustedMin
        adjustedMin += shift
        adjustedMax += shift
    }
    if (adjustedMax > limitMax) {
        val shift = adjustedMax - limitMax
        adjustedMin -= shift
        adjustedMax -= shift
    }
    return adjustedMin to adjustedMax
}

private fun createHeartRateChartScale(
    minBpm: Float,
    maxBpm: Float
): HeartRateChartScale {
    val span = (maxBpm - minBpm).coerceAtLeast(CHART_MIN_SPAN_BPM)
    val adjustedMax = minBpm + span
    val tickStep = calculateHeartRateTickStep(span)
    val intervalCount = floor(span / tickStep).toInt()
    val gridLines = (0..intervalCount).map { index ->
        minBpm + index * tickStep
    }

    return HeartRateChartScale(
        minBpm = minBpm,
        maxBpm = adjustedMax,
        gridLines = gridLines
    )
}

private fun calculateHeartRateTickStep(spanBpm: Float): Float {
    val rawStep = spanBpm / CHART_TARGET_GRID_INTERVALS
    val increment = if (rawStep <= CHART_FINE_TICK_THRESHOLD_BPM) {
        CHART_FINE_TICK_INCREMENT_BPM
    } else {
        CHART_COARSE_TICK_INCREMENT_BPM
    }
    var step = max(increment, roundToIncrement(rawStep, increment))

    while (floor(spanBpm / step).toInt() + 1 > CHART_MAX_GRID_LINES) {
        step += increment
    }
    return step
}

private fun roundToIncrement(value: Float, increment: Float): Float =
    round(value / increment) * increment

private fun floorToIncrement(value: Float, increment: Float): Float =
    floor(value / increment) * increment

private fun ceilToIncrement(value: Float, increment: Float): Float =
    ceil(value / increment) * increment

private fun formatLastUpdate(sample: HeartRateSample?): String {
    if (sample == null) return "No samples yet"
    return DateFormat.getTimeInstance(DateFormat.MEDIUM)
        .format(Date(sample.receivedAtMillis))
}

private const val EM_DASH = "—"
private const val CHART_DEFAULT_MIN_BPM = 60f
private const val CHART_DEFAULT_MAX_BPM = 100f
private const val CHART_MIN_SPAN_BPM = 40f
private const val CHART_MIN_MARGIN_BPM = 5f
private const val CHART_MARGIN_FRACTION = 0.10f
private const val CHART_NARROW_CENTER_INCREMENT_BPM = 5f
private const val CHART_FINE_BOUND_THRESHOLD_BPM = 80f
private const val CHART_FINE_BOUND_INCREMENT_BPM = 5f
private const val CHART_COARSE_BOUND_INCREMENT_BPM = 10f
private const val CHART_SAFETY_MIN_BPM = 20f
private const val CHART_SAFETY_MAX_BPM = 240f
private const val CHART_OUTER_MIN_BPM = 0f
private const val CHART_OUTER_MAX_BPM = 260f
private const val CHART_TARGET_GRID_INTERVALS = 5f
private const val CHART_FINE_TICK_THRESHOLD_BPM = 25f
private const val CHART_FINE_TICK_INCREMENT_BPM = 5f
private const val CHART_COARSE_TICK_INCREMENT_BPM = 10f
private const val CHART_MAX_GRID_LINES = 7
private val CHART_AXIS_WIDTH = 42.dp
private val CHART_AXIS_LABEL_GAP = 8.dp
private val CHART_TOP_INSET = 20.dp
private val CHART_BOTTOM_INSET = 8.dp
