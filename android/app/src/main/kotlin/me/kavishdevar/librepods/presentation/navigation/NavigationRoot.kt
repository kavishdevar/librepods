package me.kavishdevar.librepods.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.NavBackStack
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.serializer
import me.kavishdevar.librepods.bluetooth.MacAddress
import me.kavishdevar.librepods.devices.AppleDevice
import me.kavishdevar.librepods.devices.ConnectionState
import me.kavishdevar.librepods.devices.Device

@Composable
fun rememberScreenNavBackStack(vararg elements: Screen): NavBackStack<Screen> {
    return rememberSerializable(serializer = serializer()) {
        NavBackStack(*elements)
    }
}

@Composable
fun NavigationRoot(
    showReleaseNotes: Boolean = false,
    updatesShown: () -> Unit = {},
    showOnboarding: Boolean = false,
    onboardingComplete: () -> Unit = {},
    devicesState: State<Map<MacAddress, Device<*, *, *>>>
) {
    val devices by devicesState

    val backStack = rememberScreenNavBackStack(
        when {
            showOnboarding -> Screen.Onboarding
            showReleaseNotes -> Screen.ReleaseNotes
            else -> Screen.DeviceList
        }
    )

    val connectedDevice = devices.values.firstOrNull { it.connectionState.collectAsState().value == ConnectionState.CONNECTED }

    LaunchedEffect(connectedDevice) {
        if (connectedDevice != null) {
            val targetScreen = when (connectedDevice) {
                is AppleDevice -> Screen.AppleScreen(connectedDevice.macAddress)
            }

            if (targetScreen !in backStack) {
                backStack.add(targetScreen)
            }
        }
    }

    AppNavGraph(
        backStack = backStack,
        devicesState = devicesState,
        showReleaseNotes = showReleaseNotes,
        updatesShown = updatesShown,
        onboardingComplete = onboardingComplete,
    )
}
