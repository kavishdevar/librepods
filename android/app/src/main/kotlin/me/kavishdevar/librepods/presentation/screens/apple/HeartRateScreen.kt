package me.kavishdevar.librepods.presentation.screens.apple

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.flow.debounce
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.devices.AppleSettings
import me.kavishdevar.librepods.presentation.components.primitives.StyledIconButton
import me.kavishdevar.librepods.presentation.components.primitives.StyledListItem
import me.kavishdevar.librepods.presentation.components.primitives.StyledListItemOrientation
import me.kavishdevar.librepods.presentation.components.primitives.StyledScaffold
import me.kavishdevar.librepods.presentation.components.primitives.StyledSlider
import me.kavishdevar.librepods.presentation.components.primitives.StyledToggle
import me.kavishdevar.librepods.presentation.icons.LocalIcons
import me.kavishdevar.librepods.presentation.icons.MaterialIcons
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.viewmodel.AppleUiState
import me.kavishdevar.librepods.presentation.viewmodel.AppleViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun HeartRateRoute(
    viewModel: AppleViewModel,
    navigateBack: (() -> Unit)?
) {
    val uiState by viewModel.uiState.collectAsState()

    HeartRateScreen(
        uiState = uiState,
        navigateBack = navigateBack,
        startHr = viewModel::startHr,
        stopHr = viewModel::stopHr,
        setHrRange = viewModel::setHrRange,
        updateSettings = viewModel::updateSettings
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HeartRateScreen(
    uiState: AppleUiState,
    navigateBack: (() -> Unit)?,
    startHr: () -> Unit,
    stopHr: () -> Unit,
    setHrRange: (ClosedRange<Long>) -> Unit,
    updateSettings: (transform: (AppleSettings) -> AppleSettings) -> Unit,
) {
    val state = uiState.state
    val settings = uiState.settings

    val scrollState = rememberScrollState()

    StyledScaffold(
        title = stringResource(R.string.heart_rate),
        navigateBack = navigateBack,
        actionButtons = listOf(
            { scaffoldBackdrop ->
                if (LocalDesignSystem.current == DesignSystem.Material) {
                    FilledTonalIconToggleButton(
                        checked = state.hrmActive,
                        onCheckedChange = { if (it) startHr() else stopHr() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Uniform)),
                        shape = IconButtonDefaults.mediumRoundShape
                    ) {
                        Icon(
                            imageVector = if (state.hrmActive) MaterialIcons.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Start/Stop",
                            modifier = Modifier.size(IconButtonDefaults.mediumIconSize),
                        )
                    }
                } else {
                    StyledIconButton(
                        onClick = { if (!state.hrmActive) startHr() else stopHr() },
                        backdrop = scaffoldBackdrop
                    ) {
                        Icon(
                            imageVector = if (state.hrmActive) LocalIcons.current.Pause else LocalIcons.current.Play,
                            contentDescription = "Start/Stop",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        )
    ) { topPadding, bottomPadding ->
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(topPadding))

            val healthPermissions = rememberPermissionState(HealthPermission.getWritePermission(HeartRateRecord::class))

            AnimatedVisibility(state.currentHeartRate != null) {
                if (state.currentHeartRate == null) return@AnimatedVisibility
                StyledListItem(
                    onClick = null,
                    content = {
                        Text(
                            text = "${state.currentHeartRate.bpm} bpm",
                            style = MaterialTheme.typography.bodyLargeEmphasized,
                            modifier = Modifier.fillMaxHeight()
                        )
                    },
                    supportingContent = {
                        val locale = LocalLocale.current.platformLocale
                        val timePattern = DateFormat.getBestDateTimePattern(
                            locale,
                            "jms"
                        )
                        val formatter = DateTimeFormatter.ofPattern(timePattern)

                        val timeString = formatter.format(
                            Instant.ofEpochMilli(state.currentHeartRate.timestamp.toEpochMilliseconds())
                                .atZone(ZoneId.systemDefault())
                        )

                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialShapes.SoftBurst.normalized()
                                        .toShape()
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = LocalIcons.current.VitalSigns,
                                contentDescription = "vital signs",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    },
                    orientation = StyledListItemOrientation.Vertical
                )
            }

            AnimatedVisibility(visible = !healthPermissions.status.isGranted) {
                StyledListItem(
                    contentText = stringResource(R.string.permission_healthconnect),
                    onClick = { healthPermissions.launchPermissionRequest() },
                    supportingText = stringResource(R.string.permission_description_healthconnect),
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                    MaterialShapes.SoftBurst.normalized()
                                        .toShape()
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = LocalIcons.current.VitalSigns,
                                contentDescription = "vital signs",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    orientation = StyledListItemOrientation.Vertical
                )
            }

            StyledToggle(
                label = stringResource(R.string.heart_rate_alert),
                description = stringResource(R.string.hrm_alert_description),
                checked = settings.hrAlertEnabled,
                onCheckedChange = { enabled ->
                    updateSettings {
                        it.copy(hrAlertEnabled = enabled)
                    }
                }
            )

            val sliderValue = remember { mutableFloatStateOf(settings.hrmAlertThreshold.toFloat()) }

            LaunchedEffect(sliderValue) {
                snapshotFlow { sliderValue.floatValue }
                    .debounce(250.milliseconds)
                    .collect { value ->
                        updateSettings {
                            it.copy(hrmAlertThreshold = value.toInt())
                        }
                    }
            }

            StyledSlider(
                label = stringResource(R.string.heart_rate_alert_threshold),
                value = sliderValue.floatValue,
                onValueChange = { sliderValue.floatValue = it },
                valueRange = 120f..180f,
                description = "${sliderValue.floatValue.roundToInt()} bpm",
                independent = true
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 350.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        RoundedCornerShape(28.dp)
                    )
            ) {
                // TODO: graph or something
            }

            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
}
