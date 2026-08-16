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

package me.kavishdevar.librepods.presentation.components.apple

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.primitives.StyledList
import me.kavishdevar.librepods.presentation.components.primitives.StyledListItem
import me.kavishdevar.librepods.presentation.icons.richText
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable
fun AboutCard(
    modelName: String,
    actualModel: String,
    serialNumbers: List<String>,
    version: String?,
    navigateToVersion: () -> Unit
) {
    val serialNumbers = listOf(
        richText(serialNumbers[0]),
        richText("\\icon{LeftCircleFill} " + serialNumbers[1]),
        richText("\\icon{RightCircleFill} " + serialNumbers[2]),
    )

    val serialNumber = remember { mutableIntStateOf(0) }

    StyledList(title = stringResource(R.string.about)) {
        StyledListItem(
            contentText = stringResource(R.string.model_name),
            supportingText = modelName
        )

        StyledListItem(
            contentText = stringResource(R.string.model_number),
            supportingText = actualModel
        )

        StyledListItem(
            contentText = stringResource(R.string.serial_number),
            supportingContent = {
                Text(
                    text = serialNumbers[serialNumber.intValue].text,
                    inlineContent = serialNumbers[serialNumber.intValue].inlineContent,
                    style = if (LocalDesignSystem.current == DesignSystem.Apple) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                )
            },
            onClick = { serialNumber.intValue = (serialNumber.intValue + 1) % serialNumbers.size }
        )

        if (version != null) {
            StyledListItem(
                contentText = stringResource(R.string.version),
                supportingText = version,
                onClick = navigateToVersion,
            )
        } else {
            StyledListItem(
                contentText = stringResource(R.string.version),
                onClick = navigateToVersion,
            )
        }
    }
}

@Preview
@Composable
fun AboutCardPreview() {
    LibrePodsTheme(
        designSystem = DesignSystem.Apple
    ) {
        AboutCard(
            modelName = "AirPods Pro",
            actualModel = "A2084",
            serialNumbers = listOf("123456789", "987654321", "567890123"),
            version = "9141234",
            navigateToVersion = {}
        )
    }
}
