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

package me.kavishdevar.librepods.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.libraryColors
import com.mikepenz.aboutlibraries.ui.compose.m3.style.m3VariantColors
import com.mikepenz.aboutlibraries.ui.compose.style.DefaultLibraryActionBadges
import com.mikepenz.aboutlibraries.ui.compose.variant.LibrariesVariant
import com.mikepenz.aboutlibraries.ui.compose.variant.LibraryActionMode
import com.mikepenz.aboutlibraries.ui.compose.variant.LibraryBadges
import com.mikepenz.aboutlibraries.ui.compose.variant.LibraryDetailMode
import com.mikepenz.aboutlibraries.ui.compose.variant.LibraryInlineDetail
import com.mikepenz.aboutlibraries.ui.compose.variant.LibraryRow
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.StyledScaffold
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem

@Composable
fun OpenSourceLicensesScreen(
    navigateBack: (() -> Unit)?
) {
    StyledScaffold(
        title = stringResource(R.string.open_source_licenses),
        navigateBack = navigateBack
    ) { topPadding, bottomPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(topPadding))

            val libraries by produceLibraries(R.raw.aboutlibraries)

            val count = libraries?.libraries?.size ?: 0

            LibrariesContainer(
                libraries = libraries,
                modifier = Modifier.fillMaxSize(),

                contentPadding = PaddingValues(top = 16.dp, bottom = bottomPadding),

                badges = LibraryBadges(version = true),

                variant = LibrariesVariant.Refined,
                detailMode = LibraryDetailMode.Inline,

                colors = LibraryDefaults.libraryColors(
                    libraryBackgroundColor = MaterialTheme.colorScheme.surface,
                    libraryContentColor = MaterialTheme.colorScheme.onBackground,
                ),

                variantColors = LibraryDefaults.m3VariantColors(
                    rowBackground = MaterialTheme.colorScheme.surfaceContainer,
                    rowOnBackground = MaterialTheme.colorScheme.onSurface,
                    rowExpandedBackground = MaterialTheme.colorScheme.surfaceContainer
                ),

                divider = {
                    Spacer(modifier = Modifier.height(2.dp))
                },

                libraryRow = { index, library, expanded, toggle, style ->
                    val transition = updateTransition(
                        targetState = expanded,
                        label = "library"
                    )

                    val bottomCorner by transition.animateDp(
                        transitionSpec = {
                            spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            )
                        },
                        label = "bottomCorner"
                    ) { expanded ->
                        if (expanded) 0.dp else if (index == count - 1) 24.dp else 8.dp
                    }

                    val topCorner = when {
                        count == 1 -> 24.dp
                        index == 0 -> 24.dp
                        index == count - 1 -> 8.dp
                        else -> 8.dp
                    }

                    val shape = RoundedCornerShape(
                        topStart = topCorner,
                        topEnd = topCorner,
                        bottomStart = bottomCorner,
                        bottomEnd = bottomCorner,
                    )

                    LibraryRow(
                        library = library,
                        expanded = expanded,
                        onToggle = toggle,
                        style = style,
                        variant = LibrariesVariant.Refined,
                        badges = LibraryBadges(version = true),
                        modifier = Modifier.clip(shape)
                    )

                    transition.AnimatedVisibility(
                        visible = { it },
                        enter = expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            )
                        ),
                        exit = shrinkVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            )
                        ),
                    ) {
                        LibraryInlineDetail(
                            library = library,
                            actionMode = LibraryActionMode.Chips,
                            style = style,
                            actionLabels = DefaultLibraryActionBadges,
                            onActionClick = { _, _ -> false },
                            onDialogRequest = { },
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        bottomStart = if (index == count - 1) 24.dp else 8.dp,
                                        bottomEnd = if (index == count - 1) 24.dp else 8.dp
                                    )
                                )
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                        )
                    }
                }
            )

            Spacer(Modifier.height(bottomPadding))
        }
    }
}
