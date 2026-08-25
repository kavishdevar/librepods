package me.kavishdevar.librepods.presentation.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.backdrops.LayerBackdrop
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.MaterialIcons
import me.kavishdevar.librepods.presentation.components.StyledIconButton
import me.kavishdevar.librepods.presentation.components.StyledScaffold
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsUiState
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel

@Immutable
private data class NavigationChromeState(
    val isLocallyConnected: Boolean,
    val deviceName: String,
    val headTrackingActive: Boolean,
)

@Composable
fun NavigationRoot(
    showReleaseNotes: Boolean = false,
    updatesShown: () -> Unit = {},
    showOnboarding: Boolean = false,
    onboardingComplete: () -> Unit = {},
    airPodsViewModel: AirPodsViewModel
) {
    val backStack = remember {
        mutableStateListOf(
            when {
                showOnboarding -> Screen.Onboarding
                showReleaseNotes -> Screen.ReleaseNotes
                else -> Screen.AirPodsSettings
            }
        )
    }

    val currentScreen = backStack.last()

    val initialChromeState = remember(airPodsViewModel) {
        airPodsViewModel.uiState.value.toNavigationChromeState()
    }
    val chromeStateFlow = remember(airPodsViewModel) {
        airPodsViewModel.uiState
            .map { it.toNavigationChromeState() }
            .distinctUntilChanged()
    }
    val chromeState by chromeStateFlow.collectAsStateWithLifecycle(
        initialValue = initialChromeState
    )

    val m3eEnabled = LocalDesignSystem.current == DesignSystem.Material

    val title = when (currentScreen) {
        Screen.Onboarding -> ""
        Screen.AirPodsSettings -> if (chromeState.isLocallyConnected) chromeState.deviceName else stringResource(R.string.app_name)
        Screen.Accessibility -> stringResource(R.string.accessibility)
        Screen.AdaptiveStrength -> stringResource(R.string.customize_adaptive_audio)
        Screen.AppSettings -> stringResource(R.string.settings)
        Screen.ConnectionHealth -> stringResource(R.string.connection_health)
//        Screen.CameraControl -> stringResource(R.string.camera_control)
        Screen.Equalizer -> stringResource(R.string.equalizer)
        Screen.HeadTracking -> stringResource(R.string.head_tracking)
        Screen.HearingAid -> stringResource(R.string.hearing_aid)
        Screen.HearingAidAdjustments -> stringResource(R.string.adjustments)
        Screen.HearingProtection -> stringResource(R.string.hearing_protection)
        is Screen.LongPress -> currentScreen.bud
        Screen.OpenSourceLicenses -> stringResource(R.string.open_source_licenses)
        Screen.Purchase -> stringResource(R.string.unlock_advanced_features)
        Screen.Rename -> stringResource(R.string.name)
        Screen.TransparencyCustomization -> stringResource(R.string.customize_transparency_mode)
        Screen.Troubleshooting -> stringResource(R.string.troubleshooting)
        Screen.UpdateHearingTest -> stringResource(R.string.update_hearing_test)
        Screen.VersionInfo -> stringResource(R.string.version)
        is Screen.CallControl -> currentScreen.action
        Screen.MicrophoneSettings -> stringResource(R.string.microphone_mode)
        Screen.ReleaseNotes -> ""
    }

    val actionButtons = when (currentScreen) {
        Screen.AirPodsSettings -> listOf<@Composable (backdrop: LayerBackdrop) -> Unit>(
                { scaffoldBackdrop ->
                    if (m3eEnabled) {
                        FilledTonalIconButton(
                            onClick = { backStack.add(Screen.AppSettings) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(48.dp),

                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.settings),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        StyledIconButton(
                            onClick = { backStack.add(Screen.AppSettings) },
                            icon = "􀍟",
                            backdrop = scaffoldBackdrop
                        )
                    }
                }
            )
        Screen.HeadTracking -> listOf<@Composable (backdrop: LayerBackdrop) -> Unit>(
            { scaffoldBackdrop ->
                if (m3eEnabled) {
                    FilledTonalIconToggleButton(
                        checked = chromeState.headTrackingActive,
                        onCheckedChange = { if (it) airPodsViewModel.startHeadTracking() else airPodsViewModel.stopHeadTracking() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(48.dp),
                        shape = IconButtonDefaults.mediumRoundShape
                    ) {
                        Icon(
                            imageVector = if (chromeState.headTrackingActive) MaterialIcons.pause else Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.head_tracking),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    StyledIconButton(
                        onClick = {
                            if (!chromeState.headTrackingActive) {
                                airPodsViewModel.startHeadTracking()
                            } else {
                                airPodsViewModel.stopHeadTracking()
                            }
                        },
                        icon = if (chromeState.headTrackingActive) "􀊅" else "􀊃",
                        backdrop = scaffoldBackdrop
                    )
                }
            }
        )
        else -> listOf()
    }

    StyledScaffold(
        visible = currentScreen.showTopBar,
        title = title,
        showBackButton = backStack.size > 1,
        onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
        actionButtons = actionButtons
    ) {
        AppNavGraph(
            showReleaseNotes = showReleaseNotes,
            updatesShown = updatesShown,
            showOnboarding = showOnboarding,
            onboardingComplete = onboardingComplete,
            backStack = backStack,
            airPodsViewModel = airPodsViewModel,
        )
    }
}

private fun AirPodsUiState.toNavigationChromeState() =
    NavigationChromeState(
        isLocallyConnected = isLocallyConnected,
        deviceName = deviceName,
        headTrackingActive = headTrackingActive,
    )
