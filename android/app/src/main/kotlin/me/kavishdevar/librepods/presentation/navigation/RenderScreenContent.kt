package me.kavishdevar.librepods.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavBackStack
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.MacAddress
import me.kavishdevar.librepods.bluetooth.aacp.types.ControlCommandIdentifier
import me.kavishdevar.librepods.data.updates.updates
import me.kavishdevar.librepods.devices.AppleDevice
import me.kavishdevar.librepods.devices.Device
import me.kavishdevar.librepods.presentation.screens.AppSettingsScreen
import me.kavishdevar.librepods.presentation.screens.BLESettingsScreenRoute
import me.kavishdevar.librepods.presentation.screens.DeviceListScreen
import me.kavishdevar.librepods.presentation.screens.OpenSourceLicensesScreen
import me.kavishdevar.librepods.presentation.screens.PurchaseScreen
import me.kavishdevar.librepods.presentation.screens.ReleaseNotesScreen
import me.kavishdevar.librepods.presentation.screens.apple.AccessibilitySettingsScreen
import me.kavishdevar.librepods.presentation.screens.apple.AppleSettingsRoute
import me.kavishdevar.librepods.presentation.screens.apple.CallControlScreen
import me.kavishdevar.librepods.presentation.screens.apple.DebugRoute
import me.kavishdevar.librepods.presentation.screens.apple.EqualizerRoute
import me.kavishdevar.librepods.presentation.screens.apple.HeadTrackingScreen
import me.kavishdevar.librepods.presentation.screens.apple.HearingAidAdjustmentsScreen
import me.kavishdevar.librepods.presentation.screens.apple.HearingAidScreen
import me.kavishdevar.librepods.presentation.screens.apple.HearingProtectionScreen
import me.kavishdevar.librepods.presentation.screens.apple.HeartRateRoute
import me.kavishdevar.librepods.presentation.screens.apple.LongPress
import me.kavishdevar.librepods.presentation.screens.apple.MicrophoneSettingsRoute
import me.kavishdevar.librepods.presentation.screens.apple.RecordingScreenRoute
import me.kavishdevar.librepods.presentation.screens.apple.RenameScreen
import me.kavishdevar.librepods.presentation.screens.apple.TransparencySettingsScreen
import me.kavishdevar.librepods.presentation.screens.apple.UpdateHearingTestRoute
import me.kavishdevar.librepods.presentation.screens.apple.VersionScreen
import me.kavishdevar.librepods.presentation.screens.onboarding.OnboardingScreen
import me.kavishdevar.librepods.presentation.viewmodel.AppSettingsViewModel
import me.kavishdevar.librepods.presentation.viewmodel.AppleViewModel
import me.kavishdevar.librepods.presentation.viewmodel.PurchaseViewModel
import me.kavishdevar.librepods.repository.AppDataRepository
import me.kavishdevar.librepods.repository.HeartRateRepository
import me.kavishdevar.librepods.repository.RecordingRepository


private fun createAppleViewModelFactory(
    device: AppleDevice,
    appDataRepository: AppDataRepository,
    recordingRepository: RecordingRepository,
    heartRateRepository: HeartRateRepository
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        AppleViewModel(
            device = device,
            appDataRepository = appDataRepository,
            recordingRepository = recordingRepository,
            heartRateRepository = heartRateRepository
        )
    }
}

