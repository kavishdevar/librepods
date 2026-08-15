package me.kavishdevar.librepods.presentation.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import me.kavishdevar.librepods.LibrePodsApplication
import me.kavishdevar.librepods.bluetooth.MacAddress
import me.kavishdevar.librepods.devices.AppleDevice
import me.kavishdevar.librepods.devices.ConnectionState
import me.kavishdevar.librepods.devices.Device

@Composable
fun AppNavGraph(
    backStack: NavBackStack<Screen>,
    devicesState: State<Map<MacAddress, Device<*, *, *>>>,
    showReleaseNotes: Boolean = false,
    updatesShown: () -> Unit = {},
    onboardingComplete: () -> Unit = {},
) {
    val backRequests = remember { Channel<CompletableDeferred<Unit>>(Channel.BUFFERED) }

    val devices by devicesState
    val context = LocalContext.current

    val appDataRepository by lazy { (context.applicationContext as LibrePodsApplication).appDataRepository }
    val recordingRepository by lazy { (context.applicationContext as LibrePodsApplication).recordingRepository }
    val heartRateRepository by lazy { (context.applicationContext as LibrePodsApplication).heartRateRepository }

    val currentDevice: Device<*, *, *>? = devices[backStack.lastOrNull()?.let { (it as? DeviceScreen)?.macAddress }]

    @SuppressLint("UnrememberedMutableState")
    val currentConnectionState by (currentDevice as? AppleDevice)?.connectionState?.collectAsState()?: mutableStateOf(ConnectionState.DISCONNECTED)

    LaunchedEffect(currentConnectionState) {
        if (currentConnectionState == ConnectionState.DISCONNECTED) {
            while (
                backStack.size > 1 &&
                backStack.lastOrNull() is DeviceScreen
            ) {
                val completed = CompletableDeferred<Unit>()
                backRequests.send(completed)
                completed.await()
            }
        }
    }

    val appSettings by appDataRepository.settings.collectAsState()

    val sceneStrategy = remember(appSettings.swipeAnywhereForBack) {
        SwipeBackSceneStrategy<Screen>(
            enabled = appSettings.swipeAnywhereForBack,
            backRequests = backRequests,
            onDismiss = {
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.lastIndex)
                }
            }
        )
    }

    SharedTransitionLayout {
        CompositionLocalProvider(
            LocalSharedTransitionScope provides this,
        ) {
            NavDisplay(
                sharedTransitionScope = this,
                backStack = backStack,
                sceneStrategies = listOf(sceneStrategy),
                onBack = {
                    if (backStack.size > 1) {
                        backStack.removeAt(backStack.lastIndex)
                    }
                },
                entryProvider = { screen ->
                    NavEntry(screen) {
                        RenderScreenContent(
                            screen = screen,
                            backStack = backStack,
                            backRequests = backRequests,
                            devices = devices,
                            appDataRepository = appDataRepository,
                            recordingRepository = recordingRepository,
                            heartRateRepository = heartRateRepository,
                            showReleaseNotes = showReleaseNotes,
                            updatesShown = updatesShown,
                            onboardingComplete = onboardingComplete
                        )
                    }
                },
                transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                predictivePopTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None }
            )
        }
    }
}
