package me.kavishdevar.librepods.presentation.activities

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import me.kavishdevar.librepods.LibrePodsApplication
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.primitives.StyledScaffold
import me.kavishdevar.librepods.presentation.screens.onboarding.PrivacyPolicyPage
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.presentation.theme.NightTheme

class PrivacyPolicyActivity : ComponentActivity() {
    val appDataRepository by lazy { (application as LibrePodsApplication).appDataRepository }
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

            LibrePodsTheme(
                designSystem = settings.designSystem,
                darkTheme = darkTheme
            ) {
//                For demo screenshots
//                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
//                windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//                windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())

                StyledScaffold(
                    title = stringResource(R.string.privacy_policy),
                    navigateBack = { finish() }
                ) { topPadding, bottomPadding ->
                    Column {
                        Spacer(modifier = Modifier.height(topPadding))

                        Surface(
                            shape = RoundedCornerShape(52.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(52.dp))
                                .padding(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            PrivacyPolicyPage { finish() }
                        }

                        Spacer(modifier = Modifier.height(bottomPadding))
                    }
                }
            }
        }
    }
}