@Composable
fun RenderScreenContent(
    screen: Screen,
    backStack: NavBackStack<Screen>,
    backRequests: Channel<CompletableDeferred<Unit>>,
    devices: Map<MacAddress, Device<*, *, *>>,
    appDataRepository: AppDataRepository,
    recordingRepository: RecordingRepository,
    heartRateRepository: HeartRateRepository,
    showReleaseNotes: Boolean,
    updatesShown: () -> Unit,
    onboardingComplete: () -> Unit
) {
    val navigate: (Screen) -> Unit = { target -> if (target !in backStack) backStack.add(target) } // prevents multiple clicks while transitioning
    fun navigateToPurchase() = navigate(Screen.Purchase)

    val navigateBack: (() -> Unit)? = if (backStack.size > 1) {
        {
            val completed = CompletableDeferred<Unit>()
            backRequests.trySend(completed)
        }
    } else null

    when (screen) {
        Screen.Onboarding -> {
            OnboardingScreen {
                onboardingComplete()
                if (showReleaseNotes) navigate(Screen.ReleaseNotes) else navigate(Screen.DeviceList)
                backStack.remove(screen)
            }
        }

        Screen.DeviceList -> {
            DeviceListScreen(
                devices = devices,
                navigateToAppSettings = { navigate(Screen.AppSettings) },
                navigateToDevice = { macAddress ->
                    when (devices[macAddress]) {
                        is AppleDevice -> navigate(Screen.AppleScreen(macAddress))
                        else -> {}
                    }
                }
            )
        }

        is Screen.AppleScreen -> {
            val device = devices[screen.macAddress] as? AppleDevice ?: return
            val factory = createAppleViewModelFactory(device, appDataRepository, recordingRepository, heartRateRepository)
            val appleViewModel: AppleViewModel = viewModel(
                key = "${screen.macAddress.value}:${device.connectionNumber}",
                factory = factory
            )

            val left = stringResource(R.string.left)
            val right = stringResource(R.string.right)

            AppleSettingsRoute(
                viewModel = appleViewModel,
                navigateBack = navigateBack,
                navigateToRename = { navigate(Screen.Rename(screen.macAddress)) },
                navigateToHearingProtection = { navigate(Screen.HearingProtection(screen.macAddress)) },
                navigateToHearingAid = { navigate(Screen.HearingAid(screen.macAddress)) },
                navigateToLeftLongPress = { navigate(Screen.LongPress(screen.macAddress, left)) },
                navigateToRightLongPress = { navigate(Screen.LongPress(screen.macAddress, right)) },
                navigateToPurchase = ::navigateToPurchase,
                navigateToEqualizer = { navigate(Screen.Equalizer(screen.macAddress)) },
                navigateToHeadTracking = { navigate(Screen.HeadTracking(screen.macAddress)) },
                navigateToAccessibility = { navigate(Screen.Accessibility(screen.macAddress)) },
                navigateToVersion = { navigate(Screen.VersionInfo(screen.macAddress)) },
                navigateToCallControlScreen = { navigate(Screen.CallControl(screen.macAddress, it)) },
                navigateToMicrophoneSettings = { navigate(Screen.MicrophoneSettings(screen.macAddress)) },
                navigateToRecordingScreen = { navigate(Screen.Recording(screen.macAddress)) },
                navigateToHeartRateScreen = { navigate(Screen.HeartRate(screen.macAddress)) },
                navigateToDebugScreen = { navigate(Screen.Debug(screen.macAddress)) }
            )
        }

        is Screen.Rename -> {
            val device = devices[screen.macAddress] as? AppleDevice ?: return
            val factory = createAppleViewModelFactory(device, appDataRepository, recordingRepository, heartRateRepository)
            val appleViewModel: AppleViewModel = viewModel(
                key = "${screen.macAddress.value}:${device.connectionNumber}",
                factory = factory
            )
            RenameScreen(
                viewModel = appleViewModel,
                navigateBack = navigateBack,
            )
        }

        Screen.AppSettings -> {
            val factory = viewModelFactory {
                initializer { AppSettingsViewModel(appDataRepository = appDataRepository) }
            }
            val appSettingsViewModel: AppSettingsViewModel = viewModel(factory = factory)

            AppSettingsScreen(
                viewModel = appSettingsViewModel,
                navigateBack = navigateBack,
                navigateToPurchase = ::navigateToPurchase,
                navigateToOpenSourceLicenses = { navigate(Screen.OpenSourceLicenses) },
                navigateToReleaseNotesScreen = { navigate(Screen.ReleaseNotes) },
                navigateToBleSettingsScreen = { navigate(Screen.BLESettings) }
            )
        }

        is Screen.HeadTracking -> {
            val device = devices[screen.macAddress] as? AppleDevice ?: return
            val factory = createAppleViewModelFactory(device, appDataRepository, recordingRepository, heartRateRepository)
            val appleViewModel: AppleViewModel = viewModel(
                key = "${screen.macAddress.value}:${device.connectionNumber}",
                factory = factory
            )
            HeadTrackingScreen(
                viewModel = appleViewModel,
                navigateBack = navigateBack,
                navigateToPurchase = ::navigateToPurchase
            )
        }

        is Screen.Accessibility -> {
            val device = devices[screen.macAddress] as? AppleDevice ?: return
            val factory = createAppleViewModelFactory(device, appDataRepository, recordingRepository, heartRateRepository)
            val appleViewModel: AppleViewModel = viewModel(
                key = "${screen.macAddress.value}:${device.connectionNumber}",
                factory = factory
            )

            AccessibilitySettingsScreen(
                viewModel = appleViewModel,
                navigateBack = navigateBack,
                navigateToPurchase = ::navigateToPurchase,
                navigateToTransparencyCustomization = {
                    navigate(Screen.TransparencyCustomization(screen.macAddress))
                }
            )
        }

        is Screen.TransparencyCustomization -> {
            val device = devices[screen.macAddress] as? AppleDevice ?: return
            val factory = createAppleViewModelFactory(device, appDataRepository, recordingRepository, heartRateRepository)
            val appleViewModel: AppleViewModel = viewModel(
                key = "${screen.macAddress.value}:${device.connectionNumber}",
                factory = factory
            )
            TransparencySettingsScreen(
                viewModel = appleViewModel,
                navigateBack = navigateBack,
            )
        }

        is Screen.HearingAid -> {
            val device = devices[screen.macAddress] as? AppleDevice ?: return
            val factory = createAppleViewModelFactory(device, appDataRepository, recordingRepository, heartRateRepository)
            val appleViewModel: AppleViewModel = viewModel(
                key = "${screen.macAddress.value}:${device.connectionNumber}",
                factory = factory
            )

            HearingAidScreen(
                viewModel = appleViewModel,
                navigateBack = navigateBack,
                navigateToHearingAidAdjustments = { navigate(Screen.HearingAidAdjustments(screen.macAddress)) },
                navigateToHearingTest = { navigate(Screen.UpdateHearingTest(screen.macAddress)) }
            )
        }

        is Screen.HearingAidAdjustments -> {
            val device = devices[screen.macAddress] as? AppleDevice ?: return
            val factory = createAppleViewModelFactory(device, appDataRepository, recordingRepository, heartRateRepository)
            val appleViewModel: AppleViewModel = viewModel(
                key = "${screen.macAddress.value}:${device.connectionNumber}",
                factory = factory
            )
            HearingAidAdjustmentsScreen(
                viewModel = appleViewModel,
                navigateBack = navigateBack,
            )
        }

        Screen.OpenSourceLicenses -> OpenSourceLicensesScreen(navigateBack)

        is Screen.UpdateHearingTest -> {
            val device = devices[screen.macAddress] as? AppleDevice ?: return
            val factory = createAppleViewModelFactory(device, appDataRepository, recordingRepository, heartRateRepository)
            val appleViewModel: AppleViewModel = viewModel(
                key = "${screen.macAddress.value}:${device.connectionNumber}",
                factory = factory
            )
            UpdateHearingTestRoute(
                viewModel = appleViewModel,
                navigateBack = navigateBack,
            )
        }

        is Screen.VersionInfo -> {
            val device = devices[screen.macAddress] as? AppleDevice ?: return
            val factory = createAppleViewModelFactory(device, appDataRepository, recordingRepository, heartRateRepository)
            val appleViewModel: AppleViewModel = viewModel(
                key = "${screen.macAddress.value}:${device.connectionNumber}",
                factory = factory
            )
            VersionScreen(
                viewModel = appleViewModel,
                navigateBack = navigateBack
            )
        }

        is Screen.HearingProtection -> {
            val device = devices[screen.macAddress] as? AppleDevice ?: return
            val factory = createAppleViewModelFactory(device, appDataRepository, recordingRepository, heartRateRepository)
            val appleViewModel: AppleViewModel = viewModel(
                key = "${screen.macAddress.value}:${device.connectionNumber}",
                factory = factory
            )
            HearingProtectionScreen(
                viewModel = appleViewModel,
                navigateBack = navigateBack,
                navigateToPurchase = ::navigateToPurchase
            )
        }

        is Screen.Purchase -> {
            val viewModel: PurchaseViewModel = viewModel()
            PurchaseScreen(
                viewModel = viewModel,
                navigateBack = navigateBack
            )
        }

        is Screen.Equalizer -> {
            val device = devices[screen.macAddress] as? AppleDevice ?: return
            val factory = createAppleViewModelFactory(device, appDataRepository, recordingRepository, heartRateRepository)
            val appleViewModel: AppleViewModel = viewModel(
                key = "${screen.macAddress.value}:${device.connectionNumber}",
                factory = factory
            )
            EqualizerRoute(
                viewModel = appleViewModel,
                navigateBack = navigateBack
            )
        }

        is Screen.LongPress -> {
            val device = devices[screen.macAddress] as? AppleDevice ?: return
            val factory = createAppleViewModelFactory(device, appDataRepository, recordingRepository, heartRateRepository)
            val appleViewModel: AppleViewModel = viewModel(
                key = "${screen.macAddress.value}:${device.connectionNumber}",
                factory = factory
            )
            LongPress(
                viewModel = appleViewModel,
                name = screen.bud,
                navigateBack = navigateBack,
                navigateToPurchase = ::navigateToPurchase
            )
        }

        is Screen.CallControl -> {
            val device = devices[screen.macAddress] as? AppleDevice ?: return
            val factory = createAppleViewModelFactory(device, appDataRepository, recordingRepository, heartRateRepository)
            val appleViewModel: AppleViewModel = viewModel(
                key = "${screen.macAddress.value}:${device.connectionNumber}",
                factory = factory
            )
            CallControlScreen(
                viewModel = appleViewModel,
                action = screen.action,
                navigateBack = navigateBack,
                onCallControlValueChanged = { flipped ->
                    device.setControlCommand(
                        ControlCommandIdentifier.CALL_MANAGEMENT_CONFIG,
                        if (flipped) byteArrayOf(0x00, 0x02) else byteArrayOf(0x00, 0x03)
                    )
                }
            )
        }

        is Screen.MicrophoneSettings -> {
            val device = devices[screen.macAddress] as? AppleDevice ?: return
            val factory = createAppleViewModelFactory(device, appDataRepository, recordingRepository, heartRateRepository)
            val appleViewModel: AppleViewModel = viewModel(
                key = "${screen.macAddress.value}:${device.connectionNumber}",
                factory = factory
            )
            MicrophoneSettingsRoute(
                viewModel = appleViewModel,
                navigateBack = navigateBack
            )
        }

        is Screen.ReleaseNotes -> {
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

        is Screen.Recording -> {
            val device = devices[screen.macAddress] as? AppleDevice ?: return
            val factory = createAppleViewModelFactory(device, appDataRepository, recordingRepository, heartRateRepository)
            val appleViewModel: AppleViewModel = viewModel(
                key = "${screen.macAddress.value}:${device.connectionNumber}",
                factory = factory
            )
            RecordingScreenRoute(
                viewModel = appleViewModel,
                navigateBack = navigateBack
            )
        }

        is Screen.Debug -> {
            val device = devices[screen.macAddress] as? AppleDevice ?: return
            val factory = createAppleViewModelFactory(device, appDataRepository, recordingRepository, heartRateRepository)
            val appleViewModel: AppleViewModel = viewModel(
                key = "${screen.macAddress.value}:${device.connectionNumber}",
                factory = factory
            )
            DebugRoute(
                viewModel = appleViewModel,
                navigateBack = navigateBack
            )
        }

        is Screen.BLESettings -> {
            val factory = viewModelFactory {
                initializer { AppSettingsViewModel(appDataRepository = appDataRepository) }
            }
            val appSettingsViewModel: AppSettingsViewModel = viewModel(factory = factory)
            BLESettingsScreenRoute(
                viewModel = appSettingsViewModel,
                navigateBack = navigateBack
            )
        }

        is Screen.HeartRate -> {
            val device = devices[screen.macAddress] as? AppleDevice ?: return
            val factory = createAppleViewModelFactory(device, appDataRepository, recordingRepository, heartRateRepository)
            val appleViewModel: AppleViewModel = viewModel(
                key = "${screen.macAddress.value}:${device.connectionNumber}",
                factory = factory
            )
            HeartRateRoute(
                viewModel = appleViewModel,
                navigateBack = navigateBack
            )
        }
    }
}
