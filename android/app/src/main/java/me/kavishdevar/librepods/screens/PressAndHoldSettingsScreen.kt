/*
 * LibrePods - AirPods liberated from Apple’s ecosystem
 *
 * Copyright (C) 2025 LibrePods contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

@file:OptIn(ExperimentalStdlibApi::class, ExperimentalEncodingApi::class)

package me.kavishdevar.librepods.screens

import android.content.Context
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.services.ServiceManager
import me.kavishdevar.librepods.utils.AACPManager
import kotlin.experimental.and
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable()
fun RightDivider() {
    HorizontalDivider(
        thickness = 1.5.dp,
        color = Color(0x40888888),
        modifier = Modifier
            .padding(start = 72.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LongPress(navController: NavController, name: String) {
    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) Color.White else Color.Black

    val modesByte = ServiceManager.getService()!!.aacpManager.controlCommandStatusList.find {
        it.identifier == AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE_CONFIGS
    }?.value?.takeIf { it.isNotEmpty() }?.get(0)

    if (modesByte != null) {
        Log.d("PressAndHoldSettingsScreen", "Current modes state: ${modesByte.toString(2)}")
        Log.d("PressAndHoldSettingsScreen", "Off mode: ${(modesByte and 0x01) != 0.toByte()}")
        Log.d("PressAndHoldSettingsScreen", "Transparency mode: ${(modesByte and 0x02) != 0.toByte()}")
        Log.d("PressAndHoldSettingsScreen", "Noise Cancellation mode: ${(modesByte and 0x04) != 0.toByte()}")
        Log.d("PressAndHoldSettingsScreen", "Adaptive mode: ${(modesByte and 0x08) != 0.toByte()}")
    }
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val deviceName = sharedPreferences.getString("name", "AirPods Pro")
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                        Text(
                            name,
                            fontFamily = FontFamily(Font(R.font.sf_pro)),
                        )
                    },
                navigationIcon = {
                    TextButton(
                        onClick = {
                            navController.popBackStack()
                        },
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = if (isDarkTheme)  Color(0xFF007AFF) else Color(0xFF3C6DF5),
                            modifier = Modifier.scale(1.5f)
                        )
                        Text(
                            deviceName?: "AirPods Pro",
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDarkTheme) Color(0xFF007AFF) else Color(0xFF3C6DF5),
                                fontFamily = FontFamily(Font(R.font.sf_pro))
                            ),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = if (isSystemInDarkTheme()) Color(0xFF000000)
        else Color(0xFFF2F2F7),
    ) { paddingValues ->
        val backgroundColor = if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
        Column (
          modifier = Modifier
              .fillMaxSize()
              .padding(paddingValues = paddingValues)
              .padding(horizontal = 16.dp)
              .padding(top = 8.dp)
        ) {
            Text(
                text = "NOISE CONTROL",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = textColor.copy(alpha = 0.6f),
                ),
                fontFamily = FontFamily(Font(R.font.sf_pro)),
                modifier = Modifier
                    .padding(8.dp, bottom = 4.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor, RoundedCornerShape(14.dp)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val offListeningModeValue = ServiceManager.getService()!!.aacpManager.controlCommandStatusList.find {
                    it.identifier == AACPManager.Companion.ControlCommandIdentifiers.ALLOW_OFF_OPTION
                }?.value?.takeIf { it.isNotEmpty() }?.get(0)
                val offListeningMode = offListeningModeValue == 1.toByte()
                LongPressElement(
                    name = "Off",
                    enabled = offListeningMode,
                    resourceId =  R.drawable.noise_cancellation,
                    isFirst = true)
                if (offListeningMode) RightDivider()
                LongPressElement(
                    name = "Transparency",
                    resourceId = R.drawable.transparency,
                    isFirst = !offListeningMode)
                RightDivider()
                LongPressElement(
                    name = "Adaptive",
                    resourceId = R.drawable.adaptive)
                RightDivider()
                LongPressElement(
                    name = "Noise Cancellation",
                    resourceId = R.drawable.noise_cancellation,
                    isLast = true)
            }
            Text(
                "Press and hold the stem to cycle between the selected noise control modes.",
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.sf_pro)),
                color = textColor.copy(alpha = 0.6f),
                modifier = Modifier
                    .padding(start = 16.dp, top = 4.dp)
            )
        }
    }
    Log.d("PressAndHoldSettingsScreen", "Current byte: ${ServiceManager.getService()!!.aacpManager.controlCommandStatusList.find {
        it.identifier == AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE_CONFIGS
    }?.value?.takeIf { it.isNotEmpty() }?.get(0)?.toString(2)}")
}

@Composable
fun LongPressElement(name: String, enabled: Boolean = true, resourceId: Int, isFirst: Boolean = false, isLast: Boolean = false) {
    val bit = when (name) {
        "Off" -> 0x01
        "Transparency" -> 0x02
        "Noise Cancellation" -> 0x04
        "Adaptive" -> 0x08
        else -> -1
    }
    val context = LocalContext.current

    val currentByteValue = ServiceManager.getService()!!.aacpManager.controlCommandStatusList.find {
        it.identifier == AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE_CONFIGS
    }?.value?.takeIf { it.isNotEmpty() }?.get(0)

    val savedByte = context.getSharedPreferences("settings", Context.MODE_PRIVATE).getInt("long_press_byte", 0b0101.toInt())
    val byteValue = currentByteValue ?: (savedByte and 0xFF).toByte()

    val isChecked = (byteValue.toInt() and bit) != 0
    val checked = remember { mutableStateOf(isChecked) }

    Log.d("PressAndHoldSettingsScreen", "LongPressElement: $name, checked: ${checked.value}, byteValue: ${byteValue.toInt()}, in bits: ${byteValue.toInt().toString(2)}")
    val darkMode = isSystemInDarkTheme()
    val textColor = if (darkMode) Color.White else Color.Black
    val desc = when (name) {
        "Off" -> "Turns off noise management"
        "Noise Cancellation" -> "Blocks out external sounds"
        "Transparency" -> "Lets in external sounds"
        "Adaptive" -> "Dynamically adjust external noise"
        else -> ""
    }

    fun countEnabledModes(byteValue: Int): Int {
        var count = 0
        if ((byteValue and 0x01) != 0) count++
        if ((byteValue and 0x02) != 0) count++
        if ((byteValue and 0x04) != 0) count++
        if ((byteValue and 0x08) != 0) count++

        Log.d("PressAndHoldSettingsScreen", "Byte: ${byteValue.toString(2)} Enabled modes: $count")
        return count
    }

    fun valueChanged(value: Boolean = !checked.value) {
        val latestByteValue = ServiceManager.getService()!!.aacpManager.controlCommandStatusList.find {
            it.identifier == AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE_CONFIGS
        }?.value?.takeIf { it.isNotEmpty() }?.get(0)

        val currentValue = (latestByteValue?.toInt() ?: byteValue.toInt()) and 0xFF

        Log.d("PressAndHoldSettingsScreen", "Current value: $currentValue (binary: ${Integer.toBinaryString(currentValue)}), bit: $bit, value: $value")

        if (!value) {
            val newValue = currentValue and bit.inv()

            Log.d("PressAndHoldSettingsScreen", "Bit to disable: $bit, inverted: ${bit.inv()}, after AND: ${Integer.toBinaryString(newValue)}")

            val modeCount = countEnabledModes(newValue)

            Log.d("PressAndHoldSettingsScreen", "After disabling, enabled modes count: $modeCount")

            if (modeCount < 2) {
                Log.d("PressAndHoldSettingsScreen", "Cannot disable $name mode - need at least 2 modes enabled")
                return
            }

            val updatedByte = newValue.toByte()

            Log.d("PressAndHoldSettingsScreen", "Sending updated byte: ${updatedByte.toInt() and 0xFF} (binary: ${Integer.toBinaryString(updatedByte.toInt() and 0xFF)})")

            ServiceManager.getService()!!.aacpManager.sendControlCommand(
                AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE_CONFIGS.value,
                updatedByte
            )

            context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
                .putInt("long_press_byte", newValue).apply()

            checked.value = false
            Log.d("PressAndHoldSettingsScreen", "Updated: $name, enabled: false, byte: ${updatedByte.toInt() and 0xFF}, bits: ${Integer.toBinaryString(updatedByte.toInt() and 0xFF)}")
        } else {
            val newValue = currentValue or bit
            val updatedByte = newValue.toByte()

            ServiceManager.getService()!!.aacpManager.sendControlCommand(
                AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE_CONFIGS.value,
                updatedByte
            )

            context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
                .putInt("long_press_byte", newValue).apply()

            checked.value = true
            Log.d("PressAndHoldSettingsScreen", "Updated: $name, enabled: true, byte: ${updatedByte.toInt() and 0xFF}, bits: ${newValue.toString(2)}")
        }
    }

    val shape = when {
        isFirst -> RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
        isLast -> RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)
        else -> RoundedCornerShape(0.dp)
    }
    var backgroundColor by remember { mutableStateOf(if (darkMode) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)) }
    val animatedBackgroundColor by animateColorAsState(targetValue = backgroundColor, animationSpec = tween(durationMillis = 500))
    if (!enabled) {
        valueChanged(false)
    } else {
        Row(
            modifier = Modifier
                .height(72.dp)
                .background(animatedBackgroundColor, shape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            backgroundColor = if (darkMode) Color(0x40888888) else Color(0x40D9D9D9)
                            tryAwaitRelease()
                            backgroundColor = if (darkMode) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
                            valueChanged()
                        },
                    )
                }
                .padding(horizontal = 16.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                bitmap = ImageBitmap.imageResource(resourceId),
                contentDescription = "Icon",
                tint = Color(0xFF007AFF),
                modifier = Modifier
                    .height(48.dp)
                    .wrapContentWidth()
            )
            Column (
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp)
                    .padding(start = 8.dp)
            )
            {
                Text(
                    name,
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.sf_pro)),
                )
                Text (
                    desc,
                    fontSize = 14.sp,
                    color = textColor.copy(alpha = 0.6f),
                    fontFamily = FontFamily(Font(R.font.sf_pro)),
                )
            }
            Checkbox(
                checked = checked.value,
                onCheckedChange = { valueChanged() },
                colors = CheckboxDefaults.colors().copy(
                    checkedCheckmarkColor = Color(0xFF007AFF),
                    uncheckedCheckmarkColor = Color.Transparent,
                    checkedBoxColor = Color.Transparent,
                    uncheckedBoxColor = Color.Transparent,
                    checkedBorderColor = Color.Transparent,
                    uncheckedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    disabledCheckedBoxColor = Color.Transparent,
                    disabledUncheckedBoxColor = Color.Transparent,
                    disabledUncheckedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .height(24.dp)
                    .scale(1.5f),
            )
        }
    }
}
