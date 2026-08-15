package me.kavishdevar.librepods.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.debounce
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.StyledList
import me.kavishdevar.librepods.presentation.components.StyledListItem
import me.kavishdevar.librepods.presentation.components.StyledListItemOrientation
import me.kavishdevar.librepods.presentation.components.StyledScaffold
import me.kavishdevar.librepods.presentation.components.StyledSlider
import me.kavishdevar.librepods.presentation.viewmodel.AppSettingsViewModel
import kotlin.time.Duration.Companion.seconds

@Composable
fun BLESettingsScreenRoute(
    viewModel: AppSettingsViewModel,
    navigateBack: (() -> Unit)?
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings

    BLESettingsScreen(
        navigateBack = navigateBack,
        scanMode = settings.bleScanMode,
        onScanModeChanged = { scanMode ->
            viewModel.updateSettings {
                it.copy(bleScanMode = scanMode)
            }
        },
        reportDelay = settings.bleReportDelay,
        onReportDelayChanged = { reportDelay ->
            viewModel.updateSettings {
                it.copy(bleReportDelay = reportDelay)
            }
        },
    )
}

@Composable
fun BLESettingsScreen(
    navigateBack: (() -> Unit)? = null,
    scanMode: Int,
    onScanModeChanged: (Int) -> Unit,
    reportDelay: Long,
    onReportDelayChanged: (Long) -> Unit,
) {
    val scrollState = rememberScrollState()

    StyledScaffold(
        title = stringResource(R.string.ble_settings),
        navigateBack = navigateBack
    ) { topPadding, bottomPadding ->
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.padding(top = topPadding))

            Text(
                text = stringResource(R.string.do_not_change),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )

            StyledList(title = stringResource(R.string.scanMode)) {
                StyledListItem(
                    onClick = { onScanModeChanged(0) },
                    contentText = stringResource(R.string.low_power),
                    supportingText = stringResource(R.string.ble_scan_mode_low_power_description),
                    orientation = StyledListItemOrientation.Vertical,
                    selected = scanMode == 0
                )
                StyledListItem(
                    onClick = { onScanModeChanged(1) },
                    contentText = stringResource(R.string.balanced),
                    supportingText = stringResource(R.string.ble_scan_mode_balanced_description),
                    orientation = StyledListItemOrientation.Vertical,
                    selected = scanMode == 1
                )
                StyledListItem(
                    onClick = { onScanModeChanged(2) },
                    contentText = stringResource(R.string.low_latency),
                    supportingText = stringResource(R.string.ble_scan_mode_low_latency_description),
                    orientation = StyledListItemOrientation.Vertical,
                    selected = scanMode == 2
                )
            }

            val sliderValue = remember { mutableFloatStateOf(reportDelay.toFloat()) }

            LaunchedEffect(sliderValue) {
                snapshotFlow { sliderValue.floatValue }
                    .debounce(1.seconds)
                    .collect { newValue ->
                        onReportDelayChanged(newValue.toLong())
                    }
            }

            StyledSlider(
                label = stringResource(R.string.ble_report_delay),
                value = sliderValue.floatValue,
                onValueChange = { sliderValue.floatValue = it },
                valueRange = 0f..1000f,
                description = sliderValue.floatValue.toString() + "ms",
                independent = true // i thought I got rid of all this lol
            )

            Spacer(modifier = Modifier.padding(bottom = bottomPadding))
        }
    }
}
