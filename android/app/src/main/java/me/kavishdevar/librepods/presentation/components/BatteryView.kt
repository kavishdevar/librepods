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

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryComponent
import me.kavishdevar.librepods.data.BatteryStatus
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.utils.BatteryLevels

@Composable
fun BatteryView(
    batteryList: List<Battery>,
    budsRes: Int,
    caseRes: Int
) {
    val resources = LocalResources.current
    val budsBitmap = remember(resources, budsRes) {
        ImageBitmap.imageResource(resources, budsRes)
    }
    val caseBitmap = remember(resources, caseRes) {
        ImageBitmap.imageResource(resources, caseRes)
    }

    if (LocalDesignSystem.current == DesignSystem.Material) {
        MaterialBatteryView(
            batteryList = batteryList,
            budsBitmap = budsBitmap,
            caseBitmap = caseBitmap
        )
        return
    }

    val left = batteryList.find { it.component == BatteryComponent.LEFT }
    val right = batteryList.find { it.component == BatteryComponent.RIGHT }
    val case = batteryList.find { it.component == BatteryComponent.CASE }

    val leftLevel = left?.level ?: BatteryLevels.UNKNOWN_LEVEL
    val rightLevel = right?.level ?: BatteryLevels.UNKNOWN_LEVEL
    val caseLevel = case?.level ?: BatteryLevels.UNKNOWN_LEVEL

    val leftVisible = left != null && left.status != BatteryStatus.DISCONNECTED
    val rightVisible = right != null && right.status != BatteryStatus.DISCONNECTED
    val caseVisible = case != null && case.status != BatteryStatus.DISCONNECTED

    val displayCombinedBuds =
        leftVisible && rightVisible &&
            BatteryLevels.isKnown(leftLevel) && BatteryLevels.isKnown(rightLevel) &&
            left.status == right.status &&
            (leftLevel - rightLevel) in -3..3

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.widthIn(max = 500.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    bitmap = budsBitmap,
                    contentDescription = stringResource(R.string.buds),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )

                if (displayCombinedBuds) {
                    BatteryIndicator(
                        leftLevel.coerceAtMost(rightLevel),
                        left.status
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (leftVisible) {
                            BatteryIndicator(
                                leftLevel,
                                left.status,
                                "\uDBC6\uDCE5"
                            )
                        }

                        if (leftVisible && rightVisible) {
                            Spacer(modifier = Modifier.width(16.dp))
                        }

                        if (rightVisible) {
                            BatteryIndicator(
                                rightLevel,
                                right.status,
                                "\uDBC6\uDCE8"
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    bitmap = caseBitmap,
                    contentDescription = stringResource(R.string.case_alt),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )

                if (caseVisible) {
                    BatteryIndicator(
                        caseLevel,
                        case.status,
                        prefix = if (!displayCombinedBuds) "\uDBC3\uDE6C" else ""
                    )
                }
            }
        }
    }
}

@Composable
private fun MaterialBatteryView(
    batteryList: List<Battery>,
    budsBitmap: ImageBitmap,
    caseBitmap: ImageBitmap
) {
    val left = batteryList.find { it.component == BatteryComponent.LEFT }
        ?.takeUnless { it.status == BatteryStatus.DISCONNECTED }
    val right = batteryList.find { it.component == BatteryComponent.RIGHT }
        ?.takeUnless { it.status == BatteryStatus.DISCONNECTED }
    val case = batteryList.find { it.component == BatteryComponent.CASE }
        ?.takeUnless { it.status == BatteryStatus.DISCONNECTED }

    val budsLabel = stringResource(R.string.buds)
    val leftLabel = stringResource(R.string.left)
    val rightLabel = stringResource(R.string.right)
    val caseLabel = stringResource(R.string.case_alt)
    val displayedBatteries = buildList {
        if (left != null && right != null &&
            BatteryLevels.isKnown(left.level) && BatteryLevels.isKnown(right.level) &&
            left.status == right.status && (left.level - right.level) in -3..3
        ) {
            add(budsLabel to left.copy(level = minOf(left.level, right.level)))
        } else {
            left?.let { add(leftLabel to it) }
            right?.let { add(rightLabel to it) }
        }
        case?.let { add(caseLabel to it) }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    bitmap = budsBitmap,
                    contentDescription = stringResource(R.string.buds),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .weight(1.15f)
                        .height(106.dp)
                )
                Image(
                    bitmap = caseBitmap,
                    contentDescription = stringResource(R.string.case_alt),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .weight(0.85f)
                        .height(96.dp)
                )
            }

            if (displayedBatteries.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    displayedBatteries.forEach { (label, battery) ->
                        MaterialBatteryPill(
                            label = label,
                            battery = battery,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialBatteryPill(
    label: String,
    battery: Battery,
    modifier: Modifier = Modifier
) {
    val known = BatteryLevels.isKnown(battery.level)
    val batteryColor = when {
        !known -> MaterialTheme.colorScheme.onSurfaceVariant
        battery.level <= 20 -> MaterialTheme.colorScheme.error
        battery.level <= 40 -> Color(0xFFFFB300)
        else -> Color(0xFF36C56B)
    }
    val charging = battery.status == BatteryStatus.CHARGING ||
        battery.status == BatteryStatus.OPTIMIZED_CHARGING

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(batteryColor, CircleShape)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = buildString {
                        if (charging) append("⚡")
                        append(BatteryLevels.displayPercent(battery.level))
                    },
                    style = MaterialTheme.typography.labelMediumEmphasized,
                    maxLines = 1
                )
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BatteryViewPreview() {
    val fakeBattery = listOf(
        Battery(BatteryComponent.LEFT, 85, BatteryStatus.CHARGING),
        Battery(BatteryComponent.RIGHT, 40, BatteryStatus.OPTIMIZED_CHARGING),
        Battery(BatteryComponent.CASE, 60, BatteryStatus.NOT_CHARGING)
    )

    val bg = if (isSystemInDarkTheme()) Color.Black else Color(0xFFF2F2F7)

    Box(
        modifier = Modifier
            .background(bg)
            .padding(16.dp)
    ) {
        BatteryView(
            batteryList = fakeBattery,
            budsRes = R.drawable.airpods_pro_2_buds,
            caseRes = R.drawable.airpods_pro_2_case
        )
    }
}
