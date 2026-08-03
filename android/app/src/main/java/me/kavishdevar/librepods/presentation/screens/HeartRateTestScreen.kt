/*
    LibrePods - AirPods liberated from Appleâ€™s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.
*/

package me.kavishdevar.librepods.presentation.screens

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.bluetooth.HeartRateSample
import me.kavishdevar.librepods.presentation.components.StyledToggle
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun HeartRateTestScreen(viewModel: AirPodsViewModel) {
    val state by viewModel.uiState.collectAsState()
    val materialDesign = LocalDesignSystem.current == DesignSystem.Material
    val topPadding = if (materialDesign) {
        16.dp
    } else {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 84.dp
    }
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp

    val latestSample = state.heartRateSamples.lastOrNull()
    val monitoringStatus = when {
        !state.heartRateMonitoringEnabled -> "Disabled"
        !state.isLocallyConnected -> "Enabled â€” waiting for connection"
        state.heartRateStreaming -> "Streaming"
        else -> "Enabled â€” awaiting stream"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(topPadding))

        StyledToggle(
            title = "Heart-rate test",
            label = "Enable monitoring",
            description = "Uses the existing AirPods AACP connection and remains enabled across reconnects.",
            checked = state.heartRateMonitoringEnabled,
            onCheckedChange = viewModel::setHeartRateMonitoringEnabled,
            header = true
        )

        Spacer(modifier = Modifier.height(4.dp))

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
                            text = latestSample?.bpm?.toString() ?: "â€”",
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
                            text = if (state.isLocallyConnected) "Connected" else "Disconnected",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (state.isLocallyConnected) {
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

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Recent samples",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        HeartRateGraph(samples = state.heartRateSamples)

        Text(
            text = "Experimental test data only. Do not use it for medical decisions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(bottomPadding))
    }
}

@Composable
private fun HeartRateGraph(samples: List<HeartRateSample>) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val pointColor = MaterialTheme.colorScheme.onSurface

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
                val chartHeight = size.height
                val chartWidth = size.width
                val minBpm = 30f
                val maxBpm = 220f

                listOf(30f, 60f, 100f, 140f, 180f, 220f).forEach { bpm ->
                    val y = chartHeight - ((bpm - minBpm) / (maxBpm - minBpm)) * chartHeight
                    drawLine(
                        color = gridColor,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(chartWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (samples.isNotEmpty()) {
                    val path = Path()
                    samples.forEachIndexed { index, sample ->
                        val x = if (samples.size == 1) {
                            chartWidth / 2f
                        } else {
                            index.toFloat() / (samples.size - 1).toFloat() * chartWidth
                        }
                        val normalized = ((sample.bpm.toFloat() - minBpm) / (maxBpm - minBpm))
                            .coerceIn(0f, 1f)
                        val y = chartHeight - normalized * chartHeight

                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        if (index == samples.lastIndex) {
                            drawCircle(
                                color = pointColor,
                                radius = 4.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(x, y)
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
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun formatLastUpdate(sample: HeartRateSample?): String {
    if (sample == null) return "No samples yet"
    return DateFormat.getTimeInstance(DateFormat.MEDIUM)
        .format(Date(sample.receivedAtMillis))
}


