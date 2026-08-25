/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.
*/

package me.kavishdevar.librepods.presentation.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.diagnostics.ConnectionDiagnosticsSnapshot
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.services.ServiceManager

private const val DIAGNOSTICS_REFRESH_INTERVAL_MS = 1_000L

@Composable
fun ConnectionHealthScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val materialDesign = LocalDesignSystem.current == DesignSystem.Material
    var snapshot by remember { mutableStateOf<ConnectionDiagnosticsSnapshot?>(null) }

    LaunchedEffect(Unit) {
        while (isActive) {
            snapshot = ServiceManager.getService()?.connectionDiagnosticsSnapshot()
            delay(DIAGNOSTICS_REFRESH_INTERVAL_MS)
        }
    }
    val currentSnapshot = snapshot

    val topPadding = if (materialDesign) {
        16.dp
    } else {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 84.dp
    }
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 20.dp

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "summary") {
            HealthSummary(snapshot = currentSnapshot)
        }

        if (currentSnapshot == null) {
            item(key = "waiting") {
                HealthCard {
                    Text(
                        text = stringResource(R.string.connection_health_waiting),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.connection_health_waiting_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            val current = currentSnapshot

            item(key = "connection") {
                HealthSection(
                    title = stringResource(R.string.connection_health_connection),
                    rows = listOf(
                        HealthRow(
                            stringResource(R.string.connection_health_device),
                            current.deviceName,
                        ),
                        HealthRow(
                            stringResource(R.string.connection_health_sound),
                            stringResource(
                                if (current.bluetoothAudioConnected) {
                                    R.string.connection_health_connected
                                } else {
                                    R.string.connection_health_not_connected
                                }
                            ),
                            positive = current.bluetoothAudioConnected,
                        ),
                        HealthRow(
                            stringResource(R.string.connection_health_controls),
                            stringResource(
                                if (current.controlChannelConnected) {
                                    R.string.connection_health_available
                                } else {
                                    R.string.connection_health_unavailable
                                }
                            ),
                            positive = current.controlChannelConnected,
                        ),
                    ),
                )
            }

            item(key = "battery") {
                HealthSection(
                    title = stringResource(R.string.connection_health_battery),
                    rows = listOf(
                        HealthRow(stringResource(R.string.connection_health_left_airpod), current.leftBattery),
                        HealthRow(stringResource(R.string.connection_health_right_airpod), current.rightBattery),
                        HealthRow(stringResource(R.string.connection_health_case), current.caseBattery),
                        HealthRow(
                            stringResource(R.string.connection_health_battery_updated),
                            current.batteryFreshnessLabel,
                        ),
                    ),
                )
            }

            item(key = "features") {
                HealthSection(
                    title = stringResource(R.string.connection_health_features),
                    rows = listOf(
                        HealthRow(
                            stringResource(R.string.connection_health_head_movement),
                            stringResource(
                                if (current.headTrackingActive) {
                                    R.string.connection_health_active
                                } else {
                                    R.string.connection_health_inactive
                                }
                            ),
                            positive = current.headTrackingActive,
                        ),
                        HealthRow(
                            stringResource(R.string.connection_health_head_movement_update),
                            current.lastHeadTrackingPacketLabel,
                        ),
                        HealthRow(
                            stringResource(R.string.connection_health_connection_alert),
                            stringResource(
                                if (current.liveAlertEnabled) {
                                    R.string.connection_health_on
                                } else {
                                    R.string.connection_health_off
                                }
                            ),
                            positive = current.liveAlertEnabled,
                        ),
                    ),
                )
            }

            item(key = "compatibility_header") {
                HealthSectionHeader(
                    title = stringResource(R.string.connection_health_compatibility),
                    description = stringResource(R.string.connection_health_compatibility_description),
                )
            }

            if (current.suppressedIntegrations.isEmpty()) {
                item(key = "compatibility_clear") {
                    HealthCard {
                        Text(
                            text = stringResource(R.string.connection_health_no_suppressed_integrations),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(
                    items = current.suppressedIntegrations,
                    key = { "compatibility_$it" },
                ) { integration ->
                    HealthCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(color = MaterialTheme.colorScheme.tertiary)
                            Text(
                                text = integration.toFriendlyLabel(),
                                modifier = Modifier.padding(start = 12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            item(key = "report") {
                HealthCard {
                    Text(
                        text = stringResource(R.string.connection_health_private_report),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.connection_health_private_report_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = { copyReport(context, current.redactedReport) },
                            enabled = current.redactedReport.isNotBlank(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.connection_health_copy))
                        }
                        FilledTonalButton(
                            onClick = { shareReport(context, current.redactedReport) },
                            enabled = current.redactedReport.isNotBlank(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.connection_health_share))
                        }
                    }
                }
            }

            item(key = "updated") {
                Text(
                    text = stringResource(R.string.connection_health_updated, current.generatedAtLabel),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun HealthSummary(snapshot: ConnectionDiagnosticsSnapshot?) {
    val soundConnected = snapshot?.bluetoothAudioConnected == true
    val controlsConnected = snapshot?.controlChannelConnected == true
    val accent = when {
        soundConnected && controlsConnected -> MaterialTheme.colorScheme.primary
        soundConnected -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = accent.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(accent.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                StatusDot(color = accent, size = 14)
            }
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = snapshot?.stateLabel
                        ?: stringResource(R.string.connection_health_waiting),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.connection_health_live_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HealthSection(
    title: String,
    rows: List<HealthRow>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HealthSectionHeader(title = title)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column {
                rows.forEachIndexed { index, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = row.label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (row.positive != null) {
                            StatusDot(
                                color = if (row.positive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                        Text(
                            text = row.value,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                        )
                    }
                    if (index < rows.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthSectionHeader(title: String, description: String? = null) {
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        if (description != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HealthCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content,
        )
    }
}

@Composable
private fun StatusDot(color: Color, size: Int = 9) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(color, CircleShape)
    )
}

private data class HealthRow(
    val label: String,
    val value: String,
    val positive: Boolean? = null,
)

private fun String.toFriendlyLabel(): String {
    if (none { it == '_' || it == '-' }) return this
    return lowercase()
        .replace('_', ' ')
        .replace('-', ' ')
        .replaceFirstChar { it.titlecase() }
}

private fun copyReport(context: Context, report: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(
        ClipData.newPlainText(
            context.getString(R.string.connection_health_private_report),
            report,
        )
    )
    Toast.makeText(
        context,
        R.string.connection_health_copied,
        Toast.LENGTH_SHORT,
    ).show()
}

private fun shareReport(context: Context, report: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.connection_health))
        putExtra(Intent.EXTRA_TEXT, report)
    }
    try {
        context.startActivity(
            Intent.createChooser(
                shareIntent,
                context.getString(R.string.connection_health_share_title),
            )
        )
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            context,
            R.string.connection_health_share_unavailable,
            Toast.LENGTH_SHORT,
        ).show()
    }
}
