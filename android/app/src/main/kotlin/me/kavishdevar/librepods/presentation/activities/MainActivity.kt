/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

@file:OptIn(ExperimentalEncodingApi::class)

package me.kavishdevar.librepods.presentation.activities

//import dagger.hilt.android.AndroidEntryPoint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.android.play.core.review.ReviewManagerFactory
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.flow.MutableStateFlow
import me.kavishdevar.librepods.BuildConfig
import me.kavishdevar.librepods.LibrePodsApplication
import me.kavishdevar.librepods.presentation.navigation.NavigationRoot
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.presentation.theme.NightTheme
import me.kavishdevar.librepods.repository.AppDataRepository
import me.kavishdevar.librepods.services.LibrePodsService
import me.kavishdevar.librepods.utils.XposedState
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration.Companion.hours

private lateinit var serviceConnection: ServiceConnection
private lateinit var connectionStatusReceiver: BroadcastReceiver
//lateinit var testReviewReceiver: BroadcastReceiver

class MainActivity : ComponentActivity() {
    companion object {
        init {
            if (XposedState.isAvailable && XposedState.bluetoothScopeEnabled) {
                System.loadLibrary("fluoride_hooks")
            }
        }
    }

    val appDataRepository by lazy { (application as LibrePodsApplication).appDataRepository }
    @ExperimentalHazeMaterialsApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by appDataRepository.settings.collectAsState()

            val systemDarkTheme = isSystemInDarkTheme()

            val darkTheme = when (settings.nightMode) {
                NightTheme.Dark -> true
                NightTheme.Light -> false
                NightTheme.System -> systemDarkTheme
            }

            val view = LocalView.current
            val window = (view.context as Activity).window

            LaunchedEffect(darkTheme) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }

            LaunchedEffect(settings.swipeAnywhereForBack) {
                if (settings.useHighestRefreshRate) {
                    val display = getSystemService(DisplayManager::class.java)
                        .getDisplay(Display.DEFAULT_DISPLAY)

                    val highest = display.supportedModes
                        .maxByOrNull { it.refreshRate }

                    highest?.let {
                        window.attributes = window.attributes.apply {
                            preferredRefreshRate = it.refreshRate
                        }
                    }
                }
            }

            LibrePodsTheme(
                designSystem = settings.designSystem,
                overrideMaterialColor = settings.overrideMaterialColor,
                darkTheme = darkTheme
            ) {
//                For demo screenshots
//                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
//                windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//                windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())

                Main()
            }
        }
    }

    override fun onDestroy() {
        if (::serviceConnection.isInitialized) try {
            unbindService(serviceConnection)
            Log.d("MainActivity", "Unbound service")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error while unbinding service: $e")
        }
        if (::connectionStatusReceiver.isInitialized) try {
            unregisterReceiver(connectionStatusReceiver)
            Log.d("MainActivity", "Unregistered receiver")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error while unregistering receiver: $e")
        }
        super.onDestroy()
    }
}

@Composable
fun Main() {
    val context = LocalContext.current
    val librepodsService = remember { mutableStateOf<LibrePodsService?>(null) }

    val appDataRepository: AppDataRepository = (LocalContext.current.applicationContext as LibrePodsApplication).appDataRepository
    val appState by appDataRepository.state.collectAsState()

    LaunchedEffect(Unit) {
        if (BuildConfig.PLAY_BUILD) {
            val now = System.currentTimeMillis()
            val firstConn = appState.firstSuccessfulConnectionTime?: 0L

            val alreadyPrompted = appState.reviewPrompted

            val oneDay = 24.hours.inWholeMilliseconds

            if (
                firstConn != 0L &&
                !alreadyPrompted &&
                (now - firstConn) > oneDay
            ) {
                triggerReviewFlow(context as? Activity ?: return@LaunchedEffect)

                appDataRepository.updateState {
                    it.copy(reviewPrompted = true)
                }
            }
        }
    }

    val onboardingComplete = appState.hasCompletedOnboarding

    val currentVersion = BuildConfig.VERSION_NAME.removeSuffix("-debug").removeSuffix("-play")
    val lastVersionShown = appState.lastVersionShown

    val releaseNotesShown = lastVersionShown == currentVersion

    val devicesState = remember(librepodsService.value) {
        librepodsService.value?.devices ?: MutableStateFlow(emptyMap())
    }.collectAsState()

    DisposableEffect(onboardingComplete) {
        if (!onboardingComplete) {
            onDispose { }
        } else {
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val binder = service as LibrePodsService.LocalBinder
                    librepodsService.value = binder.getService()
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    librepodsService.value = null
                }
            }

            context.startForegroundService(Intent(context, LibrePodsService::class.java))

            context.bindService(
                Intent(context, LibrePodsService::class.java),
                connection,
                Context.BIND_AUTO_CREATE
            )

            onDispose {
                try {
                    context.unbindService(connection)
                } catch(e: Exception) {
                    Log.w("Main", "Error while unbinding service: $e")
                }
            }
        }
    }

    fun bindService() {
        context.startForegroundService(Intent(context, LibrePodsService::class.java))
        serviceConnection = object: ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as LibrePodsService.LocalBinder
                val service = binder.getService()
                librepodsService.value = service
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                librepodsService.value = null
            }
        }

        context.bindService(
            Intent(context, LibrePodsService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    NavigationRoot(
        showReleaseNotes = !releaseNotesShown,
        updatesShown = {
            appDataRepository.updateState {
                it.copy(lastVersionShown = currentVersion)
            }
        },
        showOnboarding = !onboardingComplete,
        onboardingComplete = {
            appDataRepository.updateState {
                it.copy(hasCompletedOnboarding = true)
            }
            bindService()
        },
        devicesState = devicesState
    )
}

private fun triggerReviewFlow(activity: Activity) {
    val manager = ReviewManagerFactory.create(activity)
    val request = manager.requestReviewFlow()
    request.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val reviewInfo = task.result
            manager.launchReviewFlow(activity, reviewInfo)
        }
    }
}
