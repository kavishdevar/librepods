package me.kavishdevar.librepods.presentation.screens.apple

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.aacp.types.ControlCommandIdentifier
import me.kavishdevar.librepods.presentation.components.StyledList
import me.kavishdevar.librepods.presentation.components.StyledListItem
import me.kavishdevar.librepods.presentation.components.StyledScaffold
import me.kavishdevar.librepods.presentation.viewmodel.AppleViewModel

@Composable
fun MicrophoneSettingsRoute(
    viewModel: AppleViewModel,
    navigateBack: (() -> Unit)?
) {
    val uiState by viewModel.uiState.collectAsState()

    val state = uiState.state

    val id = ControlCommandIdentifier.MIC_MODE

    MicrophoneSettingsScreen(
        navigateBack = navigateBack,
        selectedMode = state.controlStates[id]?.getOrNull(0)?.toInt() ?: 0,
        onMicrophoneSettingsChanged = {
            viewModel.setControlCommand(id, it)
        }
    )
}

@Composable
fun MicrophoneSettingsScreen(
    navigateBack: (() -> Unit)?,
    selectedMode: Int,
    onMicrophoneSettingsChanged: (Int) -> Unit
) {
    val scrollState = rememberScrollState()

    StyledScaffold(
        title = stringResource(R.string.microphone_mode),
        navigateBack = navigateBack
    ) { topPadding, bottomPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .verticalScroll(scrollState)
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(topPadding))

            StyledList {
                StyledListItem(
                    contentText = stringResource(R.string.microphone_automatic),
                    selected = selectedMode == 0,
                    onClick = { onMicrophoneSettingsChanged(0) }
                )

                StyledListItem(
                    contentText = stringResource(R.string.microphone_always_right),
                    selected = selectedMode == 1,
                    onClick = { onMicrophoneSettingsChanged(1) }
                )

                StyledListItem(
                    contentText = stringResource(R.string.microphone_always_left),
                    selected = selectedMode == 2,
                    onClick = { onMicrophoneSettingsChanged(2) }
                )
            }

            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
}
