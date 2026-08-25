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

package me.kavishdevar.librepods.presentation.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.NoiseControlMode
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.theme.sectionHeader
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@SuppressLint("UnspecifiedRegisterReceiverFlag", "UnusedBoxWithConstraintsScope")
@Composable
fun NoiseControlSettings(
    showOffListeningMode: Boolean,
    noiseControlModeValue: Int,
    onNoiseControlModeChanged: (Int) -> Unit
) {
    when (LocalDesignSystem.current) {
        DesignSystem.Material -> {
            val options = remember(showOffListeningMode) {
                buildList {
                    if (showOffListeningMode) {
                        add(
                            Triple(
                                NoiseControlMode.OFF,
                                R.string.off,
                                R.drawable.noise_cancellation
                            )
                        )
                    }

                    add(
                        Triple(
                            NoiseControlMode.TRANSPARENCY,
                            R.string.transparency,
                            R.drawable.transparency
                        )
                    )
                    add(
                        Triple(
                            NoiseControlMode.ADAPTIVE,
                            R.string.adaptive,
                            R.drawable.adaptive
                        )
                    )
                    add(
                        Triple(
                            NoiseControlMode.NOISE_CANCELLATION,
                            R.string.noise_cancellation,
                            R.drawable.noise_cancellation
                        )
                    )
                }
            }

            val selectedMode = NoiseControlMode.entries[(noiseControlModeValue - 1).coerceIn(0, NoiseControlMode.entries.lastIndex)]

            Column {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.noise_control),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmallEmphasized
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                ) {
                    options.forEachIndexed { index, (mode, labelRes, iconRes) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            ToggleButton(
                                checked = selectedMode == mode,
                                onCheckedChange = {
                                    if (it) {
                                        onNoiseControlModeChanged(mode.ordinal + 1)
                                    }
                                },
                                shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                },
                                colors = ToggleButtonDefaults.toggleButtonColors()
                                    .copy(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Icon(
                                    bitmap = ImageBitmap.imageResource(iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Text(
                                text = stringResource(labelRes),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                lineHeight = 14.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        DesignSystem.Apple -> {
            val isDarkTheme = isSystemInDarkTheme()
            val backgroundColor = if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFE3E3E8)
            val textColor = if (isDarkTheme) Color.White else Color.Black
            val selectedBackground = if (isDarkTheme) Color(0xBF5C5A5F) else Color(0xFFFFFFFF)

            val receivedMode = NoiseControlMode.entries[
                (noiseControlModeValue - 1).coerceIn(0, NoiseControlMode.entries.lastIndex)
            ].let { mode ->
                if (!showOffListeningMode && mode == NoiseControlMode.OFF) {
                    NoiseControlMode.TRANSPARENCY
                } else {
                    mode
                }
            }
            var selectedMode by remember { mutableStateOf(receivedMode) }
            LaunchedEffect(receivedMode) {
                selectedMode = receivedMode
            }

            fun onModeSelected(mode: NoiseControlMode) {
                val targetMode = if (!showOffListeningMode && mode == NoiseControlMode.OFF) {
                    NoiseControlMode.TRANSPARENCY
                } else {
                    mode
                }

                if (targetMode != selectedMode) {
                    selectedMode = targetMode
                    onNoiseControlModeChanged(targetMode.ordinal + 1)
                }
            }

            val firstDividerAlpha by animateFloatAsState(
                targetValue = if (
                    selectedMode == NoiseControlMode.OFF ||
                    selectedMode == NoiseControlMode.TRANSPARENCY
                ) 0f else 1f,
                label = "off-transparency divider"
            )
            val secondDividerAlpha by animateFloatAsState(
                targetValue = if (
                    selectedMode == NoiseControlMode.TRANSPARENCY ||
                    selectedMode == NoiseControlMode.ADAPTIVE
                ) 0f else 1f,
                label = "transparency-adaptive divider"
            )
            val thirdDividerAlpha by animateFloatAsState(
                targetValue = if (
                    selectedMode == NoiseControlMode.ADAPTIVE ||
                    selectedMode == NoiseControlMode.NOISE_CANCELLATION
                ) 0f else 1f,
                label = "adaptive-noise-cancellation divider"
            )

            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.noise_control),
                    color = MaterialTheme.colorScheme.sectionHeader,
                    style = MaterialTheme.typography.labelSmallEmphasized
                )
            }
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                val density = LocalDensity.current
                val buttonCount = if (showOffListeningMode) 4 else 3
                val buttonWidth = maxWidth / buttonCount

                var isDragging by remember { mutableStateOf(false) }
                var dragOffset by remember(buttonWidth, density, showOffListeningMode) {
                    mutableFloatStateOf(0f)
                }

                val animationSpec: AnimationSpec<Float> = remember {
                    SpringSpec(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = 0.01f
                    )
                }

                val targetOffset = buttonWidth * when(selectedMode) {
                    NoiseControlMode.OFF -> if (showOffListeningMode) 0 else 1
                    NoiseControlMode.TRANSPARENCY -> if (showOffListeningMode) 1 else 0
                    NoiseControlMode.ADAPTIVE -> if (showOffListeningMode) 2 else 1
                    NoiseControlMode.NOISE_CANCELLATION -> if (showOffListeningMode) 3 else 2
                }
                val targetOffsetPx = with(density) { targetOffset.toPx() }

                val animatedOffset by animateFloatAsState(
                    targetValue = if (isDragging) dragOffset else targetOffsetPx,
                    animationSpec = animationSpec,
                    label = "selector"
                )

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(backgroundColor, RoundedCornerShape(28.dp))
                            .draggable(
                                orientation = Orientation.Horizontal,
                                state = rememberDraggableState { delta ->
                                    dragOffset = (dragOffset + delta).coerceIn(
                                        0f,
                                        with(density) {
                                            (buttonWidth * (buttonCount - 1)).toPx()
                                        }
                                    )
                                },
                                onDragStarted = {
                                    dragOffset = targetOffsetPx
                                    isDragging = true
                                },
                                onDragStopped = {
                                    isDragging = false
                                    val position = dragOffset / with(density) { buttonWidth.toPx() }
                                    val newIndex = position.roundToInt()
                                    val newMode = when (newIndex) {
                                        0 -> if (showOffListeningMode) NoiseControlMode.OFF else NoiseControlMode.TRANSPARENCY
                                        1 -> if (showOffListeningMode) NoiseControlMode.TRANSPARENCY else NoiseControlMode.ADAPTIVE
                                        2 -> if (showOffListeningMode) NoiseControlMode.ADAPTIVE else NoiseControlMode.NOISE_CANCELLATION
                                        3 -> NoiseControlMode.NOISE_CANCELLATION
                                        else -> selectedMode
                                    }
                                    onModeSelected(newMode)
                                }
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .width(buttonWidth)
                                .fillMaxHeight()
                                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                                .zIndex(0f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(3.dp)
                                    .background(selectedBackground, RoundedCornerShape(26.dp))
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(1f)
                        ) {
                            if (showOffListeningMode) {
                                NoiseControlButton(
                                    icon = ImageBitmap.imageResource(R.drawable.noise_cancellation),
                                    onClick = { onModeSelected(NoiseControlMode.OFF) },
                                    textColor = textColor,
                                    modifier = Modifier.weight(1f),
                                    usePadding = false
                                )
                                VerticalDivider(
                                    thickness = 1.dp,
                                    modifier = Modifier
                                        .padding(vertical = 10.dp)
                                        .alpha(firstDividerAlpha),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                )
                            }
                            NoiseControlButton(
                                icon = ImageBitmap.imageResource(R.drawable.transparency),
                                onClick = { onModeSelected(NoiseControlMode.TRANSPARENCY) },
                                textColor = textColor,
                                modifier = Modifier.weight(1f),
                                usePadding = false
                            )
                            VerticalDivider(
                                thickness = 1.dp,
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .alpha(secondDividerAlpha),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                            NoiseControlButton(
                                icon = ImageBitmap.imageResource(R.drawable.adaptive),
                                onClick = { onModeSelected(NoiseControlMode.ADAPTIVE) },
                                textColor = textColor,
                                modifier = Modifier.weight(1f),
                                usePadding = false
                            )
                            VerticalDivider(
                                thickness = 1.dp,
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .alpha(thirdDividerAlpha),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                            NoiseControlButton(
                                icon = ImageBitmap.imageResource(R.drawable.noise_cancellation),
                                onClick = { onModeSelected(NoiseControlMode.NOISE_CANCELLATION) },
                                textColor = textColor,
                                modifier = Modifier.weight(1f),
                                usePadding = false
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        if (showOffListeningMode) {
                            Text(
                                text = stringResource(R.string.off),
                                style = TextStyle(fontSize = 12.sp, color = textColor),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = stringResource(R.string.transparency),
                            style = TextStyle(fontSize = 12.sp, color = textColor),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.adaptive),
                            style = TextStyle(fontSize = 12.sp, color = textColor),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.noise_cancellation),
                            style = TextStyle(fontSize = 12.sp, color = textColor),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}


@Preview
@Composable
fun NoiseControlSettingsPreview() {
    LibrePodsTheme(
        m3eEnabled = true
    ) {
        Box(
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            NoiseControlSettings(
                showOffListeningMode = false,
                noiseControlModeValue = 2,
                onNoiseControlModeChanged = { }
            )
        }
    }
}
