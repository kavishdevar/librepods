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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
import me.kavishdevar.librepods.health.HealthConnectExportState
import me.kavishdevar.librepods.health.HealthConnectExportStatus
import me.kavishdevar.librepods.health.HealthConnectHeartRateExporter
import me.kavishdevar.librepods.presentation.components.HeartRateStatusChip
import me.kavishdevar.librepods.presentation.components.StyledSwitch
import me.kavishdevar.librepods.presentation.components.StyledToggle
import me.kavishdevar.librepods.presentation.components.rememberHeartRateSampleIsDisplayable
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import me.kavishdevar.librepods.services.HeartRateMonitoringState
import me.kavishdevar.librepods.services.HeartRateMonitoringStatus
import java.text.DateFormat
import java.util.Date
import kotlin.math.ceil
import kotlin.math.floor

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

    val heartRate = state.heartRate
    val healthConnect = state.healthConnect
    val sampleIsDisplayable = rememberHeartRateSampleIsDisplayable(
        sample = heartRate.latestSample,
        monitoringStatus = heartRate.status
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
            state = heartRate,
            sampleIsDisplayable = sampleIsDisplayable,
            onReconnectAacp = viewModel::reconnectAacpForHeartRate,
            onMonitoringChanged = viewModel::setHeartRateMonitoringEnabled
        )

        Spacer(modifier = Modifier.height(16.dp))

        HealthConnectControls(
            state = healthConnect,
            onExportChanged = { enabled ->
                when {
                    !enabled -> viewModel.setHealthConnectExportEnabled(false)
                    healthConnect.status.canEnableExport ->
                        viewModel.setHealthConnectExportEnabled(true)

                    healthConnect.status.requiresPermissionRequest ->
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

        Text(
            text = formatGraphSummary(heartRate.samples),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        HeartRateGraph(samples = heartRate.samples)

        Spacer(modifier = Modifier.height(bottomPadding))
    }
}

@Composable
private fun HeartRateSummaryCard(
    state: HeartRateMonitoringState,
    sampleIsDisplayable: Boolean,
    onReconnectAacp: () -> Unit,
    onMonitoringChanged: (Boolean) -> Unit
) {
    val materialDesign = LocalDesignSystem.current == DesignSystem.Material

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (materialDesign) 24.dp else 28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(if (materialDesign) 12.dp else 14.dp)
        ) {
            when (LocalDesignSystem.current) {
                DesignSystem.Material -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = state.latestSample?.takeIf { sampleIsDisplayable }?.bpm?.toString() ?: EM_DASH,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "BPM",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.enabled,
                            onCheckedChange = onMonitoringChanged,
                            modifier = Modifier.scale(MATERIAL_SWITCH_SCALE)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatLastReading(state.latestSample),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        HeartRateStatusChip(
                            status = state.status,
                            onRetry = onReconnectAacp.takeIf {
                                state.status == HeartRateMonitoringStatus.COULDNT_START
                            },
                            compact = true
                        )
                    }
                }

                DesignSystem.Apple -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = state.latestSample?.takeIf { sampleIsDisplayable }?.bpm?.toString() ?: EM_DASH,
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
                            Box(modifier = Modifier.padding(end = 4.dp, bottom = 8.dp)) {
                                StyledSwitch(
                                    checked = state.enabled,
                                    onCheckedChange = onMonitoringChanged
                                )
                            }
                            HeartRateStatusChip(
                                status = state.status,
                                onRetry = onReconnectAacp.takeIf {
                                    state.status == HeartRateMonitoringStatus.COULDNT_START
                                }
                            )
                        }
                    }

                    Text(
                        text = formatLastReading(state.latestSample),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthConnectControls(
    state: HealthConnectExportState,
    onExportChanged: (Boolean) -> Unit,
    onDetailedSamplesChanged: (Boolean) -> Unit
) {
    val available = state.status.isAvailable

    StyledToggle(
        title = "Health Connect",
        label = "Save heart-rate samples",
        description = healthConnectDescription(state.status, state.detailedSamples),
        checked = state.enabled,
        enabled = available,
        onCheckedChange = onExportChanged
    )

    Spacer(modifier = Modifier.height(8.dp))

    StyledToggle(
        title = null,
        label = "Detailed samples",
        description = if (state.detailedSamples) {
            "Save one BPM record every second."
        } else {
            "Save one average BPM record every minute."
        },
        checked = state.detailedSamples,
        enabled = available,
        onCheckedChange = onDetailedSamplesChanged
    )
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

private fun formatGraphSummary(samples: List<HeartRateSample>): String {
    if (samples.isEmpty()) return "Min $EM_DASH · Avg $EM_DASH · Max $EM_DASH BPM"

    val min = samples.minOf { it.bpm }
    val max = samples.maxOf { it.bpm }
    val average = samples.sumOf { it.bpm }.toFloat() / samples.size
    return "Min $min · Avg ${average.toInt()} · Max $max BPM"
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
        return HeartRateChartScale(
            minBpm = CHART_DEFAULT_MIN_BPM,
            maxBpm = CHART_DEFAULT_MAX_BPM,
            gridLines = listOf(60f, 70f, 80f, 90f, 100f)
        )
    }

    val dataMin = bpms.minOrNull() ?: CHART_DEFAULT_MIN_BPM
    val dataMax = bpms.maxOrNull() ?: CHART_DEFAULT_MAX_BPM
    val paddedMin = dataMin - CHART_MARGIN_BPM
    val paddedMax = dataMax + CHART_MARGIN_BPM
    val requestedSpan = maxOf(paddedMax - paddedMin, CHART_MIN_SPAN_BPM)
    val center = (paddedMin + paddedMax) / 2f
    val roughMin = center - requestedSpan / 2f
    val roughMax = center + requestedSpan / 2f
    val tickStep = niceTickStep(requestedSpan / CHART_TARGET_GRID_INTERVALS)

    var minBpm = floor(roughMin / tickStep) * tickStep
    var maxBpm = ceil(roughMax / tickStep) * tickStep
    if (minBpm < CHART_OUTER_MIN_BPM) {
        maxBpm -= minBpm - CHART_OUTER_MIN_BPM
        minBpm = CHART_OUTER_MIN_BPM
    }
    if (maxBpm > CHART_OUTER_MAX_BPM) {
        minBpm -= maxBpm - CHART_OUTER_MAX_BPM
        maxBpm = CHART_OUTER_MAX_BPM
    }

    val gridLines = buildList {
        var value = minBpm
        while (value <= maxBpm + 0.01f) {
            add(value)
            value += tickStep
        }
    }
    return HeartRateChartScale(minBpm, maxBpm, gridLines)
}

private fun niceTickStep(rawStep: Float): Float =
    CHART_TICK_STEPS.firstOrNull { it >= rawStep } ?: CHART_TICK_STEPS.last()

private fun formatLastReading(sample: HeartRateSample?): String {
    if (sample == null) return "Last reading: $EM_DASH"
    val time = DateFormat.getTimeInstance(DateFormat.SHORT)
        .format(Date(sample.receivedAtMillis))
    return "Last reading: ${sample.bpm} BPM at $time"
}

private const val EM_DASH = "—"
private const val MATERIAL_SWITCH_SCALE = 0.82f
private const val CHART_DEFAULT_MIN_BPM = 60f
private const val CHART_DEFAULT_MAX_BPM = 100f
private const val CHART_MIN_SPAN_BPM = 40f
private const val CHART_MARGIN_BPM = 5f
private const val CHART_OUTER_MIN_BPM = 0f
private const val CHART_OUTER_MAX_BPM = 260f
private const val CHART_TARGET_GRID_INTERVALS = 5f
private val CHART_TICK_STEPS = listOf(5f, 10f, 20f, 25f, 50f)
private val CHART_AXIS_WIDTH = 42.dp
private val CHART_AXIS_LABEL_GAP = 8.dp
private val CHART_TOP_INSET = 20.dp
private val CHART_BOTTOM_INSET = 8.dp
