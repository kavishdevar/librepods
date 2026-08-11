package me.kavishdevar.librepods.presentation.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.MacAddress
import me.kavishdevar.librepods.devices.AppleDevice
import me.kavishdevar.librepods.devices.Device
import me.kavishdevar.librepods.presentation.components.StyledIconButton
import me.kavishdevar.librepods.presentation.components.StyledScaffold
import me.kavishdevar.librepods.presentation.icons.LocalIcons
import me.kavishdevar.librepods.presentation.icons.MaterialIcons
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem

@Composable
fun NavigationRoot(
    showReleaseNotes: Boolean = false,
    updatesShown: () -> Unit = {},
    showOnboarding: Boolean = false,
    onboardingComplete: () -> Unit = {},
    devicesState: State<Map<MacAddress, Device<*, *, *>>>
) {
    val devices by devicesState

    val backStack = remember {
        mutableStateListOf(
            when {
                showOnboarding -> Screen.Onboarding
                showReleaseNotes -> Screen.ReleaseNotes
                else -> Screen.DeviceList
            }
        )
    }

    val currentScreen = backStack.last()

    val m3eEnabled = LocalDesignSystem.current == DesignSystem.Material

    val title = when (currentScreen) {
        Screen.Onboarding -> ""
        Screen.DeviceList -> stringResource(R.string.app_name)
        is Screen.AppleScreen -> devices[currentScreen.macAddress]?.metadata?.collectAsState()?.value?.name ?: currentScreen.macAddress.value
        is Screen.Accessibility -> stringResource(R.string.accessibility)
        is Screen.AdaptiveStrength -> stringResource(R.string.customize_adaptive_audio)
        Screen.AppSettings -> stringResource(R.string.settings)
//        Screen.CameraControl -> stringResource(R.string.camera_control)
        is Screen.Equalizer -> stringResource(R.string.equalizer)
        is Screen.HeadTracking -> stringResource(R.string.head_tracking)
        is Screen.HearingAid -> stringResource(R.string.hearing_aid)
        is Screen.HearingAidAdjustments -> stringResource(R.string.adjustments)
        is Screen.HearingProtection -> stringResource(R.string.hearing_protection)
        is Screen.LongPress -> currentScreen.bud
        Screen.OpenSourceLicenses -> stringResource(R.string.open_source_licenses)
        Screen.Purchase -> stringResource(R.string.unlock_advanced_features)
        is Screen.Rename -> stringResource(R.string.name)
        is Screen.TransparencyCustomization -> stringResource(R.string.customize_transparency_mode)
        Screen.Troubleshooting -> stringResource(R.string.troubleshooting)
        is Screen.UpdateHearingTest -> stringResource(R.string.update_hearing_test)
        is Screen.VersionInfo -> stringResource(R.string.version)
        is Screen.CallControl -> currentScreen.action
        is Screen.MicrophoneSettings -> stringResource(R.string.microphone_mode)
        Screen.ReleaseNotes -> ""
        is Screen.Recording -> stringResource(R.string.recorder)
        is Screen.Debug -> "debug"
        is Screen.BLESettings -> stringResource(R.string.ble_settings)
    }

    // is this a bad idea? probably. I can't think of a better way without having to pass around a shouldShowBackButton to each screen to pass to each scaffold
    val actionButtons = when (currentScreen) {
        is Screen.AppleScreen, is Screen.DeviceList -> listOf<@Composable (backdrop: LayerBackdrop) -> Unit>(
                { scaffoldBackdrop ->
                    if (m3eEnabled) {
                        FilledTonalIconButton(
                            onClick = { backStack.add(Screen.AppSettings) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Uniform)),

                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "settings",
                                modifier = Modifier.size(IconButtonDefaults.mediumIconSize),
                            )
                        }
                    } else {
                        StyledIconButton(
                            onClick = { backStack.add(Screen.AppSettings) },
                            backdrop = scaffoldBackdrop
                        ) {
                            Icon(
                                imageVector = LocalIcons.current.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            )
        is Screen.HeadTracking -> listOf<@Composable (backdrop: LayerBackdrop) -> Unit>(
            { scaffoldBackdrop ->
                val device = devices[currentScreen.macAddress] as? AppleDevice? ?: return@listOf

                val state by device.state.collectAsState()

                if (m3eEnabled) {
                    FilledTonalIconToggleButton(
                        checked = state.headTrackingActive,
                        onCheckedChange = { if (it) device.startHeadTracking() else device.stopHeadTracking() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Uniform)),
                        shape = IconButtonDefaults.mediumRoundShape
                    ) {
                        Icon(
                            imageVector = if (state.headTrackingActive) MaterialIcons.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(IconButtonDefaults.mediumIconSize),
                        )
                    }
                } else {
                    StyledIconButton(
                        onClick = if (!state.headTrackingActive) device::startHeadTracking else device::stopHeadTracking,
                        backdrop = scaffoldBackdrop
                    ) {
                        Icon(
                            imageVector = if (state.headTrackingActive) LocalIcons.current.Pause else LocalIcons.current.Play,
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
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
            onboardingComplete = onboardingComplete,
            backStack = backStack,
            devicesState = devicesState,
        )
    }
}
