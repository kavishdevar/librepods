package me.kavishdevar.librepods.presentation.navigation

import android.annotation.SuppressLint
import androidx.activity.BackEventCompat.Companion.EDGE_LEFT
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import me.kavishdevar.librepods.LibrePodsApplication
import me.kavishdevar.librepods.bluetooth.MacAddress
import me.kavishdevar.librepods.bluetooth.aacp.types.ControlCommandIdentifier
import me.kavishdevar.librepods.data.updates.updates
import me.kavishdevar.librepods.devices.AppleDevice
import me.kavishdevar.librepods.devices.ConnectionState
import me.kavishdevar.librepods.devices.Device
import me.kavishdevar.librepods.presentation.screens.AppSettingsScreen
import me.kavishdevar.librepods.presentation.screens.BLESettingsScreenRoute
import me.kavishdevar.librepods.presentation.screens.DeviceListRoute
import me.kavishdevar.librepods.presentation.screens.OpenSourceLicensesScreen
import me.kavishdevar.librepods.presentation.screens.PurchaseScreen
import me.kavishdevar.librepods.presentation.screens.ReleaseNotesScreen
import me.kavishdevar.librepods.presentation.screens.TroubleshootingScreen
import me.kavishdevar.librepods.presentation.screens.apple.AccessibilitySettingsScreen
import me.kavishdevar.librepods.presentation.screens.apple.AdaptiveStrengthScreen
import me.kavishdevar.librepods.presentation.screens.apple.AirPodsSettingsRoute
import me.kavishdevar.librepods.presentation.screens.apple.CallControlScreen
import me.kavishdevar.librepods.presentation.screens.apple.DebugRoute
import me.kavishdevar.librepods.presentation.screens.apple.EqualizerRoute
import me.kavishdevar.librepods.presentation.screens.apple.HeadTrackingScreen
import me.kavishdevar.librepods.presentation.screens.apple.HearingAidAdjustmentsScreen
import me.kavishdevar.librepods.presentation.screens.apple.HearingAidScreen
import me.kavishdevar.librepods.presentation.screens.apple.HearingProtectionScreen
import me.kavishdevar.librepods.presentation.screens.apple.LongPress
import me.kavishdevar.librepods.presentation.screens.apple.MicrophoneSettingsRoute
import me.kavishdevar.librepods.presentation.screens.apple.RecordingScreenRoute
import me.kavishdevar.librepods.presentation.screens.apple.RenameScreen
import me.kavishdevar.librepods.presentation.screens.apple.TransparencySettingsScreen
import me.kavishdevar.librepods.presentation.screens.apple.UpdateHearingTestRoute
import me.kavishdevar.librepods.presentation.screens.apple.VersionScreen
import me.kavishdevar.librepods.presentation.screens.onboarding.OnboardingScreen
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.viewmodel.AppSettingsViewModel
import me.kavishdevar.librepods.presentation.viewmodel.AppleViewModel
import me.kavishdevar.librepods.presentation.viewmodel.PurchaseViewModel
import me.kavishdevar.librepods.repository.RecordingRepository

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun AppNavGraph(
    showReleaseNotes: Boolean = false,
    updatesShown: () -> Unit = {},
    onboardingComplete: () -> Unit = {},
    backStack: SnapshotStateList<Screen>,
    devicesState: State<Map<MacAddress, Device<*, *, *>>>
) {
    val devices by devicesState

    val navigate: (Screen) -> Unit = { screen ->
        backStack.add(screen)
    }

    fun navigateToPurchase() {
        navigate(Screen.Purchase)
    }

    val context = LocalContext.current

    val appDataRepository by lazy { (context.applicationContext as LibrePodsApplication).appDataRepository }
    val recordingRepository = RecordingRepository(context)

    val currentDevice: Device<*, *, *>? =
        devices[backStack.lastOrNull()?.let { (it as? DeviceScreen)?.macAddress }]

    @SuppressLint("UnrememberedMutableState")
    val currentConnectionState by (currentDevice as? AppleDevice)?.connectionState?.collectAsState()
        ?: mutableStateOf(ConnectionState.DISCONNECTED)

    if (currentConnectionState == ConnectionState.DISCONNECTED) {
        // not sure how we will be able to navigate to another device from one device, but just in case we had two different devices in the backstack, we will remove DeviceScreens only of the disconnected device from the backstack
        while (backStack.isNotEmpty() && backStack.last() is DeviceScreen && (backStack.last() as DeviceScreen).macAddress == currentDevice?.macAddress) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    val m3eEnabled = LocalDesignSystem.current == DesignSystem.Material

    SharedTransitionLayout {
        NavDisplay(
            sharedTransitionScope = this,
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.lastIndex)
                }
            },
            entryProvider = { screen ->
                when (screen) {
                    Screen.Onboarding ->
                        NavEntry(screen) {
                            OnboardingScreen {
                                onboardingComplete()
                                if (showReleaseNotes) navigate(Screen.ReleaseNotes) else navigate(
                                    Screen.DeviceList
                                )
                                backStack.remove(screen)
                            }
                        }

                    Screen.DeviceList ->
                        NavEntry(screen) {
                            DeviceListRoute(
                                devices = devices,
                                navigateToDevice = { macAddress ->
                                    when (devices[macAddress]) {
                                        is AppleDevice -> navigate(Screen.AppleScreen(macAddress))
                                        else -> {}
                                    }
                                }
                            )
                        }

                    is Screen.AppleScreen ->
                        NavEntry(screen) {
                            val device = devices[screen.macAddress] as? AppleDevice ?: return@NavEntry

                            val factory = viewModelFactory {
                                initializer {
                                    AppleViewModel(
                                        device = device,
                                        recordingRepository = recordingRepository,
                                    )
                                }
                            }

                            val appleViewModel: AppleViewModel = viewModel(key = "${screen.macAddress.value}:${device.connectionNumber}", factory = factory)

                            AirPodsSettingsRoute(
                                viewModel = appleViewModel,
                                navigateToRename = { navigate(Screen.Rename(screen.macAddress)) },
                                navigateToHearingProtection = {
                                    navigate(
                                        Screen.HearingProtection(
                                            screen.macAddress
                                        )
                                    )
                                },
                                navigateToHearingAid = { navigate(Screen.HearingAid(screen.macAddress)) },
                                navigateToLeftLongPress = {
                                    navigate(
                                        Screen.LongPress(screen.macAddress, "Left")
                                    )
                                },
                                navigateToRightLongPress = {
                                    navigate(
                                        Screen.LongPress(screen.macAddress, "Right")
                                    )
                                },
                                navigateToPurchase = { navigate(Screen.Purchase) },
                                navigateToAdaptiveStrength = {
                                    navigate(
                                        Screen.AdaptiveStrength(
                                            screen.macAddress
                                        )
                                    )
                                },
                                navigateToEqualizer = { navigate(Screen.Equalizer(screen.macAddress)) },
                                navigateToHeadTracking = { navigate(Screen.HeadTracking(screen.macAddress)) },
                                navigateToAccessibility = { navigate(Screen.Accessibility(screen.macAddress)) },
                                navigateToVersion = { navigate(Screen.VersionInfo(screen.macAddress)) },
                                navigateToCallControlScreen = {
                                    navigate(
                                        Screen.CallControl(
                                            screen.macAddress,
                                            it
                                        )
                                    )
                                },
                                navigateToMicrophoneSettings = {
                                    navigate(
                                        Screen.MicrophoneSettings(
                                            screen.macAddress
                                        )
                                    )
                                },
                                navigateToRecordingScreen = { navigate(Screen.Recording(screen.macAddress)) },
                                navigateToDebugScreen = { navigate(Screen.Debug(screen.macAddress)) }
                            )
                        }

                    is Screen.Rename ->
                        NavEntry(screen) {
                            val device = devices[screen.macAddress] as? AppleDevice ?: return@NavEntry

                            val factory = viewModelFactory {
                                initializer {
                                    AppleViewModel(
                                        device = device,
                                        recordingRepository = recordingRepository,
                                    )
                                }
                            }

                            val appleViewModel: AppleViewModel = viewModel(key = "${screen.macAddress.value}:${device.connectionNumber}", factory = factory)

                            RenameScreen(appleViewModel)
                        }

                    Screen.AppSettings ->
                        NavEntry(screen) {
                            val factory = viewModelFactory {
                                initializer {
                                    AppSettingsViewModel(
                                        appDataRepository = appDataRepository,
                                    )
                                }
                            }
                            val appSettingsViewModel: AppSettingsViewModel = viewModel(factory = factory)

                            AppSettingsScreen(
                                viewModel = appSettingsViewModel,
                                navigateToPurchase = ::navigateToPurchase,
                                navigateToTroubleshooting = { navigate(Screen.Troubleshooting) },
                                navigateToOpenSourceLicenses = { navigate(Screen.OpenSourceLicenses) },
                                navigateToReleaseNotesScreen = { navigate(Screen.ReleaseNotes) },
                                navigateToBleSettingsScreen = { navigate(Screen.BLESettings) }
                            )
                        }

                    Screen.Troubleshooting ->
                        NavEntry(screen) {
                            TroubleshootingScreen()
                        }

                    is Screen.HeadTracking ->
                        NavEntry(screen) {
                            val device = devices[screen.macAddress] as? AppleDevice ?: return@NavEntry

                            val factory = viewModelFactory {
                                initializer {
                                    AppleViewModel(
                                        device = device,
                                        recordingRepository = recordingRepository,
                                    )
                                }
                            }

                            val appleViewModel: AppleViewModel = viewModel(key = "${screen.macAddress.value}:${device.connectionNumber}", factory = factory)

                            HeadTrackingScreen(appleViewModel, ::navigateToPurchase)
                        }

                    is Screen.Accessibility ->
                        NavEntry(screen) {
                            val device = devices[screen.macAddress] as? AppleDevice ?: return@NavEntry
                            val factory = viewModelFactory {
                                initializer {
                                    AppleViewModel(
                                        device = device,
                                        recordingRepository = recordingRepository,
                                    )
                                }
                            }

                            val appleViewModel: AppleViewModel = viewModel(key = "${screen.macAddress.value}:${device.connectionNumber}", factory = factory)

                            AccessibilitySettingsScreen(
                                viewModel = appleViewModel,
                                navigateToPurchase = ::navigateToPurchase,
                                navigateToTransparencyCustomization = {
                                    navigate(
                                        Screen.TransparencyCustomization(
                                            screen.macAddress
                                        )
                                    )
                                }
                            )
                        }

                    is Screen.TransparencyCustomization ->
                        NavEntry(screen) {
                            val device = devices[screen.macAddress] as? AppleDevice ?: return@NavEntry

                            val factory = viewModelFactory {
                                initializer {
                                    AppleViewModel(
                                        device = device,
                                        recordingRepository = recordingRepository,
                                    )
                                }
                            }

                            val appleViewModel: AppleViewModel = viewModel(key = "${screen.macAddress.value}:${device.connectionNumber}", factory = factory)

                            TransparencySettingsScreen(appleViewModel)
                        }

                    is Screen.HearingAid ->
                        NavEntry(screen) {
                            val device = devices[screen.macAddress] as? AppleDevice ?: return@NavEntry

                            val factory = viewModelFactory {
                                initializer {
                                    AppleViewModel(
                                        device = device,
                                        recordingRepository = recordingRepository,
                                    )
                                }
                            }

                            val appleViewModel: AppleViewModel = viewModel(key = "${screen.macAddress.value}:${device.connectionNumber}", factory = factory)

                            HearingAidScreen(
                                viewModel = appleViewModel,
                                onNavigateHearingAidAdjustments = {
                                    navigate(
                                        Screen.HearingAidAdjustments(
                                            screen.macAddress
                                        )
                                    )
                                },
                                onNavigateHearingTest = { navigate(Screen.UpdateHearingTest(screen.macAddress)) },
                            )
                        }

                    is Screen.HearingAidAdjustments ->
                        NavEntry(screen) {
                            val device = devices[screen.macAddress] as? AppleDevice ?: return@NavEntry

                            val factory = viewModelFactory {
                                initializer {
                                    AppleViewModel(
                                        device = device,
                                        recordingRepository = recordingRepository,
                                    )
                                }
                            }

                            val appleViewModel: AppleViewModel = viewModel(key = "${screen.macAddress.value}:${device.connectionNumber}", factory = factory)

                            HearingAidAdjustmentsScreen(appleViewModel)
                        }

                    is Screen.AdaptiveStrength ->
                        NavEntry(screen) {
                            val device = devices[screen.macAddress] as? AppleDevice ?: return@NavEntry

                            val factory = viewModelFactory {
                                initializer {
                                    AppleViewModel(
                                        device = device,
                                        recordingRepository = recordingRepository,
                                    )
                                }
                            }

                            val appleViewModel: AppleViewModel = viewModel(key = "${screen.macAddress.value}:${device.connectionNumber}", factory = factory)

                            AdaptiveStrengthScreen(appleViewModel, ::navigateToPurchase)
                        }

//                Screen.CameraControl ->
//                    NavEntry(screen) {
//                        CameraControlScreen(AppleViewModel)
//                    }

                    Screen.OpenSourceLicenses ->
                        NavEntry(screen) {
                            OpenSourceLicensesScreen()
                        }

                    is Screen.UpdateHearingTest ->
                        NavEntry(screen) {
                            val device = devices[screen.macAddress] as? AppleDevice ?: return@NavEntry

                            val factory = viewModelFactory {
                                initializer {
                                    AppleViewModel(
                                        device = device,
                                        recordingRepository = recordingRepository,
                                    )
                                }
                            }

                            val appleViewModel: AppleViewModel = viewModel(key = "${screen.macAddress.value}:${device.connectionNumber}", factory = factory)

                            UpdateHearingTestRoute(appleViewModel)
                        }

                    is Screen.VersionInfo ->
                        NavEntry(screen) {
                            val device = devices[screen.macAddress] as? AppleDevice ?: return@NavEntry

                            val factory = viewModelFactory {
                                initializer {
                                    AppleViewModel(
                                        device = device,
                                        recordingRepository = recordingRepository,
                                    )
                                }
                            }

                            val appleViewModel: AppleViewModel = viewModel(key = "${screen.macAddress.value}:${device.connectionNumber}", factory = factory)

                            VersionScreen(appleViewModel)
                        }

                    is Screen.HearingProtection ->
                        NavEntry(screen) {
                            val device = devices[screen.macAddress] as? AppleDevice ?: return@NavEntry

                            val factory = viewModelFactory {
                                initializer {
                                    AppleViewModel(
                                        device = device,
                                        recordingRepository = recordingRepository,
                                    )
                                }
                            }

                            val appleViewModel: AppleViewModel = viewModel(key = "${screen.macAddress.value}:${device.connectionNumber}", factory = factory)

                            HearingProtectionScreen(
                                viewModel = appleViewModel,
                                navigateToPurchase = ::navigateToPurchase
                            )
                        }

                    is Screen.Purchase ->
                        NavEntry(screen) {
                            val vm: PurchaseViewModel = viewModel()
                            PurchaseScreen(vm, backStack)
                        }

                    is Screen.Equalizer ->
                        NavEntry(screen) {
                            val device = devices[screen.macAddress] as? AppleDevice ?: return@NavEntry

                            val factory = viewModelFactory {
                                initializer {
                                    AppleViewModel(
                                        device = device,
                                        recordingRepository = recordingRepository,
                                    )
                                }
                            }

                            val appleViewModel: AppleViewModel = viewModel(key = "${screen.macAddress.value}:${device.connectionNumber}", factory = factory)

                            EqualizerRoute(appleViewModel)
                        }

                    is Screen.LongPress ->
                        NavEntry(screen) {
                            val device = devices[screen.macAddress] as? AppleDevice ?: return@NavEntry

                            val factory = viewModelFactory {
                                initializer {
                                    AppleViewModel(
                                        device = device,
                                        recordingRepository = recordingRepository,
                                    )
                                }
                            }

                            val appleViewModel: AppleViewModel = viewModel(key = "${screen.macAddress.value}:${device.connectionNumber}", factory = factory)

                            LongPress(
                                viewModel = appleViewModel,
                                name = screen.bud,
                                navigateToPurchase = ::navigateToPurchase
                            )
                        }

                    is Screen.CallControl ->
                        NavEntry(screen) {
                            val device = devices[screen.macAddress] as? AppleDevice ?: return@NavEntry

                            val factory = viewModelFactory {
                                initializer {
                                    AppleViewModel(
                                        device = device,
                                        recordingRepository = recordingRepository,
                                    )
                                }
                            }

                            val appleViewModel: AppleViewModel = viewModel(key = "${screen.macAddress.value}:${device.connectionNumber}", factory = factory)

                            CallControlScreen(
                                viewModel = appleViewModel,
                                action = screen.action,
                                onCallControlValueChanged = { flipped ->
                                    device.setControlCommand(
                                        ControlCommandIdentifier.CALL_MANAGEMENT_CONFIG,
                                        if (flipped) byteArrayOf(0x00, 0x02) else byteArrayOf(
                                            0x00,
                                            0x03
                                        )
                                    )
                                }
                            )
                        }

                    is Screen.MicrophoneSettings ->
                        NavEntry(screen) {
                            val device = devices[screen.macAddress] as? AppleDevice ?: return@NavEntry

                            val factory = viewModelFactory {
                                initializer {
                                    AppleViewModel(
                                        device = device,
                                        recordingRepository = recordingRepository,
                                    )
                                }
                            }

                            val appleViewModel: AppleViewModel = viewModel(key = "${screen.macAddress.value}:${device.connectionNumber}", factory = factory)

                            MicrophoneSettingsRoute(viewModel = appleViewModel)
                        }

                    is Screen.ReleaseNotes ->
                        NavEntry(screen) {
                            ReleaseNotesScreen(
                                updates = updates,
                                releaseNotesShown = {
                                    if (showReleaseNotes) {
                                        navigate(Screen.DeviceList)
                                        backStack.remove(screen)
                                        updatesShown()
                                    } else {
                                        backStack.removeAt(backStack.lastIndex)
                                    }
                                }
                            )
                        }

                    is Screen.Recording ->
                        NavEntry(screen) {
                            val device = devices[screen.macAddress] as? AppleDevice ?: return@NavEntry

                            val factory = viewModelFactory {
                                initializer {
                                    AppleViewModel(
                                        device = device,
                                        recordingRepository = recordingRepository,
                                    )
                                }
                            }

                            val appleViewModel: AppleViewModel = viewModel(key = "${screen.macAddress.value}:${device.connectionNumber}", factory = factory)

                            RecordingScreenRoute(appleViewModel)
                        }

                    is Screen.Debug ->
                        NavEntry(screen) {
                            val device = devices[screen.macAddress] as? AppleDevice ?: return@NavEntry

                            val factory = viewModelFactory {
                                initializer {
                                    AppleViewModel(
                                        device = device,
                                        recordingRepository = recordingRepository,
                                    )
                                }
                            }

                            val appleViewModel: AppleViewModel = viewModel(key = "${screen.macAddress.value}:${device.connectionNumber}", factory = factory)

                            DebugRoute(appleViewModel)
                        }

                    is Screen.BLESettings ->
                        NavEntry(screen) {
                            val factory = viewModelFactory {
                                initializer {
                                    AppSettingsViewModel(
                                        appDataRepository = appDataRepository,
                                    )
                                }
                            }
                            val appSettingsViewModel: AppSettingsViewModel = viewModel(factory = factory)

                            BLESettingsScreenRoute(
                                viewModel = appSettingsViewModel
                            )
                        }
                }
            },
            transitionSpec = {
                slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 4 }
            },
            popTransitionSpec = {
                slideInHorizontally { -it / 4 } togetherWith slideOutHorizontally { it }
            },
            predictivePopTransitionSpec = { swipeEdge ->
                if (m3eEnabled) {
                    val enterOffset: (Int) -> Int =
                        if (swipeEdge == EDGE_LEFT) {
                            { -it / 6 }
                        } else {
                            { it / 6 }
                        }

                    val exitOffset: (Int) -> Int =
                        if (swipeEdge == EDGE_LEFT) {
                            { it / 8 }
                        } else {
                            { -it / 8 }
                        }

                    fadeIn(
                        animationSpec = tween(250)
                    ) +
                        slideInHorizontally(
                            initialOffsetX = enterOffset,
                            animationSpec = tween(250)
                        ) togetherWith
                        fadeOut(
                            targetAlpha = 0.75f,
                            animationSpec = tween(250)
                        ) +
                        scaleOut(
                            targetScale = 0.85f,
                            animationSpec = tween(250)
                        ) +
                        slideOutHorizontally(
                            targetOffsetX = exitOffset,
                            animationSpec = tween(250)
                        )
                } else {
                    slideInHorizontally { -it / 4 } togetherWith slideOutHorizontally { it }
                }
            },
        )
    }
}
