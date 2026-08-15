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

package me.kavishdevar.librepods.presentation.screens.apple

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.StyledList
import me.kavishdevar.librepods.presentation.components.StyledListItem
import me.kavishdevar.librepods.presentation.components.StyledScaffold
import me.kavishdevar.librepods.presentation.viewmodel.AppleViewModel

@Composable
fun VersionScreen(
    viewModel: AppleViewModel,
    navigateBack: (() -> Unit)?
) {
    val uiState by viewModel.uiState.collectAsState()

    val metadata = uiState.metadata

    StyledScaffold(
        title = stringResource(R.string.version),
        navigateBack = navigateBack
    ) { topPadding, bottomPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(topPadding))
            StyledList(title = stringResource(R.string.version)) {
                StyledListItem(
                    contentText = stringResource(R.string.version) + " 1",
                    supportingText = metadata.version1,
                    enabled = false
                )

                StyledListItem(
                    contentText = stringResource(R.string.version) + " 2",
                    supportingText = metadata.version2,
                    enabled = false
                )

                StyledListItem(
                    contentText = stringResource(R.string.version) + " 3",
                    supportingText = metadata.version3,
                    enabled = false
                )
            }
            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
}
