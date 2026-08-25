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

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import me.kavishdevar.librepods.BuildConfig
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.AppInfoCard
import me.kavishdevar.librepods.presentation.components.DeviceInfoCard
import me.kavishdevar.librepods.presentation.components.ListItemOrientation
import me.kavishdevar.librepods.presentation.components.StyledBottomSheet
import me.kavishdevar.librepods.presentation.components.StyledButton
import me.kavishdevar.librepods.presentation.components.StyledIconButton
import me.kavishdevar.librepods.presentation.components.StyledInputField
import me.kavishdevar.librepods.presentation.components.StyledList
import me.kavishdevar.librepods.presentation.components.StyledListItem
import me.kavishdevar.librepods.presentation.components.StyledSlider
import me.kavishdevar.librepods.presentation.components.StyledToggle
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.theme.MaterialTypography
import me.kavishdevar.librepods.presentation.overlays.ConnectionAlertStyle
import me.kavishdevar.librepods.presentation.overlays.LiveAlertSupport
import me.kavishdevar.librepods.services.AirPodsService
import me.kavishdevar.librepods.services.ServiceManager
import me.kavishdevar.librepods.presentation.viewmodel.AppSettingsViewModel
import me.kavishdevar.librepods.utils.XposedState
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    viewModel: AppSettingsViewModel = viewModel(),
    navigateToPurchase: () -> Unit,
    navigateToConnectionHealth: () -> Unit,
    navigateToTroubleshooting: () -> Unit,
    navigateToOpenSourceLicenses: () -> Unit,
    navigateToReleaseNotesScreen: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshLiveAlertAccess()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val backdrop = rememberLayerBackdrop()

    val contactBottomSheet = remember { mutableStateOf(false) }
    val connectionAlertPicker = remember { mutableStateOf(false) }
    val subjectState = remember { TextFieldState() }
    val descriptionState = remember { TextFieldState() }
    val subjectFocusRequester = remember { FocusRequester() }
    val descriptionFocusRequester = remember { FocusRequester() }

    val connectionAlertOptions = listOf(
        ConnectionAlertStyle.SYSTEM_LIVE_ALERT to Pair(
            stringResource(R.string.oneplus_live_alert),
            if (state.systemLiveAlertsSupported) {
                stringResource(R.string.oneplus_live_alert_description)
            } else {
                stringResource(R.string.oneplus_live_alert_unavailable)
            }
        ),
        ConnectionAlertStyle.CAMERA_CUTOUT to Pair(
            stringResource(R.string.camera_cutout_capsule),
            stringResource(R.string.camera_cutout_capsule_description)
        ),
        ConnectionAlertStyle.BOTTOM_SHEET to Pair(
            stringResource(R.string.bottom_connection_card),
            stringResource(R.string.bottom_connection_card_description)
        ),
        ConnectionAlertStyle.OFF to Pair(
            stringResource(R.string.connection_alerts_off),
            stringResource(R.string.connection_alerts_off_description)
        )
    )

    val m3eEnabled = LocalDesignSystem.current == DesignSystem.Material
    val topPadding = if (m3eEnabled) 16.dp else WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 84.dp
    val bottomPadding = if (m3eEnabled) 0.dp else WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp
    val isDarkTheme = isSystemInDarkTheme()
    val conversationalAwarenessVolume = state.conversationalAwarenessVolume
    val hasBluetoothPrivileged = remember(context) {
        context.checkSelfPermission("android.permission.BLUETOOTH_PRIVILEGED") ==
            PackageManager.PERMISSION_GRANTED
    }
    val isOnePlusOrOppo = remember {
        Build.MANUFACTURER.contains("OnePlus", ignoreCase = true) ||
            Build.MANUFACTURER.contains("OPPO", ignoreCase = true)
    }

    LaunchedEffect(state.connectionSuccessful, conversationalAwarenessVolume) {
        if (state.connectionSuccessful) {
            viewModel.setConversationalAwarenessVolume(conversationalAwarenessVolume)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(
                if (!m3eEnabled || contactBottomSheet.value) {
                    Modifier.layerBackdrop(backdrop)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding)
    ) {
        if (!state.isPremium && state.connectionSuccessful) {
            item(key = "premium") {
                Column {
                    StyledButton(
                        onClick = navigateToPurchase,
                        backdrop = backdrop,
                        modifier = Modifier.fillMaxWidth(),
                        maxScale = 0.05f,
                        surfaceColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            stringResource(R.string.unlock_advanced_features),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
        if (state.timeUntilFOSSPremiumExpiry > 0L) {
            item(key = "premium_expiry") {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF32829B), RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                        .clickable {
                            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:".toUri()
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("billing@kavish.xyz"))
                                putExtra(Intent.EXTRA_SUBJECT, "LibrePods Play billing error")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Please enter your GitHub username to restore your premium access:\n\nGitHub username: "
                                )
                            }
                            context.startActivity(emailIntent)
                        }
                ) {
                    Text(
                        text = stringResource(
                            R.string.play_foss_premium_banner,
                            maxOf(
                                1,
                                TimeUnit.MILLISECONDS.toDays(state.timeUntilFOSSPremiumExpiry)
                                    .toInt()
                            )
                        ),
                        modifier = Modifier.padding(16.dp),
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily(Font(R.font.sf_pro))
                        )
                    )
                }
            }
        }

        item(key = "vendor_conflict") {
            me.kavishdevar.librepods.presentation.components.VendorAacpConflictBanner()
        }

        item(key = "appearance") {
            StyledToggle(
                title = stringResource(R.string.appearance),
                label = stringResource(R.string.use_material3e),
                checked = state.m3eEnabled,
                onCheckedChange = viewModel::setm3eEnabled,
                enabled = state.isPremium
            )
        }

        if (state.connectionSuccessful) {
            item(key = "widget") {
                StyledToggle(
                    title = stringResource(R.string.widget),
                    label = stringResource(R.string.show_phone_battery_in_widget),
                    description = stringResource(R.string.show_phone_battery_in_widget_description),
                    checked = state.showPhoneBatteryInWidget,
                    onCheckedChange = viewModel::setShowPhoneBatteryInWidget,
                    enabled = state.isPremium
                )
            }

            item(key = "connection_alerts") {
                StyledList(
                    title = stringResource(R.string.connection_alerts),
                    description = stringResource(R.string.connection_alerts_description)
                ) {
                    val selectedAlert = connectionAlertOptions.firstOrNull {
                        it.first == state.connectionAlertStyle
                    }?.second ?: connectionAlertOptions.last().second
                    StyledListItem(
                        name = selectedAlert.first,
                        description = selectedAlert.second,
                        onClick = { connectionAlertPicker.value = true },
                        orientation = ListItemOrientation.Vertical
                    )

                    StyledListItem(
                        name = stringResource(R.string.preview_connection_alert),
                        description = stringResource(R.string.preview_connection_alert_description),
                        onClick = {
                            ServiceManager.getService()?.apply {
                                islandOpen = false
                                showConnectionAlert()
                            }
                        },
                        orientation = ListItemOrientation.Vertical
                    )

                    if (state.connectionAlertStyle == ConnectionAlertStyle.SYSTEM_LIVE_ALERT) {
                        StyledListItem(
                            name = stringResource(R.string.oneplus_live_alert_permission),
                            description = stringResource(
                                if (state.systemLiveAlertsAllowed) {
                                    R.string.oneplus_live_alert_permission_allowed
                                } else {
                                    R.string.oneplus_live_alert_permission_required
                                }
                            ),
                            onClick = if (!state.systemLiveAlertsAllowed) {
                                {
                                    try {
                                        context.startActivity(LiveAlertSupport.settingsIntent(context))
                                    } catch (_: Exception) {
                                        context.startActivity(
                                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                putExtra(
                                                    Settings.EXTRA_APP_PACKAGE,
                                                    context.packageName
                                                )
                                            }
                                        )
                                    }
                                }
                            } else null,
                            orientation = ListItemOrientation.Vertical
                        )
                    }

                    StyledListItem(
                        name = stringResource(R.string.background_service_notification),
                        description = stringResource(
                            if (state.backgroundServiceNoticeHidden) {
                                R.string.background_service_notification_hidden_description
                            } else {
                                R.string.background_service_notification_visible_description
                            }
                        ),
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    putExtra(
                                        Settings.EXTRA_CHANNEL_ID,
                                        AirPodsService.BACKGROUND_CHANNEL_ID
                                    )
                                }
                            )
                        },
                        orientation = ListItemOrientation.Vertical
                    )

                    if (state.connectionAlertStyle != ConnectionAlertStyle.SYSTEM_LIVE_ALERT &&
                        state.connectionAlertStyle != ConnectionAlertStyle.OFF
                    ) {
                        StyledToggle(
                            label = stringResource(R.string.standard_battery_notification),
                            description = stringResource(R.string.standard_battery_notification_description),
                            checked = state.showNotificationInShade,
                            onCheckedChange = viewModel::setShowNotificationInShade,
                        )
                    }
                }
            }

            item(key = "conversation_awareness") {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    StyledList(title = stringResource(R.string.conversational_awareness)) {
                        StyledToggle(
                            label = stringResource(R.string.conversational_awareness_pause_music),
                            description = stringResource(R.string.conversational_awareness_pause_music_description),
                            checked = state.conversationalAwarenessPauseMusicEnabled,
                            onCheckedChange = viewModel::setConversationalAwarenessPauseMusicEnabled,
                            enabled = state.isPremium
                        )

                        StyledToggle(
                            label = stringResource(R.string.relative_conversational_awareness_volume),
                            description = stringResource(R.string.relative_conversational_awareness_volume_description),
                            checked = state.relativeConversationalAwarenessVolumeEnabled,
                            onCheckedChange = viewModel::setRelativeConversationalAwarenessVolumeEnabled,
                            enabled = state.isPremium,
                        )
                    }
                }
            }

            item(key = "conversation_volume") {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    StyledSlider(
                        label = stringResource(R.string.conversational_awareness_volume),
                        value = conversationalAwarenessVolume,
                        valueRange = 10f..85f,
                        snapPoints = listOf(44f),
                        startLabel = "10%",
                        endLabel = "85%",
                        onValueChange = viewModel::setConversationalAwarenessVolume,
                        independent = true,
                        enabled = state.isPremium
                    )
                }
            }

            if (hasBluetoothPrivileged) {
                item(key = "ear_detection") {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        StyledToggle(
                            title = stringResource(R.string.ear_detection),
                            label = stringResource(R.string.disconnect_when_not_wearing),
                            description = stringResource(R.string.disconnect_when_not_wearing_description),
                            checked = state.disconnectWhenNotWearing,
                            onCheckedChange = viewModel::setDisconnectWhenNotWearing,
                            enabled = state.isPremium
                        )
                    }
                }
            }

            item(key = "takeover_airpods") {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    StyledList(title = stringResource(R.string.takeover_airpods_state)) {
                        StyledToggle(
                            label = stringResource(R.string.takeover_disconnected),
                            description = stringResource(R.string.takeover_disconnected_desc),
                            checked = state.takeoverWhenDisconnected,
                            onCheckedChange = viewModel::setTakeoverWhenDisconnected,
                            enabled = state.isPremium
                        )
                        StyledToggle(
                            label = stringResource(R.string.takeover_idle),
                            description = stringResource(R.string.takeover_idle_desc),
                            checked = state.takeoverWhenIdle,
                            onCheckedChange = viewModel::setTakeoverWhenIdle,
                            enabled = state.isPremium
                        )
                        StyledToggle(
                            label = stringResource(R.string.takeover_music),
                            description = stringResource(R.string.takeover_music_desc),
                            checked = state.takeoverWhenMusic,
                            onCheckedChange = viewModel::setTakeoverWhenMusic,
                            enabled = state.isPremium
                        )
                        StyledToggle(
                            label = stringResource(R.string.takeover_call),
                            description = stringResource(R.string.takeover_call_desc),
                            checked = state.takeoverWhenCall,
                            onCheckedChange = viewModel::setTakeoverWhenCall,
                            enabled = state.isPremium
                        )
                    }
                }
            }

            item(key = "takeover_phone") {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    StyledList(title = stringResource(R.string.takeover_phone_state)) {
                        StyledToggle(
                            label = stringResource(R.string.takeover_ringing_call),
                            description = stringResource(R.string.takeover_ringing_call_desc),
                            checked = state.takeoverWhenRingingCall,
                            onCheckedChange = viewModel::setTakeoverWhenRingingCall,
                            enabled = state.isPremium
                        )
                        StyledToggle(
                            label = stringResource(R.string.takeover_media_start),
                            description = stringResource(R.string.takeover_media_start_desc),
                            checked = state.takeoverWhenMediaStart,
                            onCheckedChange = viewModel::setTakeoverWhenMediaStart,
                            enabled = state.isPremium
                        )
                    }
                }
            }

            item(key = "advanced") {
                Column {
                    StyledToggle(
                        title = stringResource(R.string.advanced_options),
                        label = stringResource(R.string.use_alternate_head_tracking_packets),
                        description = stringResource(R.string.use_alternate_head_tracking_packets_description),
                        checked = state.useAlternateHeadTrackingPackets,
                        onCheckedChange = viewModel::setUseAlternateHeadTrackingPackets,
                        enabled = state.isPremium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else {
            item(key = "customizations_unavailable") {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.customizations_unavailable),
                        style = MaterialTypography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (XposedState.isAvailable && XposedState.bluetoothScopeEnabled) {
            item(key = "xposed") {
                val restartBluetoothText = stringResource(R.string.found_offset_restart_bluetooth)
                StyledToggle(
                    label = stringResource(R.string.act_as_an_apple_device) + " (${
                        stringResource(R.string.requires_xposed)
                    })",
                    description = stringResource(R.string.act_as_an_apple_device_description),
                    checked = state.vendorIdHook,
                    onCheckedChange = { enabled ->
                        Toast.makeText(context, restartBluetoothText, Toast.LENGTH_SHORT).show()
                        viewModel.setVendorIdHook(enabled)
                    }
                )
            }
        }

        item(key = "connection_support") {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                StyledList {
                    StyledListItem(
                        name = stringResource(R.string.connection_health),
                        description = stringResource(R.string.connection_health_settings_description),
                        onClick = navigateToConnectionHealth,
                        orientation = ListItemOrientation.Vertical,
                    )
                    if (!BuildConfig.PLAY_BUILD) {
                        StyledListItem(
                            name = stringResource(R.string.troubleshooting),
                            onClick = navigateToTroubleshooting,
                        )
                    }
                }
            }
        }

        item(key = "contact") {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                StyledList(title = stringResource(R.string.contact)) {
                    StyledListItem(
                        name = stringResource(R.string.email),
                        onClick = { contactBottomSheet.value = true },
                    )

                    StyledListItem(
                        name = stringResource(R.string.discord),
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "https://discord.gg/Ts4wupXcmc".toUri()
                                )
                            )
                        },
                    )

                    StyledListItem(
                        name = stringResource(R.string.github_issues),
                        onClick = {
                            val appVersion = Uri.encode(
                                "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                            )
                            val device = Uri.encode("${Build.MANUFACTURER} ${Build.MODEL}")
                            val androidVersion = Uri.encode("${Build.ID} (${Build.DISPLAY})")
                            val appSource = Uri.encode(
                                if (BuildConfig.PLAY_BUILD) "Play" else "GitHub"
                            )
                            val url = "https://github.com/kavishdevar/librepods/issues/new" +
                                "?template=01-bug-report-android.yml" +
                                "&app-source=$appSource" +
                                "&app-version=$appVersion" +
                                "&device=$device" +
                                "&android-version=$androidVersion"

                            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                        }
                    )
                }
            }
        }

        if (isOnePlusOrOppo) {
            item(key = "oneplus") {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    StyledList(title = stringResource(R.string.oneplus_setup)) {
                        StyledListItem(
                            name = stringResource(R.string.oneplus_background_access),
                            description = stringResource(R.string.oneplus_background_access_description),
                            onClick = {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    )
                                } catch (_: Exception) {
                                }
                            }
                        )
                    }
                }
            }
        }

        item(key = "device_info") {
            Column {
                Spacer(modifier = Modifier.height(20.dp))
                DeviceInfoCard()
            }
        }

        item(key = "app_info") {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                AppInfoCard(navigateToReleaseNotesScreen)
            }
        }

        item(key = "licenses") {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                StyledListItem(
                    name = stringResource(R.string.open_source_licenses),
                    onClick = navigateToOpenSourceLicenses,
                )
            }
        }
    }

    if (state.showCameraDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowCameraDialog(false) },
            title = {
                Text(
                    stringResource(R.string.set_custom_camera_package),
                    fontFamily = FontFamily(Font(R.font.sf_pro)),
                    fontWeight = FontWeight.Medium
                )
            },
            text = {
                Column {
                    Text(
                        stringResource(R.string.enter_custom_camera_package),
                        fontFamily = FontFamily(Font(R.font.sf_pro)),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = state.cameraPackageValue,
                        onValueChange = {
                            viewModel.setCameraPackageValue(it)
                            viewModel.setCameraPackageError(null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.cameraPackageError != null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            capitalization = KeyboardCapitalization.None
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDarkTheme) Color(0xFF007AFF) else Color(
                                0xFF3C6DF5
                            ),
                            unfocusedBorderColor = if (isDarkTheme) Color.Gray else Color.LightGray
                        ),
                        supportingText = {
                            if (state.cameraPackageError != null) {
                                Text(
                                    state.cameraPackageError ?: "",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        label = { Text(stringResource(R.string.custom_camera_package)) })
                }
            },
            confirmButton = {
                val successText = stringResource(R.string.custom_camera_package_set_success)
                TextButton(
                    onClick = {
                        viewModel.saveCameraPackage()
                        Toast.makeText(context, successText, Toast.LENGTH_SHORT).show()
                    }) {
                    Text(
                        "Save",
                        fontFamily = FontFamily(Font(R.font.sf_pro)),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.setShowCameraDialog(false) }) {
                    Text(
                        "Cancel",
                        fontFamily = FontFamily(Font(R.font.sf_pro)),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }

    if (connectionAlertPicker.value) {
        AlertDialog(
            onDismissRequest = { connectionAlertPicker.value = false },
            title = { Text(stringResource(R.string.connection_alerts)) },
            text = {
                Column(modifier = Modifier.selectableGroup()) {
                    connectionAlertOptions.forEach { (style, copy) ->
                        val enabled = style != ConnectionAlertStyle.SYSTEM_LIVE_ALERT ||
                            state.systemLiveAlertsSupported
                        val selected = style == state.connectionAlertStyle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .selectable(
                                    selected = selected,
                                    enabled = enabled,
                                    role = Role.RadioButton,
                                    onClick = {
                                        viewModel.setConnectionAlertStyle(style)
                                        connectionAlertPicker.value = false
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                enabled = enabled
                            )
                            Column(modifier = Modifier.padding(start = 10.dp)) {
                                Text(
                                    text = copy.first,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = copy.second,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { connectionAlertPicker.value = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    StyledBottomSheet(
        visible = contactBottomSheet.value,
        onDismiss = { contactBottomSheet.value = false },
        backdrop = backdrop
    ) { innerBackdrop, progress ->
        val animatedPadding = lerp(16.dp, 2.dp, progress)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = animatedPadding)
                .padding(bottom = 16.dp),
        ) {
           Row(
               modifier = Modifier
                   .fillMaxWidth()
                   .padding(bottom = 16.dp),
               horizontalArrangement = Arrangement.SpaceBetween,
               verticalAlignment = Alignment.CenterVertically
           ) {
               StyledIconButton(
                   icon = "\uDBC0\uDD84",
                   backdrop = innerBackdrop,
                   onClick = { contactBottomSheet.value = false }
               )
               Text (
                   text = stringResource(R.string.describe_your_issue),
                   style = TextStyle(
                       fontSize = 18.sp,
                       fontFamily = FontFamily(Font(R.font.sf_pro)),
                       fontWeight = FontWeight.Bold,
                       textAlign = TextAlign.Center,
                       color = if (isSystemInDarkTheme()) Color.White else Color.Black
                   )
               )
               StyledIconButton(
                   icon = "\uDBC0\uDE1F",
                   backdrop = innerBackdrop,
                   surfaceColor = if (isSystemInDarkTheme()) Color(0xFF0091FF) else Color(0xFF0088FF),
                   iconTint = if (subjectState.text.isNotEmpty() && descriptionState.text.isNotEmpty()) Color.White else Color.Gray,
                   enabled = subjectState.text.isNotEmpty() && descriptionState.text.isNotEmpty(),
                   onClick = {
                       contactBottomSheet.value = false
                       val intent = Intent(Intent.ACTION_SENDTO).apply {
                           data = "mailto:".toUri()
                           putExtra(Intent.EXTRA_EMAIL, arrayOf("contact@kavish.xyz"))
                           putExtra(Intent.EXTRA_SUBJECT, "LibrePods: ${subjectState.text}")
                           putExtra(
                               Intent.EXTRA_TEXT,
                               "${descriptionState.text}" +
                                   "\n\n----------" +
                                   "\nPhone details:" +
                                   "\nMANUFACTURER: ${Build.MANUFACTURER}" +
                                   "\nMODEL: ${Build.MODEL} (${Build.PRODUCT})" +
                                   "\nDISPLAY_VERSION: ${Build.DISPLAY}" +
                                   "\nID: ${Build.ID} (SDK ${Build.VERSION.SDK_INT_FULL})" +
                                   "\nXposed enabled/active: ${XposedState.isAvailable}/${XposedState.bluetoothScopeEnabled}" +
                                   "\n\nApp details:" +
                                   "\nVERSION: ${BuildConfig.VERSION_NAME}" +
                                   "\nVERSION_CODE: ${BuildConfig.VERSION_CODE}" +
                                   "\nFLAVOR: ${BuildConfig.FLAVOR}" +
                                   "\nBUILD_TYPE: ${BuildConfig.BUILD_TYPE}"
                           )
                       }
                       context.startActivity(intent)
                       subjectState.clearText()
                       descriptionState.clearText()
                   }
               )
           }

           Spacer(modifier = Modifier.height(8.dp))

           StyledInputField(
               inputState = subjectState,
               focusRequester = subjectFocusRequester,
               placeholder = stringResource(R.string.subject),
               forceApple = true
           )

           Spacer(modifier = Modifier.height(12.dp))

           StyledInputField(
               inputState = descriptionState,
               focusRequester = descriptionFocusRequester,
               placeholder = stringResource(R.string.describe_your_issue),
               singleLine = false,
               forceApple = true
           )
        }
    }
}
