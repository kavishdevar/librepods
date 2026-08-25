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

package me.kavishdevar.librepods.services

//import me.kavishdevar.librepods.utils.CrossDevice
//import me.kavishdevar.librepods.utils.CrossDevicePackets
import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelUuid
import android.os.SystemClock
import android.os.UserHandle
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import me.kavishdevar.librepods.BuildConfig
import me.kavishdevar.librepods.MainActivity
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.battery.BatteryDataSource
import me.kavishdevar.librepods.battery.BatteryStateTracker
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.bluetooth.AACPManager.Companion.StemPressType
import me.kavishdevar.librepods.bluetooth.ATTHandles
import me.kavishdevar.librepods.bluetooth.ATTManagerv2
import me.kavishdevar.librepods.bluetooth.BLEManager
import me.kavishdevar.librepods.bluetooth.BluetoothConnectionManager
import me.kavishdevar.librepods.bluetooth.createBluetoothSocket
import me.kavishdevar.librepods.connection.AirPodsConnectionSnapshot
import me.kavishdevar.librepods.connection.AirPodsConnectionStateMachine
import me.kavishdevar.librepods.data.AirPodsInstance
import me.kavishdevar.librepods.data.AirPodsModels
import me.kavishdevar.librepods.data.AirPodsNotifications
import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryComponent
import me.kavishdevar.librepods.data.BatteryStatus
import me.kavishdevar.librepods.data.Capability
import me.kavishdevar.librepods.data.CustomEq
import me.kavishdevar.librepods.data.StemAction
import me.kavishdevar.librepods.data.XposedRemotePrefProvider
import me.kavishdevar.librepods.data.isHeadTrackingData
import me.kavishdevar.librepods.diagnostics.ConnectionDiagnosticsSnapshot
import me.kavishdevar.librepods.integration.SystemIntegrationController
import me.kavishdevar.librepods.presentation.overlays.IslandType
import me.kavishdevar.librepods.presentation.overlays.IslandWindow
import me.kavishdevar.librepods.presentation.overlays.PopupWindow
import me.kavishdevar.librepods.presentation.overlays.ConnectionAlertStyle
import me.kavishdevar.librepods.presentation.widgets.BatteryWidget
import me.kavishdevar.librepods.presentation.widgets.NoiseControlWidget
import me.kavishdevar.librepods.utils.GestureDetector
import me.kavishdevar.librepods.utils.HeadTracking
import me.kavishdevar.librepods.utils.MediaController
import me.kavishdevar.librepods.utils.BatteryDisplay
import me.kavishdevar.librepods.utils.BatteryDisplaySource
import me.kavishdevar.librepods.utils.BatteryLevels
import me.kavishdevar.librepods.utils.SystemApisUtils
import me.kavishdevar.librepods.utils.SystemApisUtils.DEVICE_TYPE_UNTETHERED_HEADSET
import me.kavishdevar.librepods.utils.SystemApisUtils.METADATA_COMPANION_APP
import me.kavishdevar.librepods.utils.SystemApisUtils.METADATA_DEVICE_TYPE
import me.kavishdevar.librepods.utils.SystemApisUtils.METADATA_MAIN_ICON
import me.kavishdevar.librepods.utils.SystemApisUtils.METADATA_MANUFACTURER_NAME
import me.kavishdevar.librepods.utils.SystemApisUtils.METADATA_MODEL_NAME
import me.kavishdevar.librepods.utils.SystemApisUtils.METADATA_UNTETHERED_CASE_BATTERY
import me.kavishdevar.librepods.utils.SystemApisUtils.METADATA_UNTETHERED_CASE_CHARGING
import me.kavishdevar.librepods.utils.SystemApisUtils.METADATA_UNTETHERED_CASE_ICON
import me.kavishdevar.librepods.utils.SystemApisUtils.METADATA_UNTETHERED_CASE_LOW_BATTERY_THRESHOLD
import me.kavishdevar.librepods.utils.SystemApisUtils.METADATA_UNTETHERED_LEFT_BATTERY
import me.kavishdevar.librepods.utils.SystemApisUtils.METADATA_UNTETHERED_LEFT_CHARGING
import me.kavishdevar.librepods.utils.SystemApisUtils.METADATA_UNTETHERED_LEFT_ICON
import me.kavishdevar.librepods.utils.SystemApisUtils.METADATA_UNTETHERED_LEFT_LOW_BATTERY_THRESHOLD
import me.kavishdevar.librepods.utils.SystemApisUtils.METADATA_UNTETHERED_RIGHT_BATTERY
import me.kavishdevar.librepods.utils.SystemApisUtils.METADATA_UNTETHERED_RIGHT_CHARGING
import me.kavishdevar.librepods.utils.SystemApisUtils.METADATA_UNTETHERED_RIGHT_ICON
import me.kavishdevar.librepods.utils.SystemApisUtils.METADATA_UNTETHERED_RIGHT_LOW_BATTERY_THRESHOLD
import java.text.DateFormat
import java.util.Date
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val TAG = "AirPodsService"
private const val PARSER_TAG = "AirPodsParser"
private const val AACP_CONNECT_TIMEOUT_MS = 5_000L
private const val ATT_CONNECT_TIMEOUT_MS = 3_000L

object ServiceManager {
    private var service: AirPodsService? = null

    @Synchronized
    fun getService(): AirPodsService? {
        return service
    }

    @Synchronized
    fun setService(service: AirPodsService?) {
        this.service = service
    }
}

// @Suppress("unused")
class AirPodsService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)
    private val mainScope = CoroutineScope(serviceJob + Dispatchers.Main.immediate)
    private val socketConnectionLock = Any()
    private val headTrackingCommandLock = Any()
    private val batteryStateTracker = BatteryStateTracker()

    private lateinit var connectionStateMachine: AirPodsConnectionStateMachine
    private lateinit var systemIntegrationController: SystemIntegrationController

    val connectionState: StateFlow<AirPodsConnectionSnapshot>
        get() = connectionStateMachine.snapshot

    @Volatile
    private var socketConnectionInProgress = false
    private var lastAutomaticSocketAttemptAt = 0L
    private var lastPublishedBattery: List<Battery>? = null
    @Volatile
    private var lastAacpBatteryPacketAt = 0L
    @Volatile
    private var lastHeadTrackingPacketAt = 0L

    var macAddress = ""
    @Volatile
    var localMac = ""
    lateinit var aacpManager: AACPManager
    lateinit var attManager: ATTManagerv2
    var airpodsInstance: AirPodsInstance? = null
    var cameraActive = false
    private var disconnectedBecauseReversed = false
    private var otherDeviceTookOver = false

    data class ServiceConfig(
        var deviceName: String = "AirPods",
        var earDetectionEnabled: Boolean = true,
        var conversationalAwarenessPauseMusic: Boolean = false,
        var showPhoneBatteryInWidget: Boolean = true,
        var relativeConversationalAwarenessVolume: Boolean = true,
        var headGestures: Boolean = true,
        var disconnectWhenNotWearing: Boolean = false,
        var conversationalAwarenessVolume: Int = 43,
        var qsClickBehavior: String = "cycle",
        var bleOnlyMode: Boolean = false,

        // AirPods state-based takeover
        var takeoverWhenDisconnected: Boolean = true,
        var takeoverWhenIdle: Boolean = true,
        var takeoverWhenMusic: Boolean = false,
        var takeoverWhenCall: Boolean = true,

        // Phone state-based takeover
        var takeoverWhenRingingCall: Boolean = true,
        var takeoverWhenMediaStart: Boolean = true,

        var leftSinglePressAction: StemAction = StemAction.defaultActions[StemPressType.SINGLE_PRESS]!!,
        var rightSinglePressAction: StemAction = StemAction.defaultActions[StemPressType.SINGLE_PRESS]!!,

        var leftDoublePressAction: StemAction = StemAction.defaultActions[StemPressType.DOUBLE_PRESS]!!,
        var rightDoublePressAction: StemAction = StemAction.defaultActions[StemPressType.DOUBLE_PRESS]!!,

        var leftTriplePressAction: StemAction = StemAction.defaultActions[StemPressType.TRIPLE_PRESS]!!,
        var rightTriplePressAction: StemAction = StemAction.defaultActions[StemPressType.TRIPLE_PRESS]!!,

        var leftLongPressAction: StemAction = StemAction.defaultActions[StemPressType.LONG_PRESS]!!,
        var rightLongPressAction: StemAction = StemAction.defaultActions[StemPressType.LONG_PRESS]!!,

        var cameraAction: StemPressType? = null,

        // AirPods device information
        var airpodsName: String = "",
        var airpodsModelNumber: String = "",
        var airpodsManufacturer: String = "",
        var airpodsSerialNumber: String = "",
        var airpodsLeftSerialNumber: String = "",
        var airpodsRightSerialNumber: String = "",
        var airpodsVersion1: String = "",
        var airpodsVersion2: String = "",
        var airpodsVersion3: String = "",
        var airpodsHardwareRevision: String = "",
        var airpodsUpdaterIdentifier: String = "",

        // phone's mac, needed for tipi
        var selfMacAddress: String = ""
    )

    private lateinit var config: ServiceConfig

    inner class LocalBinder : Binder() {
        fun getService(): AirPodsService = this@AirPodsService
    }

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var phoneStateListener: TelephonyCallback

    private var handleIncomingCallOnceConnected = false

    lateinit var bleManager: BLEManager

    companion object {
        private const val BACKGROUND_NOTIFICATION_ID = 1
        private const val CONNECTION_NOTIFICATION_ID = 2
        private const val SOCKET_FAILURE_NOTIFICATION_ID = 3
        private const val LIVE_ALERT_NOTIFICATION_ID = 4
        const val BACKGROUND_CHANNEL_ID = "background_service_status_hidden_v2"
        private const val LEGACY_BACKGROUND_CHANNEL_ID = "background_service_status"
        private const val CONNECTION_CHANNEL_ID = "airpods_connection_status"
        private const val LIVE_ALERT_CHANNEL_ID = "airpods_live_alert"
        private const val SOCKET_FAILURE_CHANNEL_ID = "airpods_connection_help"
        private const val LEGACY_SOCKET_FAILURE_CHANNEL_ID = "socket_connection_failure"
        private const val AUTOMATIC_SOCKET_RETRY_COOLDOWN_MS = 15_000L
        private const val AACP_BATTERY_FRESHNESS_MS = 60_000L
        private const val LIVE_ALERT_TIMEOUT_MS = 6_000L
        private const val ACTION_RECONNECT_AFTER_REVERSE =
            "me.kavishdevar.librepods.RECONNECT_AFTER_REVERSE"
        private const val ACTION_DISCONNECT = "me.kavishdevar.librepods.DISCONNECT"
        private const val ACTION_TAKE_BACK_AUDIO = "me.kavishdevar.librepods.TAKE_BACK_AUDIO"

        init {
            System.loadLibrary("bluetooth_socket")
        }
    }

    private val bleStatusListener = object : BLEManager.AirPodsStatusListener {
        @SuppressLint("NewApi")
        override fun onDeviceStatusChanged(
            device: BLEManager.AirPodsStatus, previousStatus: BLEManager.AirPodsStatus?
        ) {
            // BLE proximity advertisements describe the AirPods' wider ecosystem state.
            // "Disconnected" does not mean Android should open AACP; doing that here caused
            // a reconnect attempt (and notification) for every advertisement while the buds
            // were simply away. ACL/A2DP broadcasts are the source of truth for local connects.
            Log.v(
                TAG,
                "BLE state changed: ${previousStatus?.connectionState} -> ${device.connectionState}"
            )
        }

        override fun onBroadcastFromNewAddress(device: BLEManager.AirPodsStatus) {
            Log.d(TAG, "New address detected")
        }

        override fun onLidStateChanged(
            lidOpen: Boolean,
        ) {
            if (lidOpen) {
                Log.d(TAG, "Lid opened - displaying connection alert")
                val status = bleManager.getMostRecentStatus()
                val leftLevel = status?.leftBattery ?: 0
                val rightLevel = status?.rightBattery ?: 0
                val caseLevel = status?.caseBattery ?: 0
                val leftCharging = status?.isLeftCharging
                val rightCharging = status?.isRightCharging
                val caseCharging = status?.isCaseCharging

                if (BluetoothConnectionManager.aacpSocket?.isConnected != true) {
                    batteryNotification.setBatteryDirect(
                        leftLevel = leftLevel,
                        leftCharging = leftCharging == true,
                        rightLevel = rightLevel,
                        rightCharging = rightCharging == true,
                        caseLevel = caseLevel,
                        caseCharging = caseCharging == true
                    )
                }
                sendBatteryBroadcast()

                val visibleLevels = listOf(leftLevel, rightLevel)
                    .filter { BatteryLevels.isKnown(it) }
                val batteryPercentage = visibleLevels.minOrNull()
                    ?: caseLevel.takeIf { BatteryLevels.isKnown(it) }
                    ?: BatteryLevels.UNKNOWN_LEVEL
                showConnectionAlert(batteryPercentage)
            } else {
                Log.d(TAG, "Lid closed")
            }
        }

        override fun onEarStateChanged(
            device: BLEManager.AirPodsStatus, leftInEar: Boolean, rightInEar: Boolean
        ) {
            Log.d(TAG, "Ear state changed - Left: $leftInEar, Right: $rightInEar")

            // In BLE-only mode, ear detection is purely based on BLE data
            if (config.bleOnlyMode) {
                Log.d(TAG, "BLE-only mode: ear detection from BLE data")
            }
        }

        override fun onBatteryChanged(device: BLEManager.AirPodsStatus) {
            observeBleBattery(device)
            val aacpConnected = BluetoothConnectionManager.aacpSocket?.isConnected == true
            val aacpBatteryIsFresh = lastAacpBatteryPacketAt > 0L &&
                SystemClock.elapsedRealtime() - lastAacpBatteryPacketAt < AACP_BATTERY_FRESHNESS_MS
            if (aacpConnected && (!device.hasExactBatteryData || aacpBatteryIsFresh)) return
            batteryNotification.setBatteryDirect(
                leftLevel = device.leftBattery ?: BatteryLevels.UNKNOWN_LEVEL,
                leftCharging = device.isLeftCharging,
                rightLevel = device.rightBattery ?: BatteryLevels.UNKNOWN_LEVEL,
                rightCharging = device.isRightCharging,
                caseLevel = device.caseBattery ?: BatteryLevels.UNKNOWN_LEVEL,
                caseCharging = device.isCaseCharging
            )
            updateBattery()
            Log.d(TAG, "Battery changed")
        }

        override fun onBatteryObserved(device: BLEManager.AirPodsStatus) {
            observeBleBattery(device)
        }

        override fun onDeviceDisappeared() {
            Log.d(TAG, "All disappeared")
            // BLE advertisements commonly disappear after the lid closes even while the
            // A2DP/AACP connection remains healthy. ACL/AACP lifecycle events own the
            // connected state, so do not clear a valid connection here.
            if (BluetoothConnectionManager.aacpSocket?.isConnected != true) {
                updateNotificationContent(false)
            }
        }
    }

    private fun observeBleBattery(status: BLEManager.AirPodsStatus) {
        val podsSource = if (status.hasExactBatteryData) {
            BatteryDataSource.BLE_EXACT
        } else {
            BatteryDataSource.BLE_APPROXIMATE
        }
        val podsObservedAt = BatteryStateTracker.elapsedTimeForWallClock(status.lastSeen)
        val podBatteries = buildList {
            status.leftBattery?.let { level ->
                add(
                    Battery(
                        BatteryComponent.LEFT,
                        level,
                        if (status.isLeftCharging) BatteryStatus.CHARGING else BatteryStatus.NOT_CHARGING,
                    )
                )
            }
            status.rightBattery?.let { level ->
                add(
                    Battery(
                        BatteryComponent.RIGHT,
                        level,
                        if (status.isRightCharging) BatteryStatus.CHARGING else BatteryStatus.NOT_CHARGING,
                    )
                )
            }
        }
        if (podBatteries.isNotEmpty()) {
            batteryStateTracker.observe(
                batteries = podBatteries,
                source = podsSource,
                observedAtElapsedRealtime = podsObservedAt,
            )
        }

        val caseLevel = status.caseBattery ?: return
        val caseObservedAt = status.caseBatteryObservedAt ?: status.lastSeen
        batteryStateTracker.observe(
            batteries = listOf(
                Battery(
                    BatteryComponent.CASE,
                    caseLevel,
                    if (status.isCaseCharging) BatteryStatus.CHARGING else BatteryStatus.NOT_CHARGING,
                )
            ),
            source = if (status.caseBatteryIsCached) {
                BatteryDataSource.BLE_CACHED_CASE
            } else {
                podsSource
            },
            observedAtElapsedRealtime = BatteryStateTracker.elapsedTimeForWallClock(caseObservedAt),
        )
    }

    fun isBluetoothSocketExempted(): Boolean {
        return try {
            BluetoothSocket::class.java.declaredConstructors // will throw if still blocked
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    @SuppressLint("MissingPermission", "UnspecifiedRegisterReceiverFlag", "HardwareIds")
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "lib exempt worked: ${isBluetoothSocketExempted()}")

        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        initializeConfig()
        connectionStateMachine = AirPodsConnectionStateMachine(config.deviceName)
        systemIntegrationController = SystemIntegrationController(applicationContext)

        aacpManager = AACPManager()
        initializeAACPManagerCallback()

        attManager = ATTManagerv2()

        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        localMac = config.selfMacAddress
        if (localMac.isEmpty()) {
            if (checkSelfPermission("android.permission.LOCAL_MAC_ADDRESS") == PackageManager.PERMISSION_GRANTED) {
                val bluetoothManager = getSystemService(BluetoothManager::class.java)
                val bluetoothAdapter = bluetoothManager.adapter
                localMac = bluetoothAdapter.address
            }
            if (localMac.isNotEmpty()) {
                config.selfMacAddress = localMac
                sharedPreferences.edit {
                    putString("self_mac_address", localMac)
                }
            }
        }

        ServiceManager.setService(this)
        startForegroundNotification()
        me.kavishdevar.librepods.utils.OxygenOsKeepAlive.scheduleWatchdog(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            initGestureDetector()
        } else {
            gestureDetector = null
            config.headGestures = false
            sharedPreferences.edit { putBoolean("head_gestures", false) }
            Log.d(TAG, "Head gestures disabled as device is running Android 9 or below")
        }

        bleManager = BLEManager(this)
        bleManager.setAirPodsStatusListener(bleStatusListener)

        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)

        with(sharedPreferences) {
            edit {
                if (!contains("conversational_awareness_pause_music")) putBoolean(
                    "conversational_awareness_pause_music", false
                )
                if (!contains("personalized_volume")) putBoolean("personalized_volume", false)
                if (!contains("automatic_ear_detection")) putBoolean(
                    "automatic_ear_detection", true
                )
                if (!contains("long_press_nc")) putBoolean("long_press_nc", true)
                if (!contains("show_phone_battery_in_widget")) putBoolean(
                    "show_phone_battery_in_widget", true
                )
                if (!contains("single_anc")) putBoolean("single_anc", true)
                if (!contains("long_press_transparency")) putBoolean(
                    "long_press_transparency", true
                )
                if (!contains("conversational_awareness")) putBoolean(
                    "conversational_awareness", true
                )
                if (!contains("relative_conversational_awareness_volume")) putBoolean(
                    "relative_conversational_awareness_volume", true
                )
                if (!contains("long_press_adaptive")) putBoolean("long_press_adaptive", true)
                if (!contains("loud_sound_reduction")) putBoolean("loud_sound_reduction", true)
                if (!contains("long_press_off")) putBoolean("long_press_off", false)
                if (!contains("volume_control")) putBoolean("volume_control", true)
                if (!contains("head_gestures")) putBoolean("head_gestures", true)
                if (!contains("disconnect_when_not_wearing")) putBoolean(
                    "disconnect_when_not_wearing", false
                )

                // AirPods state-based takeover
                if (!contains("takeover_when_disconnected")) putBoolean(
                    "takeover_when_disconnected", false
                )
                if (!contains("takeover_when_idle")) putBoolean("takeover_when_idle", false)
                if (!contains("takeover_when_music")) putBoolean("takeover_when_music", false)
                if (!contains("takeover_when_call")) putBoolean("takeover_when_call", false)

                // Phone state-based takeover
                if (!contains("takeover_when_ringing_call")) putBoolean(
                    "takeover_when_ringing_call", false
                )
                if (!contains("takeover_when_media_start")) putBoolean(
                    "takeover_when_media_start", false
                )

                if (!contains("adaptive_strength")) putInt("adaptive_strength", 51)
                if (!contains("tone_volume")) putInt("tone_volume", 75)
                if (!contains("conversational_awareness_volume")) putInt(
                    "conversational_awareness_volume", 43
                )

                if (!contains("qs_click_behavior")) putString("qs_click_behavior", "cycle")
                if (!contains("name")) putString("name", "AirPods")

                if (!contains("left_single_press_action")) putString(
                    "left_single_press_action",
                    StemAction.defaultActions[StemPressType.SINGLE_PRESS]!!.name
                )
                if (!contains("right_single_press_action")) putString(
                    "right_single_press_action",
                    StemAction.defaultActions[StemPressType.SINGLE_PRESS]!!.name
                )
                if (!contains("left_double_press_action")) putString(
                    "left_double_press_action",
                    StemAction.defaultActions[StemPressType.DOUBLE_PRESS]!!.name
                )
                if (!contains("right_double_press_action")) putString(
                    "right_double_press_action",
                    StemAction.defaultActions[StemPressType.DOUBLE_PRESS]!!.name
                )
                if (!contains("left_triple_press_action")) putString(
                    "left_triple_press_action",
                    StemAction.defaultActions[StemPressType.TRIPLE_PRESS]!!.name
                )
                if (!contains("right_triple_press_action")) putString(
                    "right_triple_press_action",
                    StemAction.defaultActions[StemPressType.TRIPLE_PRESS]!!.name
                )
                if (!contains("left_long_press_action")) putString(
                    "left_long_press_action",
                    StemAction.defaultActions[StemPressType.LONG_PRESS]!!.name
                )
                if (!contains("right_long_press_action")) putString(
                    "right_long_press_action",
                    StemAction.defaultActions[StemPressType.LONG_PRESS]!!.name
                )
                if (!contains("camera_action")) putString("camera_action", "SINGLE_PRESS")

            }
        }

        initializeConfig()

        externalBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "me.kavishdevar.librepods.SET_ANC_MODE") {
                    if (intent.hasExtra("mode")) {
                        val mode = intent.getIntExtra("mode", -1)
                        if (mode in 1..4) {
                            aacpManager.sendControlCommand(
                                AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE.value,
                                mode
                            )
                        }
                    } else {
                        val currentMode = ancNotification.status
                        val configByte = sharedPreferences.getInt("long_press_byte", 0b0111)
                        val allowOffModeValue =
                            aacpManager.controlCommandStatusList.find { it.identifier == AACPManager.Companion.ControlCommandIdentifiers.ALLOW_OFF_OPTION }
                        val allowOffMode =
                            allowOffModeValue?.value?.takeIf { it.isNotEmpty() }?.get(0) == 0x01.toByte() || sharedPreferences.getBoolean("off_listening_mode", true)
                        val nextMode = getNextMode(currentMode = currentMode, configByte = configByte, allowOffMode)

                        aacpManager.sendControlCommand(
                            AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE.value,
                            nextMode
                        )
                        Log.d(
                            TAG,
                            "Cycling ANC mode from $currentMode to $nextMode"
                        )
                    }
                } else  if (intent?.action == "me.kavishdevar.librepods.CONVO_DETECT") {
                    if (intent.hasExtra("enabled")) {
                        val enabled = intent.getBooleanExtra("enabled", false)
                        aacpManager.sendControlCommand(
                            AACPManager.Companion.ControlCommandIdentifiers.CONVERSATION_DETECT_CONFIG.value,
                            enabled
                        )
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                externalBroadcastReceiver,
                externalBroadcastFilter,
                RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag") registerReceiver(
                externalBroadcastReceiver, externalBroadcastFilter
            )
        }
        val audioManager = this@AirPodsService.getSystemService(AUDIO_SERVICE) as AudioManager
        MediaController.initialize(
            audioManager, this@AirPodsService.getSharedPreferences(
                "settings", MODE_PRIVATE
            )
        )
//        Log.d(TAG, "Initializing CrossDevice")
//        CoroutineScope(Dispatchers.IO).launch {
//            CrossDevice.init(this@AirPodsService)
//            Log.d(TAG, "CrossDevice initialized")
//        }

        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        macAddress = sharedPreferences.getString("mac_address", "") ?: ""

        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        phoneStateListener = object: TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                when (state) {
                    TelephonyManager.CALL_STATE_RINGING -> {
                        val leAvailableForAudio =
                            bleManager.getMostRecentStatus()?.isLeftInEar == true || bleManager.getMostRecentStatus()?.isRightInEar == true
//                        if ((CrossDevice.isAvailable && !isConnectedLocally && earDetectionNotification.status.contains(0x00)) || leAvailableForAudio) CoroutineScope(Dispatchers.IO).launch {
                        if (leAvailableForAudio) serviceScope.launch {
                            takeOver("call")
                        }
                        if (config.headGestures) {
                            handleIncomingCall()
                        }
                    }

                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        val leAvailableForAudio =
                            bleManager.getMostRecentStatus()?.isLeftInEar == true || bleManager.getMostRecentStatus()?.isRightInEar == true
//                        if ((CrossDevice.isAvailable && !isConnectedLocally && earDetectionNotification.status.contains(0x00)) || leAvailableForAudio) CoroutineScope(
                        if (leAvailableForAudio) serviceScope.launch {
                            takeOver("call")
                        }
                        isInCall = true
                    }

                    TelephonyManager.CALL_STATE_IDLE -> {
                        isInCall = false
                        gestureDetector?.stopDetection()
                    }
                }
            }
        }
        if (checkSelfPermission("android.permission.READ_PHONE_STATE") == PackageManager.PERMISSION_GRANTED) {
            telephonyManager.registerTelephonyCallback(mainExecutor, phoneStateListener)
        }

        widgetMobileBatteryEnabled = config.showPhoneBatteryInWidget
        val batteryChangedIntentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                batteryChangedIntentReceiver, batteryChangedIntentFilter, RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag") registerReceiver(
                batteryChangedIntentReceiver, batteryChangedIntentFilter
            )
        }
        batteryReceiverRegistered = true
        val serviceIntentFilter = IntentFilter().apply {
            addAction("android.bluetooth.device.action.ACL_CONNECTED")
            addAction("android.bluetooth.device.action.ACL_DISCONNECTED")
            addAction("android.bluetooth.device.action.BOND_STATE_CHANGED")
            addAction("android.bluetooth.device.action.NAME_CHANGED")
            addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED")
            addAction("android.bluetooth.adapter.action.STATE_CHANGED")
            addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED")
            addAction("android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT")
            addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
            addAction("android.bluetooth.a2dp.profile.action.PLAYING_STATE_CHANGED")
            addAction("android.bluetooth.device.action.UUID")
        }

        connectionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AirPodsNotifications.AIRPODS_CONNECTION_DETECTED) {
                    val connectedDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra("device", BluetoothDevice::class.java)
                    } else {
                        intent.getParcelableExtra("device") as BluetoothDevice?
                    } ?: return
                    device = connectedDevice

                    if (config.deviceName == "AirPods" && connectedDevice.name != null) {
                        config.deviceName = connectedDevice.name ?: "AirPods"
                        sharedPreferences.edit { putString("name", config.deviceName) }
                    }
                    connectionStateMachine.detected(config.deviceName)

//                    Log.d("AirPodsCrossDevice", CrossDevice.isAvailable.toString())
//                    if (!CrossDevice.isAvailable) {
                    Log.d(TAG, "${config.deviceName} connected")
                    serviceScope.launch {
                        val bluetoothManager = getSystemService(BluetoothManager::class.java)
                        connectToSocket(bluetoothManager.adapter, connectedDevice)
                    }
                    Log.d(TAG, "Setting metadata")
                    setMetadatas(connectedDevice)
//                    isConnectedLocally = true
                    macAddress = connectedDevice.address
                    sharedPreferences.edit {
                        putString("mac_address", macAddress)
                    }
//                    }

                } else if (intent?.action == AirPodsNotifications.AIRPODS_DISCONNECTED) {
                    val controlOnly = intent.getBooleanExtra("control_only", false)
                    val disconnectReason = intent.getStringExtra("reason")
                    if (controlOnly) {
                        connectionStateMachine.recovering(config.deviceName, disconnectReason)
                    } else {
                        device = null
                        connectionStateMachine.disconnected(disconnectReason)
                        clearBatteryAfterDisconnect()
                    }
//                    isConnectedLocally = false
                    popupShown = false
                    updateNotificationContent(false)
                    aacpManager.disconnected()
                    runCatching { attManager.disconnected() }
                    runCatching { BluetoothConnectionManager.aacpSocket?.close() }
                    BluetoothConnectionManager.aacpSocket = null
                    BluetoothConnectionManager.attSocket = null
                }
            }
        }
        showIslandReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "me.kavishdevar.librepods.cross_device_island") {
                    showIsland(
                        this@AirPodsService,
                        minimumVisibleBattery(batteryNotification.getBattery())
                            ?: BatteryLevels.UNKNOWN_LEVEL
                    )
                } else if (intent?.action == AirPodsNotifications.DISCONNECT_RECEIVERS) {
                    try {
                        context?.unregisterReceiver(this)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        val showIslandIntentFilter = IntentFilter().apply {
            addAction("me.kavishdevar.librepods.cross_device_island")
            addAction(AirPodsNotifications.DISCONNECT_RECEIVERS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                showIslandReceiver,
                showIslandIntentFilter,
                RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag") registerReceiver(
                showIslandReceiver, showIslandIntentFilter
            )
        }

        val deviceIntentFilter = IntentFilter().apply {
            addAction(AirPodsNotifications.AIRPODS_CONNECTION_DETECTED)
            addAction(AirPodsNotifications.AIRPODS_DISCONNECTED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(connectionReceiver, deviceIntentFilter, RECEIVER_NOT_EXPORTED)
            registerReceiver(bluetoothReceiver, serviceIntentFilter, RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag") registerReceiver(
                connectionReceiver, deviceIntentFilter
            )
            registerReceiver(bluetoothReceiver, serviceIntentFilter)
        }

        val bluetoothAdapter = getSystemService(BluetoothManager::class.java).adapter

        bluetoothAdapter.getProfileProxy(
            this,
            object : BluetoothProfile.ServiceListener {
                @SuppressLint("NewApi")
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    try {
                        if (profile != BluetoothProfile.A2DP) return
                        val savedMac = sharedPreferences.getString("mac_address", "").orEmpty()
                        val aacpUuid = ParcelUuid.fromString(
                            "74ec2172-0bad-4d01-8f77-997b2be0722a"
                        )
                        val connectedAirPods = proxy.connectedDevices.firstOrNull { candidate ->
                            candidate.address == savedMac ||
                                candidate.uuids?.contains(aacpUuid) == true ||
                                candidate.name?.contains("AirPods", ignoreCase = true) == true
                        }

                        if (connectedAirPods != null) {
                            device = connectedAirPods
                            macAddress = connectedAirPods.address
                            sharedPreferences.edit { putString("mac_address", macAddress) }
                            val connectedName = connectedAirPods.name
                                ?.takeIf { it.isNotBlank() }
                                ?: config.deviceName
                            connectionStateMachine.detected(connectedName)
                            setMetadatas(connectedAirPods)
                            serviceScope.launch {
                                connectToSocket(bluetoothAdapter, connectedAirPods)
                            }
                        }
                    } finally {
                        bluetoothAdapter.closeProfileProxy(profile, proxy)
                    }
                }

                override fun onServiceDisconnected(profile: Int) = Unit
            },
            BluetoothProfile.A2DP
        )

//        if (!isConnectedLocally && !CrossDevice.isAvailable) {
//            clearPacketLogs()
//        }

        serviceScope.launch {
            bleManager.startScanning()
        }
    }

    @Suppress("unused")
    fun cameraOpened() {
        Log.d(TAG, "Camera opened, gonna handle stem presses and take action if visible")
        cameraActive = true
        setupStemActions()
    }

    @Suppress("unused")
    fun cameraClosed() {
        cameraActive = false
        setupStemActions()
    }

    fun isCustomAction(
        action: StemAction?, default: StemAction?
    ): Boolean {
        return action != default
    }

    fun setupStemActions() {
        val singlePressDefault = StemAction.defaultActions[StemPressType.SINGLE_PRESS]
        val doublePressDefault = StemAction.defaultActions[StemPressType.DOUBLE_PRESS]
        val triplePressDefault = StemAction.defaultActions[StemPressType.TRIPLE_PRESS]
        val longPressDefault = StemAction.defaultActions[StemPressType.LONG_PRESS]

        val singlePressCustomized =
            isCustomAction(config.leftSinglePressAction, singlePressDefault) || isCustomAction(
                config.rightSinglePressAction, singlePressDefault
            ) || (cameraActive && config.cameraAction == StemPressType.SINGLE_PRESS)
        val doublePressCustomized =
            isCustomAction(config.leftDoublePressAction, doublePressDefault) || isCustomAction(
                config.rightDoublePressAction, doublePressDefault
            )
        val triplePressCustomized =
            isCustomAction(config.leftTriplePressAction, triplePressDefault) || isCustomAction(
                config.rightTriplePressAction, triplePressDefault
            )
        val longPressCustomized = isCustomAction(
            config.leftLongPressAction, longPressDefault
        ) || isCustomAction(
            config.rightLongPressAction, longPressDefault
        ) || (cameraActive && config.cameraAction == StemPressType.LONG_PRESS)
        Log.d(
            TAG,
            "Setting up stem actions: Single Press Customized: $singlePressCustomized, Double Press Customized: $doublePressCustomized, Triple Press Customized: $triplePressCustomized, Long Press Customized: $longPressCustomized"
        )
        aacpManager.sendStemConfigPacket(
            singlePressCustomized,
            doublePressCustomized,
            triplePressCustomized,
            longPressCustomized,
        )
    }

    fun replayAllConfigurations() {
        Log.d(TAG, "Replaying all configurations to AirPods on connect/reconnect")
        setupStemActions()

        // 1. Replay listening mode configs (long press mode bitmask)
        val longPressByte = sharedPreferences.getInt("long_press_byte", -1)
        if (longPressByte != -1) {
            Log.d(TAG, "Replaying LISTENING_MODE_CONFIGS: $longPressByte")
            aacpManager.sendControlCommand(
                AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE_CONFIGS.value,
                longPressByte.toByte()
            )
        }

        // 2. Replay allow off listening mode
        if (sharedPreferences.contains("off_listening_mode")) {
            val offMode = sharedPreferences.getBoolean("off_listening_mode", true)
            Log.d(TAG, "Replaying ALLOW_OFF_OPTION: $offMode")
            aacpManager.sendControlCommand(
                AACPManager.Companion.ControlCommandIdentifiers.ALLOW_OFF_OPTION.value,
                offMode
            )
        }

        // 3. Replay automatic ear detection
        if (sharedPreferences.contains("automatic_ear_detection")) {
            val earDetect = sharedPreferences.getBoolean("automatic_ear_detection", true)
            Log.d(TAG, "Replaying EAR_DETECTION_CONFIG: $earDetect")
            aacpManager.sendControlCommand(
                AACPManager.Companion.ControlCommandIdentifiers.EAR_DETECTION_CONFIG.value,
                earDetect
            )
        }

        // 4. Replay automatic connection config
        if (sharedPreferences.contains("automatic_connection_ctrl_cmd")) {
            val autoConnect = sharedPreferences.getBoolean("automatic_connection_ctrl_cmd", true)
            Log.d(TAG, "Replaying AUTOMATIC_CONNECTION_CONFIG: $autoConnect")
            aacpManager.sendControlCommand(
                AACPManager.Companion.ControlCommandIdentifiers.AUTOMATIC_CONNECTION_CONFIG.value,
                autoConnect
            )
        }

        // 5. Replay dynamic end of charge / optimized battery charging
        if (sharedPreferences.contains("dynamic_end_of_charge")) {
            val dynamicCharge = sharedPreferences.getBoolean("dynamic_end_of_charge", false)
            Log.d(TAG, "Replaying DYNAMIC_END_OF_CHARGE: $dynamicCharge")
            aacpManager.sendControlCommand(
                AACPManager.Companion.ControlCommandIdentifiers.DYNAMIC_END_OF_CHARGE.value,
                dynamicCharge
            )
        }

        // 6. Replay adaptive volume if saved
        if (sharedPreferences.contains("adaptive_volume")) {
            val adaptiveVolume = sharedPreferences.getBoolean("adaptive_volume", false)
            Log.d(TAG, "Replaying ADAPTIVE_VOLUME_CONFIG: $adaptiveVolume")
            aacpManager.sendControlCommand(
                AACPManager.Companion.ControlCommandIdentifiers.ADAPTIVE_VOLUME_CONFIG.value,
                adaptiveVolume
            )
        }

        // 7. Replay conversation awareness if saved
        if (sharedPreferences.contains("conversation_detect")) {
            val convoDetect = sharedPreferences.getBoolean("conversation_detect", false)
            Log.d(TAG, "Replaying CONVERSATION_DETECT_CONFIG: $convoDetect")
            aacpManager.sendControlCommand(
                AACPManager.Companion.ControlCommandIdentifiers.CONVERSATION_DETECT_CONFIG.value,
                convoDetect
            )
        }

        // 8. Replay sleep detection if saved
        if (sharedPreferences.contains("sleep_detection")) {
            val sleepDetect = sharedPreferences.getBoolean("sleep_detection", false)
            Log.d(TAG, "Replaying SLEEP_DETECTION_CONFIG: $sleepDetect")
            aacpManager.sendControlCommand(
                AACPManager.Companion.ControlCommandIdentifiers.SLEEP_DETECTION_CONFIG.value,
                sleepDetect
            )
        }

        // 9. Replay click hold mode (Siri vs Noise Control per bud)
        val leftClickHold = sharedPreferences.getInt("left_click_hold_mode", -1)
        val rightClickHold = sharedPreferences.getInt("right_click_hold_mode", -1)
        if (leftClickHold != -1 && rightClickHold != -1) {
            Log.d(TAG, "Replaying CLICK_HOLD_MODE: right=$rightClickHold, left=$leftClickHold")
            aacpManager.sendControlCommand(
                AACPManager.Companion.ControlCommandIdentifiers.CLICK_HOLD_MODE.value,
                byteArrayOf(rightClickHold.toByte(), leftClickHold.toByte())
            )
        }

        // 10. Replay mic mode if saved
        val micMode = sharedPreferences.getInt("mic_mode", -1)
        if (micMode != -1) {
            Log.d(TAG, "Replaying MIC_MODE: $micMode")
            aacpManager.sendControlCommand(
                AACPManager.Companion.ControlCommandIdentifiers.MIC_MODE.value,
                micMode.toByte()
            )
        }
    }

    @ExperimentalEncodingApi
    private fun initializeAACPManagerCallback() {
        aacpManager.setPacketCallback(object : AACPManager.PacketCallback {
            @SuppressLint("MissingPermission")
            override fun onBatteryInfoReceived(batteryInfo: ByteArray) {
                val updatedComponents = batteryNotification.componentsInPacket(batteryInfo)
                if (!batteryNotification.setBattery(batteryInfo)) {
                    Log.w(PARSER_TAG, "Ignoring malformed battery packet (${batteryInfo.size} bytes)")
                    return
                }
                lastAacpBatteryPacketAt = SystemClock.elapsedRealtime()
                batteryStateTracker.observe(
                    batteries = batteryNotification.getBattery(),
                    source = BatteryDataSource.AACP,
                    components = updatedComponents,
                    observedAtElapsedRealtime = lastAacpBatteryPacketAt,
                )
                updateBattery()
//                CrossDevice.sendRemotePacket(batteryInfo)
//                CrossDevice.batteryBytes = batteryInfo

                val batteries = batteryNotification.getBattery()
                if (Log.isLoggable(PARSER_TAG, Log.DEBUG)) {
                    for (battery in batteries) {
                        Log.d(
                            PARSER_TAG,
                            "${battery.getComponentName()}: ${battery.getStatusName()} at ${battery.level}% "
                        )
                    }
                }

                if (batteries.getOrNull(0)?.status == BatteryStatus.CHARGING &&
                    batteries.getOrNull(1)?.status == BatteryStatus.CHARGING
                ) {
                    disconnectAudio(this@AirPodsService, device)
                } else {
                    connectAudio(this@AirPodsService, device)
                }
            }

            override fun onEarDetectionReceived(earDetection: ByteArray) {
                sendBroadcast(Intent(AirPodsNotifications.EAR_DETECTION_DATA).apply {
                    val list = earDetectionNotification.status
                    val bytes = ByteArray(2)
                    bytes[0] = list[0]
                    bytes[1] = list[1]
                    putExtra("data", bytes)
                }.apply {
                    setPackage(packageName)
                })
                Log.d(
                    "AirPodsParser",
                    "Ear Detection: ${earDetectionNotification.status[0]} ${earDetectionNotification.status[1]}"
                )
                processEarDetectionChange(earDetection)
            }

            override fun onConversationAwarenessReceived(conversationAwareness: ByteArray) {
                conversationAwarenessNotification.setData(conversationAwareness)
                sendBroadcast(Intent(AirPodsNotifications.CA_DATA).apply {
                    putExtra("data", conversationAwarenessNotification.status)
                }.apply {
                    setPackage(packageName)
                })

                if (conversationAwarenessNotification.status == 1.toByte() || conversationAwarenessNotification.status == 2.toByte()) {
                    MediaController.startSpeaking()
                } else if (conversationAwarenessNotification.status == 6.toByte() ||conversationAwarenessNotification.status == 8.toByte() || conversationAwarenessNotification.status == 9.toByte()) {
                    MediaController.stopSpeaking()
                }

                Log.d(
                    "AirPodsParser",
                    "Conversation Awareness: ${conversationAwarenessNotification.status}"
                )
            }

            override fun onControlCommandReceived(controlCommand: ByteArray) {
                val command = AACPManager.ControlCommand.fromByteArray(controlCommand)
                if (command.identifier == AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE.value) {
                    ancNotification.setStatus(byteArrayOf(command.value.takeIf { it.isNotEmpty() }
                        ?.get(0) ?: 0x00.toByte()))
                    sendANCBroadcast()
                    updateNoiseControlWidget()
                }
            }

            override fun onOwnershipChangeReceived(owns: Boolean) {
                if (!owns) {
                    MediaController.recentlyLostOwnership = true
                    Handler(Looper.getMainLooper()).postDelayed({
                        MediaController.recentlyLostOwnership = false
                    }, 3000)
                    Log.d(TAG, "ownership lost")
                    MediaController.sendPause()
                    MediaController.pausedForOtherDevice = true
                    otherDeviceTookOver = true
                    disconnectAudio(
                        this@AirPodsService, device
                    )
                }
            }

            override fun onOwnershipToFalseRequest(sender: String, reasonReverseTapped: Boolean) {
                // TODO: Show a reverse button, but that's a lot of effort -- i'd have to change the UI too, which i hate doing, and handle other device's reverses too, and disconnect audio etc... so for now, just pause the audio and show the island without asking to reverse.
                // handling reverse is a problem because we'd have to disconnect the audio, but there's no option connect audio again natively, so notification would have to be changed. I wish there was a way to just "change the audio output device".
                // (20 minutes later) i've done it nonetheless :]
                val senderName =
                    aacpManager.connectedDevices.find { it.mac == sender }?.type ?: "Other device"
                Log.d(
                    TAG,
                    "other device has hijacked the connection, reasonReverseTapped: $reasonReverseTapped"
                )
                aacpManager.sendControlCommand(
                    AACPManager.Companion.ControlCommandIdentifiers.OWNS_CONNECTION.value,
                    byteArrayOf(0x00)
                )
                otherDeviceTookOver = true
                disconnectAudio(
                    this@AirPodsService, device
                )
                if (reasonReverseTapped) {
                    Log.d(TAG, "reverse tapped, disconnecting audio")
                    disconnectedBecauseReversed = true
                    disconnectAudio(this@AirPodsService, device)
                    showIsland(
                        this@AirPodsService,
                        (batteryNotification.getBattery()
                            .find { it.component == BatteryComponent.LEFT }?.level
                            ?: 0).coerceAtMost(
                            batteryNotification.getBattery()
                                .find { it.component == BatteryComponent.RIGHT }?.level ?: 0
                        ),
                        IslandType.MOVED_TO_OTHER_DEVICE,
                        reversed = true,
                        otherDeviceName = senderName
                    )
                }
                if (!aacpManager.owns) {
                    showIsland(
                        this@AirPodsService,
                        (batteryNotification.getBattery()
                            .find { it.component == BatteryComponent.LEFT }?.level
                            ?: 0).coerceAtMost(
                            batteryNotification.getBattery()
                                .find { it.component == BatteryComponent.RIGHT }?.level ?: 0
                        ),
                        IslandType.MOVED_TO_OTHER_DEVICE,
                        reversed = reasonReverseTapped,
                        otherDeviceName = senderName
                    )
                }
                MediaController.sendPause()
            }

            override fun onShowNearbyUI(sender: String) {
                val senderName =
                    aacpManager.connectedDevices.find { it.mac == sender }?.type ?: "Other device"
                showIsland(
                    this@AirPodsService,
                    (batteryNotification.getBattery()
                        .find { it.component == BatteryComponent.LEFT }?.level ?: 0).coerceAtMost(
                        batteryNotification.getBattery()
                            .find { it.component == BatteryComponent.RIGHT }?.level ?: 0
                    ),
                    IslandType.MOVED_TO_OTHER_DEVICE,
                    reversed = false,
                    otherDeviceName = senderName
                )
            }

            override fun onDeviceInformationReceived(deviceInformation: AACPManager.Companion.AirPodsInformation) {
                Log.d(
                    "AirPodsParser",
                    "Device Information: name: ${deviceInformation.name}, modelNumber: ${deviceInformation.modelNumber}, manufacturer: ${deviceInformation.manufacturer}, serialNumber: ${deviceInformation.serialNumber}, version1: ${deviceInformation.version1}, version2: ${deviceInformation.version2}, hardwareRevision: ${deviceInformation.hardwareRevision}, updaterIdentifier: ${deviceInformation.updaterIdentifier}, leftSerialNumber: ${deviceInformation.leftSerialNumber}, rightSerialNumber: ${deviceInformation.rightSerialNumber}, version3: ${deviceInformation.version3}"
                )
                // Store in SharedPreferences
                sharedPreferences.edit {
                    putString("name", deviceInformation.name)
                    putString("airpods_model_number", deviceInformation.modelNumber)
                    putString("airpods_manufacturer", deviceInformation.manufacturer)
                    putString("airpods_serial_number", deviceInformation.serialNumber)
                    putString("airpods_left_serial_number", deviceInformation.leftSerialNumber)
                    putString("airpods_right_serial_number", deviceInformation.rightSerialNumber)
                    putString("airpods_version1", deviceInformation.version1)
                    putString("airpods_version2", deviceInformation.version2)
                    putString("airpods_version3", deviceInformation.version3)
                    putString("airpods_hardware_revision", deviceInformation.hardwareRevision)
                    putString("airpods_updater_identifier", deviceInformation.updaterIdentifier)
                }
                // Update config
                config.airpodsName = deviceInformation.name
                config.airpodsModelNumber = deviceInformation.modelNumber
                config.airpodsManufacturer = deviceInformation.manufacturer
                config.airpodsSerialNumber = deviceInformation.serialNumber
                config.airpodsLeftSerialNumber = deviceInformation.leftSerialNumber
                config.airpodsRightSerialNumber = deviceInformation.rightSerialNumber
                config.airpodsVersion1 = deviceInformation.version1
                config.airpodsVersion2 = deviceInformation.version2
                config.airpodsVersion3 = deviceInformation.version3
                config.airpodsHardwareRevision = deviceInformation.hardwareRevision
                config.airpodsUpdaterIdentifier = deviceInformation.updaterIdentifier

                val model = AirPodsModels.getModelByModelNumber(config.airpodsModelNumber)
                if (model != null) {
                    airpodsInstance = AirPodsInstance(
                        name = config.airpodsName,
                        model = model,
                        actualModelNumber = config.airpodsModelNumber,
                        serialNumber = config.airpodsSerialNumber,
                        leftSerialNumber = config.airpodsLeftSerialNumber,
                        rightSerialNumber = config.airpodsRightSerialNumber,
                        version1 = config.airpodsVersion1,
                        version2 = config.airpodsVersion2,
                        version3 = config.airpodsVersion3,
                    )
                    if (device != null) setMetadatas(device!!)
                }
                sendBroadcast(
                    Intent(AirPodsNotifications.AIRPODS_INFORMATION_UPDATED).setPackage(
                        packageName
                    )
                )
            }

            @SuppressLint("NewApi")
            override fun onHeadTrackingReceived(headTracking: ByteArray) {
                lastHeadTrackingPacketAt = SystemClock.elapsedRealtime()
                if (isHeadTrackingActive) {
                    HeadTracking.processPacket(headTracking)
                    processHeadTrackingData(headTracking)
                }
            }

            override fun onProximityKeysReceived(proximityKeys: ByteArray) {
                val keys = aacpManager.parseProximityKeysResponse(proximityKeys)
                Log.d("AirPodsParser", "Proximity keys: $keys")
                sharedPreferences.edit {
                    for (key in keys) {
                        Log.d("AirPodsParser", "Proximity key: ${key.key.name} = ${key.value}")
                        putString(key.key.name, Base64.encode(key.value))
                    }
                }
            }

            override fun onStemPressReceived(stemPress: ByteArray) {

                val (stemPressType, bud) = aacpManager.parseStemPressResponse(stemPress)

                Log.d(
                    "AirPodsParser",
                    "Stem press received: $stemPressType on $bud, cameraActive: $cameraActive, cameraAction: ${config.cameraAction}"
                )
                if (cameraActive && config.cameraAction != null && stemPressType == config.cameraAction) {
                        Runtime.getRuntime().exec(arrayOf("su", "-c", "input keyevent 27"))
                } else {
                    val action = getActionFor(bud, stemPressType)
                    Log.d("AirPodsParser", "$bud $stemPressType action: $action")
                    action?.let { executeStemAction(it) }
                }
            }

            override fun onAudioSourceReceived(audioSource: ByteArray) {
                Log.d(
                    "AirPodsParser",
                    "Audio source changed mac: ${aacpManager.audioSource?.mac}, type: ${aacpManager.audioSource?.type?.name}"
                )
                if (localMac!="" && (aacpManager.audioSource?.type != AACPManager.Companion.AudioSourceType.NONE && aacpManager.audioSource?.mac != localMac)) {
                    Log.d(
                        "AirPodsParser",
                        "Audio source is another device, better to give up aacp control"
                    )
                    aacpManager.sendControlCommand(
                        AACPManager.Companion.ControlCommandIdentifiers.OWNS_CONNECTION.value,
                        byteArrayOf(0x00)
                    )
                    // this also means that the other device has start playing the audio, and if that's true, we can again start listening for audio config changes
//                    Log.d(TAG, "Another device started playing audio, listening for audio config changes again")
//                    MediaController.pausedForOtherDevice = false
// future me: what the heck is this? this just means it will not be taking over again if audio source doesn't change???
                }
            }

            override fun onConnectedDevicesReceived(connectedDevices: List<AACPManager.Companion.ConnectedDevice>) {
                for (device in connectedDevices) {
                    Log.d(
                        "AirPodsParser",
                        "Connected device: ${device.mac}, info1: ${device.info1}, info2: ${device.info2})"
                    )
                }
                // Smart-routing packets require the phone's real Bluetooth MAC. Android only
                // exposes it to privileged apps, so skip this optional path instead of probing
                // root or constructing packets that cannot be valid on a stock phone.
                if (localMac.isBlank()) return

                val newDevices = connectedDevices.filter { newDevice ->
                    val notInOld =
                        aacpManager.oldConnectedDevices.none { oldDevice -> oldDevice.mac == newDevice.mac }
                    val notLocal = newDevice.mac != localMac
                    notInOld && notLocal
                }

                for (device in newDevices) {
                    Log.d(
                        "AirPodsParser",
                        "New connected device: ${device.mac}, info1: ${device.info1}, info2: ${device.info2})"
                    )
                    Log.d(
                        TAG,
                        "Sending new Tipi packet for device ${device.mac}, and sending media info to the device"
                    )
                    aacpManager.sendMediaInformationNewDevice(
                        selfMacAddress = localMac, targetMacAddress = device.mac
                    )
                    aacpManager.sendAddTiPiDevice(
                        selfMacAddress = localMac, targetMacAddress = device.mac
                    )
                }
            }

            override fun onHeadphoneAccommodationReceived(eqData: FloatArray) {
                sendBroadcast(
                    Intent(AirPodsNotifications.EQ_DATA).putExtra("eqData", eqData).apply {
                        setPackage(packageName)
                    })
            }

            override fun onCustomEqReceived(customEq: CustomEq) {
                // TODO
            }

            override fun onCapabilitiesReceived(capabilities: List<Capability>) {
                // TODO
            }

            override fun onUnknownPacketReceived(packet: ByteArray) {
                Log.d(
                    "AACPManager",
                    "Unknown packet received: ${packet.joinToString(" ") { "%02X".format(it) }}"
                )
            }
        })
    }

    private fun getActionFor(
        bud: AACPManager.Companion.StemPressBudType, type: StemPressType
    ): StemAction? {
        return when (type) {
            StemPressType.SINGLE_PRESS -> if (bud == AACPManager.Companion.StemPressBudType.LEFT) config.leftSinglePressAction else config.rightSinglePressAction
            StemPressType.DOUBLE_PRESS -> if (bud == AACPManager.Companion.StemPressBudType.LEFT) config.leftDoublePressAction else config.rightDoublePressAction
            StemPressType.TRIPLE_PRESS -> if (bud == AACPManager.Companion.StemPressBudType.LEFT) config.leftTriplePressAction else config.rightTriplePressAction
            StemPressType.LONG_PRESS -> if (bud == AACPManager.Companion.StemPressBudType.LEFT) config.leftLongPressAction else config.rightLongPressAction
        }
    }

    private fun executeStemAction(action: StemAction) {
        when (action) {
            StemAction.defaultActions[StemPressType.SINGLE_PRESS] -> {
                Log.d(
                    "AirPodsParser", "Default single press action: Play/Pause, not taking action."
                )
            }

            StemAction.PLAY_PAUSE -> MediaController.sendPlayPause()
            StemAction.PREVIOUS_TRACK -> MediaController.sendPreviousTrack()
            StemAction.NEXT_TRACK -> MediaController.sendNextTrack()
            StemAction.DIGITAL_ASSISTANT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val intent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                } else {
                    Log.w(
                        "AirPodsParser",
                        "Digital Assistant action is not supported on this Android version."
                    )
                }
            }

            StemAction.CYCLE_NOISE_CONTROL_MODES -> {
                Log.d("AirPodsParser", "Cycling noise control modes")
                sendBroadcast(Intent("me.kavishdevar.librepods.SET_ANC_MODE").apply {
                    setPackage(packageName)
                })
            }
        }
    }

    private fun processEarDetectionChange(earDetection: ByteArray) {
        var inEar: Boolean
        val inEarData = listOf(
            earDetectionNotification.status[0] == 0x00.toByte(),
            earDetectionNotification.status[1] == 0x00.toByte()
        )
        var justEnabledA2dp = false
        earDetectionNotification.setStatus(earDetection)
        if (config.earDetectionEnabled) {
            val data = earDetection.copyOfRange(earDetection.size - 2, earDetection.size)
            inEar = data[0] == 0x00.toByte() && data[1] == 0x00.toByte()

            val newInEarData = listOf(
                data[0] == 0x00.toByte(), data[1] == 0x00.toByte()
            )

            if (inEarData.sorted() == listOf(false, false) && newInEarData.sorted() != listOf(
                    false, false
                ) && islandWindow?.isVisible != true
            ) {
                showIsland(
                    this@AirPodsService,
                    (batteryNotification.getBattery()
                        .find { it.component == BatteryComponent.LEFT }?.level ?: 0).coerceAtMost(
                        batteryNotification.getBattery()
                            .find { it.component == BatteryComponent.RIGHT }?.level ?: 0
                    )
                )
            }

            if (newInEarData == listOf(false, false) && islandWindow?.isVisible == true) {
                islandWindow?.close()
            }

            if (newInEarData.contains(true) && inEarData == listOf(false, false)) {
                connectAudio(this@AirPodsService, device)
                justEnabledA2dp = true
                registerA2dpConnectionReceiver()
                if (MediaController.getMusicActive()) {
                    MediaController.userPlayedTheMedia = true
                }
            } else if (newInEarData == listOf(false, false)) {
                MediaController.sendPause(force = true)
                if (config.disconnectWhenNotWearing) {
                    disconnectAudio(this@AirPodsService, device)
                }
            }
            val wasNone = inEarData == listOf(false, false)
            val nowSingle = newInEarData.count { it } == 1

            if (wasNone && nowSingle) {
                MediaController.sendPlay()
                MediaController.iPausedTheMedia = false
                return
            }

            if (inEarData.contains(false) && newInEarData == listOf(true, true)) {
                Log.d("AirPodsParser", "User put in both AirPods from just one.")
                MediaController.userPlayedTheMedia = false
            }

            if (newInEarData.contains(false) && inEarData == listOf(true, true)) {
                Log.d("AirPodsParser", "User took one of two out.")
                MediaController.userPlayedTheMedia = false
            }

            Log.d(
                "AirPodsParser",
                "inEarData: ${inEarData.sorted()}, newInEarData: ${newInEarData.sorted()}"
            )

            if (newInEarData.sorted() != inEarData.sorted()) {
                if (inEar) {
                    if (!justEnabledA2dp) {
                        MediaController.sendPlay()
                        MediaController.iPausedTheMedia = false
                    }
                } else {
                    MediaController.sendPause()
                }
            }
        }
    }

    private fun registerA2dpConnectionReceiver() {
        val a2dpConnectionStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED") {
                    val state = intent.getIntExtra(
                        BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED
                    )
                    val previousState = intent.getIntExtra(
                        BluetoothProfile.EXTRA_PREVIOUS_STATE, BluetoothProfile.STATE_DISCONNECTED
                    )
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                    Log.d(
                        "MediaController",
                        "A2DP state changed: $previousState -> $state for device: ${device?.address}"
                    )

                    if (state == BluetoothProfile.STATE_CONNECTED && previousState != BluetoothProfile.STATE_CONNECTED && device?.address == this@AirPodsService.device?.address) {

                        Log.d("MediaController", "A2DP connected, sending play command")
                        MediaController.sendPlay()
                        MediaController.iPausedTheMedia = false

                        context.unregisterReceiver(this)
                    }
                }
            }
        }

        val a2dpIntentFilter =
            IntentFilter("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(a2dpConnectionStateReceiver, a2dpIntentFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(a2dpConnectionStateReceiver, a2dpIntentFilter)
        }
    }

    private fun initializeConfig() {
        config = ServiceConfig(
            deviceName = sharedPreferences.getString("name", "AirPods") ?: "AirPods",
            earDetectionEnabled = sharedPreferences.getBoolean("automatic_ear_detection", true),
            conversationalAwarenessPauseMusic = sharedPreferences.getBoolean(
                "conversational_awareness_pause_music", false
            ),
            showPhoneBatteryInWidget = sharedPreferences.getBoolean(
                "show_phone_battery_in_widget", true
            ),
            relativeConversationalAwarenessVolume = sharedPreferences.getBoolean(
                "relative_conversational_awareness_volume", true
            ),
            headGestures = sharedPreferences.getBoolean("head_gestures", true),
            disconnectWhenNotWearing = sharedPreferences.getBoolean(
                "disconnect_when_not_wearing", false
            ),
            conversationalAwarenessVolume = sharedPreferences.getInt(
                "conversational_awareness_volume", 43
            ),
            qsClickBehavior = sharedPreferences.getString("qs_click_behavior", "cycle") ?: "cycle",

            // AirPods state-based takeover
            takeoverWhenDisconnected = sharedPreferences.getBoolean(
                "takeover_when_disconnected", false
            ),
            takeoverWhenIdle = sharedPreferences.getBoolean("takeover_when_idle", false),
            takeoverWhenMusic = sharedPreferences.getBoolean("takeover_when_music", false),
            takeoverWhenCall = sharedPreferences.getBoolean("takeover_when_call", false),

            // Phone state-based takeover
            takeoverWhenRingingCall = sharedPreferences.getBoolean(
                "takeover_when_ringing_call", false
            ),
            takeoverWhenMediaStart = sharedPreferences.getBoolean(
                "takeover_when_media_start", false
            ),

            // Stem actions
            leftSinglePressAction = StemAction.fromString(
                sharedPreferences.getString(
                    "left_single_press_action", "PLAY_PAUSE"
                ) ?: "PLAY_PAUSE"
            )!!,
            rightSinglePressAction = StemAction.fromString(
                sharedPreferences.getString(
                    "right_single_press_action", "PLAY_PAUSE"
                ) ?: "PLAY_PAUSE"
            )!!,

            leftDoublePressAction = StemAction.fromString(
                sharedPreferences.getString(
                    "left_double_press_action", "PREVIOUS_TRACK"
                ) ?: "NEXT_TRACK"
            )!!,
            rightDoublePressAction = StemAction.fromString(
                sharedPreferences.getString(
                    "right_double_press_action", "NEXT_TRACK"
                ) ?: "NEXT_TRACK"
            )!!,

            leftTriplePressAction = StemAction.fromString(
                sharedPreferences.getString(
                    "left_triple_press_action", "PREVIOUS_TRACK"
                ) ?: "PREVIOUS_TRACK"
            )!!,
            rightTriplePressAction = StemAction.fromString(
                sharedPreferences.getString(
                    "right_triple_press_action", "PREVIOUS_TRACK"
                ) ?: "PREVIOUS_TRACK"
            )!!,

            leftLongPressAction = StemAction.fromString(
                sharedPreferences.getString(
                    "left_long_press_action", "CYCLE_NOISE_CONTROL_MODES"
                ) ?: "CYCLE_NOISE_CONTROL_MODES"
            )!!,
            rightLongPressAction = StemAction.fromString(
                sharedPreferences.getString(
                    "right_long_press_action", "DIGITAL_ASSISTANT"
                ) ?: "DIGITAL_ASSISTANT"
            )!!,

            cameraAction = sharedPreferences.getString("camera_action", null)
                ?.let { StemPressType.valueOf(it) },

            // AirPods device information
            airpodsName = sharedPreferences.getString("airpods_name", "") ?: "",
            airpodsModelNumber = sharedPreferences.getString("airpods_model_number", "") ?: "",
            airpodsManufacturer = sharedPreferences.getString("airpods_manufacturer", "") ?: "",
            airpodsSerialNumber = sharedPreferences.getString("airpods_serial_number", "") ?: "",
            airpodsLeftSerialNumber = sharedPreferences.getString("airpods_left_serial_number", "")
                ?: "",
            airpodsRightSerialNumber = sharedPreferences.getString(
                "airpods_right_serial_number", ""
            ) ?: "",
            airpodsVersion1 = sharedPreferences.getString("airpods_version1", "") ?: "",
            airpodsVersion2 = sharedPreferences.getString("airpods_version2", "") ?: "",
            airpodsVersion3 = sharedPreferences.getString("airpods_version3", "") ?: "",
            airpodsHardwareRevision = sharedPreferences.getString("airpods_hardware_revision", "")
                ?: "",
            airpodsUpdaterIdentifier = sharedPreferences.getString("airpods_updater_identifier", "")
                ?: "",

            selfMacAddress = sharedPreferences.getString("self_mac_address", "") ?: ""
        )
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
        if (preferences == null || key == null) return

        when (key) {
            "name" -> {
                config.deviceName = preferences.getString(key, "AirPods") ?: "AirPods"
                connectionStateMachine.rename(config.deviceName)
            }
            "mac_address" -> macAddress = preferences.getString(key, "") ?: ""
            "automatic_ear_detection" -> config.earDetectionEnabled =
                preferences.getBoolean(key, true)

            "conversational_awareness_pause_music" -> config.conversationalAwarenessPauseMusic =
                preferences.getBoolean(key, false)

            "show_phone_battery_in_widget" -> {
                config.showPhoneBatteryInWidget = preferences.getBoolean(key, true)
                widgetMobileBatteryEnabled = config.showPhoneBatteryInWidget
                updateBatteryWidget()
            }

            "relative_conversational_awareness_volume" -> config.relativeConversationalAwarenessVolume =
                preferences.getBoolean(key, true)

            "head_gestures" -> config.headGestures = preferences.getBoolean(key, true)
            "disconnect_when_not_wearing" -> config.disconnectWhenNotWearing =
                preferences.getBoolean(key, false)

            "conversational_awareness_volume" -> config.conversationalAwarenessVolume =
                preferences.getInt(key, 43)

            "qs_click_behavior" -> config.qsClickBehavior =
                preferences.getString(key, "cycle") ?: "cycle"

            ConnectionAlertStyle.PREFERENCE_KEY -> updateNotificationContent(
                connected = BluetoothConnectionManager.aacpSocket?.isConnected == true ||
                    config.bleOnlyMode,
                airpodsName = config.deviceName,
                batteryList = batteryNotification.getBattery()
            )

            // AirPods state-based takeover
            "takeover_when_disconnected" -> config.takeoverWhenDisconnected =
                preferences.getBoolean(key, true)

            "takeover_when_idle" -> config.takeoverWhenIdle = preferences.getBoolean(key, true)
            "takeover_when_music" -> config.takeoverWhenMusic = preferences.getBoolean(key, false)
            "takeover_when_call" -> config.takeoverWhenCall = preferences.getBoolean(key, true)

            // Phone state-based takeover
            "takeover_when_ringing_call" -> config.takeoverWhenRingingCall =
                preferences.getBoolean(key, true)

            "takeover_when_media_start" -> config.takeoverWhenMediaStart =
                preferences.getBoolean(key, true)

            "left_single_press_action" -> {
                config.leftSinglePressAction = StemAction.fromString(
                    preferences.getString(key, "PLAY_PAUSE") ?: "PLAY_PAUSE"
                )!!
                setupStemActions()
            }

            "right_single_press_action" -> {
                config.rightSinglePressAction = StemAction.fromString(
                    preferences.getString(key, "PLAY_PAUSE") ?: "PLAY_PAUSE"
                )!!
                setupStemActions()
            }

            "left_double_press_action" -> {
                config.leftDoublePressAction = StemAction.fromString(
                    preferences.getString(key, "PREVIOUS_TRACK") ?: "PREVIOUS_TRACK"
                )!!
                setupStemActions()
            }

            "right_double_press_action" -> {
                config.rightDoublePressAction = StemAction.fromString(
                    preferences.getString(key, "NEXT_TRACK") ?: "NEXT_TRACK"
                )!!
                setupStemActions()
            }

            "left_triple_press_action" -> {
                config.leftTriplePressAction = StemAction.fromString(
                    preferences.getString(key, "PREVIOUS_TRACK") ?: "PREVIOUS_TRACK"
                )!!
                setupStemActions()
            }

            "right_triple_press_action" -> {
                config.rightTriplePressAction = StemAction.fromString(
                    preferences.getString(key, "PREVIOUS_TRACK") ?: "PREVIOUS_TRACK"
                )!!
                setupStemActions()
            }

            "left_long_press_action" -> {
                config.leftLongPressAction = StemAction.fromString(
                    preferences.getString(key, "CYCLE_NOISE_CONTROL_MODES")
                        ?: "CYCLE_NOISE_CONTROL_MODES"
                )!!
                setupStemActions()
            }

            "right_long_press_action" -> {
                config.rightLongPressAction = StemAction.fromString(
                    preferences.getString(key, "DIGITAL_ASSISTANT") ?: "DIGITAL_ASSISTANT"
                )!!
                setupStemActions()
            }

            "camera_action" -> config.cameraAction =
                preferences.getString(key, null)?.let { StemPressType.valueOf(it) }

            // AirPods device information
            "airpods_name" -> config.airpodsName = preferences.getString(key, "") ?: ""
            "airpods_model_number" -> config.airpodsModelNumber =
                preferences.getString(key, "") ?: ""

            "airpods_manufacturer" -> config.airpodsManufacturer =
                preferences.getString(key, "") ?: ""

            "airpods_serial_number" -> config.airpodsSerialNumber =
                preferences.getString(key, "") ?: ""

            "airpods_left_serial_number" -> config.airpodsLeftSerialNumber =
                preferences.getString(key, "") ?: ""

            "airpods_right_serial_number" -> config.airpodsRightSerialNumber =
                preferences.getString(key, "") ?: ""

            "airpods_version1" -> config.airpodsVersion1 = preferences.getString(key, "") ?: ""
            "airpods_version2" -> config.airpodsVersion2 = preferences.getString(key, "") ?: ""
            "airpods_version3" -> config.airpodsVersion3 = preferences.getString(key, "") ?: ""
            "airpods_hardware_revision" -> config.airpodsHardwareRevision =
                preferences.getString(key, "") ?: ""

            "airpods_updater_identifier" -> config.airpodsUpdaterIdentifier =
                preferences.getString(key, "") ?: ""

            "self_mac_address" -> config.selfMacAddress = preferences.getString(key, "") ?: ""
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return LocalBinder()
    }

    private var gestureDetector: GestureDetector? = null
    private var isInCall = false
    private var callNumber: String? = null

    private fun initGestureDetector() {
        if (gestureDetector == null) {
            gestureDetector = GestureDetector(this)
        }
    }


    var popupShown = false
    private fun connectionAlertStyle(): ConnectionAlertStyle =
        ConnectionAlertStyle.fromPreferences(sharedPreferences)

    fun showConnectionAlert(
        batteryPercentage: Int = minimumVisibleBattery(batteryNotification.getBattery())
            ?: BatteryLevels.UNKNOWN_LEVEL
    ) {
        when (connectionAlertStyle()) {
            ConnectionAlertStyle.SYSTEM_LIVE_ALERT -> showLiveAlert(
                type = IslandType.CONNECTED,
                batteryList = batteryNotification.getBattery()
            )

            ConnectionAlertStyle.CAMERA_CUTOUT -> showIsland(
                this,
                batteryPercentage,
                IslandType.CONNECTED
            )

            ConnectionAlertStyle.BOTTOM_SHEET -> showPopup(
                this,
                sharedPreferences.getString("name", "AirPods Pro") ?: "AirPods"
            )

            ConnectionAlertStyle.OFF -> Unit
        }
    }

    fun showPopup(service: Service, name: String) {
        if (connectionAlertStyle() != ConnectionAlertStyle.BOTTOM_SHEET) {
            return
        }
        if (!Settings.canDrawOverlays(service)) {
            Log.d(TAG, "No permission for SYSTEM_ALERT_WINDOW")
            return
        }
        if (popupShown) {
            return
        }
        val popupWindow = PopupWindow(service.applicationContext) {
            popupShown = false
        }
        popupWindow.open(name, batteryNotification, airpodsInstance?.model)
        popupShown = true
    }

    var islandOpen = false
    var islandWindow: IslandWindow? = null

    @SuppressLint("MissingPermission")
    fun showIsland(
        service: Service,
        batteryPercentage: Int,
        type: IslandType = IslandType.CONNECTED,
        reversed: Boolean = false,
        otherDeviceName: String? = null
    ) {
        Log.d(TAG, "Showing island window")
        when (connectionAlertStyle()) {
            ConnectionAlertStyle.SYSTEM_LIVE_ALERT -> {
                showLiveAlert(type, batteryNotification.getBattery(), reversed, otherDeviceName)
                return
            }

            ConnectionAlertStyle.BOTTOM_SHEET -> {
                if (type == IslandType.CONNECTED) {
                    showPopup(
                        service,
                        sharedPreferences.getString("name", "AirPods Pro") ?: "AirPods"
                    )
                }
                return
            }

            ConnectionAlertStyle.OFF -> return
            ConnectionAlertStyle.CAMERA_CUTOUT -> Unit
        }
        if (!Settings.canDrawOverlays(service)) {
            Log.d(TAG, "No permission for SYSTEM_ALERT_WINDOW")
            return
        }
        mainScope.launch {
            islandWindow = IslandWindow(service.applicationContext)
            islandWindow!!.show(
                sharedPreferences.getString("name", "AirPods Pro").toString(),
                batteryPercentage,
                this@AirPodsService,
                type,
                reversed,
                otherDeviceName
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    //    var isConnectedLocally = false
    var device: BluetoothDevice? = null

    private lateinit var showIslandReceiver: BroadcastReceiver
    private var batteryReceiverRegistered = false
    var widgetMobileBatteryEnabled = false

    private val batteryChangedIntentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED && widgetMobileBatteryEnabled) {
                // Phone battery changes only affect the widget. Re-publishing all AirPods
                // metadata, broadcasts, and notifications here caused unrelated UI churn.
                updateBatteryWidget()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    fun startForegroundNotification() {
        val disconnectedNotificationChannel = NotificationChannel(
            BACKGROUND_CHANNEL_ID,
            "Background Service Status",
            NotificationManager.IMPORTANCE_NONE
        )

        val connectedNotificationChannel = NotificationChannel(
            CONNECTION_CHANNEL_ID,
            "AirPods Connection Status",
            NotificationManager.IMPORTANCE_LOW,
        )

        val liveAlertChannel = NotificationChannel(
            LIVE_ALERT_CHANNEL_ID,
            getString(R.string.live_alert_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.live_alert_channel_description)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.cancel(SOCKET_FAILURE_NOTIFICATION_ID)
        notificationManager.deleteNotificationChannel(SOCKET_FAILURE_CHANNEL_ID)
        notificationManager.deleteNotificationChannel(LEGACY_SOCKET_FAILURE_CHANNEL_ID)
        notificationManager.createNotificationChannel(disconnectedNotificationChannel)
        notificationManager.createNotificationChannel(connectedNotificationChannel)
        notificationManager.createNotificationChannel(liveAlertChannel)
        // Channel importance is immutable after first creation. Migrate away from the old
        // low-importance channel so existing installs receive the new drawer-free default.
        notificationManager.deleteNotificationChannel(LEGACY_BACKGROUND_CHANNEL_ID)

        val notificationSettingsIntent =
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, BACKGROUND_CHANNEL_ID)
            }
        val pendingIntentNotifDisable = PendingIntent.getActivity(
            this,
            0,
            notificationSettingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, BACKGROUND_CHANNEL_ID)
            .setSmallIcon(R.drawable.airpods)
            .setContentTitle(getString(R.string.background_service_title))
            .setContentText(getString(R.string.background_service_description))
            .setContentIntent(pendingIntentNotifDisable).setCategory(Notification.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW).setOngoing(true).build()

        try {
            startForeground(BACKGROUND_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendANCBroadcast() {
        sendBroadcast(Intent(AirPodsNotifications.ANC_DATA).apply {
            putExtra("data", ancNotification.status)
            setPackage(packageName)
        })
    }

    fun sendBatteryBroadcast(batteryList: List<Battery> = batteryNotification.getBattery()) {
        broadcastBatteryInformation(batteryList)
        sendBroadcast(Intent(AirPodsNotifications.BATTERY_DATA).apply {
            putParcelableArrayListExtra("data", ArrayList(batteryList))
            setPackage(packageName)
        })
    }

    private fun clearBatteryAfterDisconnect() {
        batteryNotification.reset()
        batteryStateTracker.clear()
        lastAacpBatteryPacketAt = 0L
        isHeadTrackingActive = false
        val cleared = batteryNotification.getBattery()
        lastPublishedBattery = cleared
        updateBatteryWidget(cleared)
        sendBroadcast(Intent(AirPodsNotifications.BATTERY_DATA).apply {
            putParcelableArrayListExtra("data", ArrayList(cleared))
            setPackage(packageName)
        })
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun sendBatteryNotification(batteryList: List<Battery> = batteryNotification.getBattery()) {
        updateNotificationContent(
            true,
            getSharedPreferences("settings", MODE_PRIVATE).getString("name", device?.name),
            batteryList
        )
    }

    fun setBatteryMetadata(batteryList: List<Battery> = batteryNotification.getBattery()) {
        val caseBattery = batteryList.find { it.component == BatteryComponent.CASE }
        val leftBattery = batteryList.find { it.component == BatteryComponent.LEFT }
        val rightBattery = batteryList.find { it.component == BatteryComponent.RIGHT }
        val connectedDevice = device ?: return
        val entries = buildList {
            fun addBattery(levelKey: Int, chargingKey: Int, battery: Battery?) {
                battery?.takeIf { BatteryLevels.isKnown(it.level) }?.let { known ->
                    add(levelKey to known.level.toString().toByteArray())
                    val charging = known.status == BatteryStatus.CHARGING ||
                        known.status == BatteryStatus.OPTIMIZED_CHARGING
                    add(chargingKey to if (charging) "1".toByteArray() else "0".toByteArray())
                }
            }
            addBattery(
                connectedDevice.METADATA_UNTETHERED_CASE_BATTERY,
                connectedDevice.METADATA_UNTETHERED_CASE_CHARGING,
                caseBattery,
            )
            addBattery(
                connectedDevice.METADATA_UNTETHERED_LEFT_BATTERY,
                connectedDevice.METADATA_UNTETHERED_LEFT_CHARGING,
                leftBattery,
            )
            addBattery(
                connectedDevice.METADATA_UNTETHERED_RIGHT_BATTERY,
                connectedDevice.METADATA_UNTETHERED_RIGHT_CHARGING,
                rightBattery,
            )
        }
        if (entries.isNotEmpty()) {
            systemIntegrationController.applyBluetoothMetadata(connectedDevice, entries)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    fun updateBatteryWidget(batteryList: List<Battery> = batteryNotification.getBattery()) {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val componentName = ComponentName(this, BatteryWidget::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (widgetIds.isEmpty()) return

        val remoteViews = RemoteViews(packageName, R.layout.battery_widget).also { it ->
            val openActivityIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            it.setOnClickPendingIntent(R.id.battery_widget, openActivityIntent)

            val leftBattery = batteryList.find { it.component == BatteryComponent.LEFT }
            val rightBattery = batteryList.find { it.component == BatteryComponent.RIGHT }
            val caseBattery = batteryList.find { it.component == BatteryComponent.CASE }

            it.setTextViewText(
                R.id.left_battery_widget,
                leftBattery?.let { battery -> BatteryLevels.displayPercent(battery.level) } ?: "—"
            )
            it.setProgressBar(
                R.id.left_battery_progress, 100, leftBattery?.level ?: 0, false
            )
            it.setViewVisibility(
                R.id.left_charging_icon,
                if (leftBattery?.status == BatteryStatus.CHARGING || leftBattery?.status == BatteryStatus.OPTIMIZED_CHARGING) View.VISIBLE else View.GONE
            )

            it.setTextViewText(
                R.id.right_battery_widget,
                rightBattery?.let { battery -> BatteryLevels.displayPercent(battery.level) } ?: "—"
            )
            it.setProgressBar(
                R.id.right_battery_progress, 100, rightBattery?.level ?: 0, false
            )
            it.setViewVisibility(
                R.id.right_charging_icon,
                if (rightBattery?.status == BatteryStatus.CHARGING || rightBattery?.status == BatteryStatus.OPTIMIZED_CHARGING ) View.VISIBLE else View.GONE
            )

            it.setTextViewText(
                R.id.case_battery_widget,
                caseBattery?.let { battery -> BatteryLevels.displayPercent(battery.level) } ?: "—"
            )
            it.setProgressBar(
                R.id.case_battery_progress, 100, caseBattery?.level ?: 0, false
            )
            it.setViewVisibility(
                R.id.case_charging_icon,
                if (caseBattery?.status == BatteryStatus.CHARGING || caseBattery?.status == BatteryStatus.OPTIMIZED_CHARGING ) View.VISIBLE else View.GONE
            )

            it.setViewVisibility(
                R.id.phone_battery_widget_container,
                if (widgetMobileBatteryEnabled) View.VISIBLE else View.GONE
            )
            if (widgetMobileBatteryEnabled) {
                val batteryManager = getSystemService(BatteryManager::class.java)
                val batteryLevel =
                    batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                val charging =
                    batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING
                it.setTextViewText(
                    R.id.phone_battery_widget, "$batteryLevel%"
                )
                it.setViewVisibility(
                    R.id.phone_charging_icon, if (charging) View.VISIBLE else View.GONE
                )
                it.setProgressBar(
                    R.id.phone_battery_progress, 100, batteryLevel, false
                )
            }
        }
        appWidgetManager.updateAppWidget(widgetIds, remoteViews)
    }

    @SuppressLint("MissingPermission")
    @OptIn(ExperimentalMaterial3Api::class)
    fun updateBattery(force: Boolean = false) {
        val batteryList = batteryNotification.getBattery()
        if (!force && batteryList == lastPublishedBattery) return
        lastPublishedBattery = batteryList
        setBatteryMetadata(batteryList)
        updateBatteryWidget(batteryList)
        sendBatteryBroadcast(batteryList)
        sendBatteryNotification(batteryList)
    }

    fun updateNoiseControlWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val componentName = ComponentName(this, NoiseControlWidget::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (widgetIds.isEmpty()) return
        val remoteViews = RemoteViews(packageName, R.layout.noise_control_widget).also { it ->
            val ancStatus = ancNotification.status
            val allowOffModeValue =
                aacpManager.controlCommandStatusList.find { it.identifier == AACPManager.Companion.ControlCommandIdentifiers.ALLOW_OFF_OPTION }
            val allowOffMode =
                allowOffModeValue?.value?.takeIf { it.isNotEmpty() }?.get(0) == 0x01.toByte() || sharedPreferences.getBoolean("off_listening_mode", true)
            it.setInt(
                R.id.widget_off_button,
                "setBackgroundResource",
                if (ancStatus == 1) R.drawable.widget_button_checked_shape_start else R.drawable.widget_button_shape_start
            )
            it.setInt(
                R.id.widget_transparency_button,
                "setBackgroundResource",
                if (ancStatus == 3) (if (allowOffMode) R.drawable.widget_button_checked_shape_middle else R.drawable.widget_button_checked_shape_start) else (if (allowOffMode) R.drawable.widget_button_shape_middle else R.drawable.widget_button_shape_start)
            )
            it.setInt(
                R.id.widget_adaptive_button,
                "setBackgroundResource",
                if (ancStatus == 4) R.drawable.widget_button_checked_shape_middle else R.drawable.widget_button_shape_middle
            )
            it.setInt(
                R.id.widget_anc_button,
                "setBackgroundResource",
                if (ancStatus == 2) R.drawable.widget_button_checked_shape_end else R.drawable.widget_button_shape_end
            )
            it.setViewVisibility(
                R.id.widget_off_button, if (allowOffMode) View.VISIBLE else View.GONE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                it.setViewLayoutMargin(
                    R.id.widget_transparency_button,
                    RemoteViews.MARGIN_START,
                    if (allowOffMode) 2f else 12f,
                    TypedValue.COMPLEX_UNIT_DIP
                )
            } else {
                it.setViewPadding(
                    R.id.widget_transparency_button,
                    if (allowOffMode) 2.dpToPx() else 12.dpToPx(),
                    12.dpToPx(),
                    2.dpToPx(),
                    12.dpToPx()
                )
            }
        }

        appWidgetManager.updateAppWidget(widgetIds, remoteViews)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    fun updateNotificationContent(
        connected: Boolean, airpodsName: String? = null, batteryList: List<Battery>? = null
    ) {
        val notificationManager = getSystemService(NotificationManager::class.java)

        if (!connected) {
            notificationManager.cancel(CONNECTION_NOTIFICATION_ID)
            notificationManager.cancel(LIVE_ALERT_NOTIFICATION_ID)
            notificationManager.cancel(SOCKET_FAILURE_NOTIFICATION_ID)
            return
        }

        val hasControlConnection = BluetoothConnectionManager.aacpSocket?.isConnected == true
        if (!hasControlConnection && !config.bleOnlyMode) {
            notificationManager.cancel(CONNECTION_NOTIFICATION_ID)
            notificationManager.cancel(LIVE_ALERT_NOTIFICATION_ID)
            Log.d(TAG, "Skipping connected notification without an AACP connection")
            return
        }
        notificationManager.cancel(SOCKET_FAILURE_NOTIFICATION_ID)

        val currentBatteries = batteryList ?: batteryNotification.getBattery()
        if (connectionAlertStyle() == ConnectionAlertStyle.SYSTEM_LIVE_ALERT) {
            // Native Live Alerts are event UI, not a continuously refreshed battery
            // notification. Lid/takeover events post the short-lived alert explicitly.
            notificationManager.cancel(CONNECTION_NOTIFICATION_ID)
            return
        }

        notificationManager.cancel(LIVE_ALERT_NOTIFICATION_ID)
        if (!sharedPreferences.getBoolean("show_notification_in_shade", false)) {
            notificationManager.cancel(CONNECTION_NOTIFICATION_ID)
            return
        }

        val notification = buildConnectionNotification(
            channelId = CONNECTION_CHANNEL_ID,
            name = airpodsName ?: config.deviceName,
            contentText = batterySummary(currentBatteries),
            shortCriticalText = null,
            requestLiveAlert = false,
            action = if (disconnectedBecauseReversed) {
                NotificationAction(
                    title = getString(R.string.reconnect),
                    intentAction = ACTION_RECONNECT_AFTER_REVERSE,
                    requestCode = 10
                )
            } else {
                NotificationAction(
                    title = getString(R.string.disconnect),
                    intentAction = ACTION_DISCONNECT,
                    requestCode = 11
                )
            }
        ).build()

        try {
            notificationManager.notify(CONNECTION_NOTIFICATION_ID, notification)
        } catch (error: SecurityException) {
            Log.w(TAG, "Notification permission is unavailable", error)
        }
    }

    private data class NotificationAction(
        val title: String,
        val intentAction: String,
        val requestCode: Int
    )

    @OptIn(ExperimentalMaterial3Api::class)
    private fun buildConnectionNotification(
        channelId: String,
        name: String,
        contentText: String,
        shortCriticalText: String?,
        requestLiveAlert: Boolean,
        action: NotificationAction?,
        smallIconRes: Int = R.drawable.airpods,
    ): NotificationCompat.Builder {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(smallIconRes)
            .setContentTitle(name)
            .setContentText(contentText)
            .setSubText(getString(R.string.app_name))
            .setContentIntent(contentIntent)
            .setCategory(Notification.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setRequestPromotedOngoing(requestLiveAlert)
            .apply {
                if (requestLiveAlert) setTimeoutAfter(LIVE_ALERT_TIMEOUT_MS)
                shortCriticalText?.let(::setShortCriticalText)
                action?.let {
                    addAction(
                        R.drawable.ic_bluetooth,
                        it.title,
                        PendingIntent.getService(
                            this@AirPodsService,
                            it.requestCode,
                            Intent(this@AirPodsService, AirPodsService::class.java).apply {
                                this.action = it.intentAction
                            },
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                }
            }
    }

    private fun showLiveAlert(
        type: IslandType,
        batteryList: List<Battery>,
        reversed: Boolean = false,
        otherDeviceName: String? = null,
        name: String = sharedPreferences.getString("name", config.deviceName) ?: config.deviceName
    ) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val summary = batterySummary(batteryList)
        val contentText = when (type) {
            IslandType.CONNECTED -> summary.ifBlank { getString(R.string.connected) }
            IslandType.TAKING_OVER -> getString(R.string.live_alert_connecting)
            IslandType.MOVED_TO_REMOTE -> getString(R.string.island_moved_to_remote_text)
            IslandType.MOVED_TO_OTHER_DEVICE -> getString(
                R.string.island_moved_to_other_device_text,
                otherDeviceName ?: getString(R.string.other_device)
            )
        }
        val action = when {
            type == IslandType.MOVED_TO_OTHER_DEVICE && !reversed -> NotificationAction(
                title = getString(R.string.take_back_audio),
                intentAction = ACTION_TAKE_BACK_AUDIO,
                requestCode = 12
            )

            disconnectedBecauseReversed -> NotificationAction(
                title = getString(R.string.reconnect),
                intentAction = ACTION_RECONNECT_AFTER_REVERSE,
                requestCode = 10
            )

            else -> NotificationAction(
                title = getString(R.string.disconnect),
                intentAction = ACTION_DISCONNECT,
                requestCode = 11
            )
        }
        val compactBattery = if (type == IslandType.CONNECTED) {
            BatteryDisplay.select(batteryList)
        } else {
            null
        }
        val shortText = compactBattery?.level?.let(BatteryLevels::displayPercent)
        val smallIconRes = if (compactBattery?.source == BatteryDisplaySource.CASE) {
            R.drawable.airpods_pro_case_notification
        } else {
            R.drawable.airpods
        }

        val notification = buildConnectionNotification(
            channelId = LIVE_ALERT_CHANNEL_ID,
            name = name,
            contentText = contentText,
            shortCriticalText = shortText,
            requestLiveAlert = true,
            action = action,
            smallIconRes = smallIconRes,
        ).build()

        try {
            notificationManager.cancel(CONNECTION_NOTIFICATION_ID)
            notificationManager.notify(LIVE_ALERT_NOTIFICATION_ID, notification)
        } catch (error: SecurityException) {
            Log.w(TAG, "Live Alert permission is unavailable", error)
        }
    }

    private fun batterySummary(batteryList: List<Battery>): String {
        fun Battery.summary(label: String): String? {
            if (status == BatteryStatus.DISCONNECTED) return null
            val charging = status == BatteryStatus.CHARGING ||
                status == BatteryStatus.OPTIMIZED_CHARGING
            return buildString {
                append(label)
                append(' ')
                if (charging) append("⚡")
                append(BatteryLevels.displayPercent(level))
            }
        }

        return listOfNotNull(
            batteryList.find { it.component == BatteryComponent.LEFT }?.summary(
                getString(R.string.left_short)
            ),
            batteryList.find { it.component == BatteryComponent.RIGHT }?.summary(
                getString(R.string.right_short)
            ),
            batteryList.find { it.component == BatteryComponent.CASE }?.summary(
                getString(R.string.case_short)
            )
        ).joinToString("  ·  ")
    }

    private fun minimumVisibleBattery(batteryList: List<Battery>): Int? =
        BatteryDisplay.select(batteryList)?.level

    fun handleIncomingCall() {
        if (isInCall) return
        if (config.headGestures) {
            initGestureDetector()
            gestureDetector?.startDetection { accepted ->
                if (accepted) {
                    answerCall()
                    handleIncomingCallOnceConnected = false
                } else {
                    rejectCall()
                    handleIncomingCallOnceConnected = false
                }
            }

        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun testHeadGestures(): Boolean {
        val detector = gestureDetector ?: return false
        return suspendCancellableCoroutine { continuation ->
            val started = detector.startDetection(doNotStop = true) { accepted ->
                if (continuation.isActive) {
                    continuation.resume(accepted) { _, _, _ -> }
                }
            }
            if (!started && continuation.isActive) {
                continuation.resume(false) { _, _, _ -> }
            }
            continuation.invokeOnCancellation {
                detector.stopDetection(doNotStop = true)
            }
        }
    }

    private fun answerCall() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
                if (checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                    telecomManager.acceptRingingCall() // TODO: Switch to InCallService (needs CDM association)
                }
            } else {
                val telephonyService = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
                val telephonyClass = Class.forName(telephonyService.javaClass.name)
                val method = telephonyClass.getDeclaredMethod("getITelephony")
                method.isAccessible = true
                val telephonyInterface = method.invoke(telephonyService)
                val answerCallMethod =
                    telephonyInterface.javaClass.getDeclaredMethod("answerRingingCall")
                answerCallMethod.invoke(telephonyInterface)
            }

            sendToast("Call answered via head gesture")
        } catch (e: Exception) {
            e.printStackTrace()
            sendToast("Failed to answer call: ${e.message}")
        } finally {
            islandWindow?.close()
        }
    }

    private fun rejectCall() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
                if (checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                    telecomManager.endCall() // TODO: Switch to InCallService (needs CDM association)
                }
            } else {
                val telephonyService = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
                val telephonyClass = Class.forName(telephonyService.javaClass.name)
                val method = telephonyClass.getDeclaredMethod("getITelephony")
                method.isAccessible = true
                val telephonyInterface = method.invoke(telephonyService)
                val endCallMethod = telephonyInterface.javaClass.getDeclaredMethod("endCall")
                endCallMethod.invoke(telephonyInterface)
            }

            sendToast("Call rejected via head gesture")
        } catch (e: Exception) {
            e.printStackTrace()
            sendToast("Failed to reject call: ${e.message}")
        } finally {
            islandWindow?.close()
        }
    }

    fun sendToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun processHeadTrackingData(data: ByteArray) {
        if (data.size < 55) return
        fun littleEndianShort(offset: Int): Int =
            (((data[offset + 1].toInt() and 0xFF) shl 8) or
                (data[offset].toInt() and 0xFF)).toShort().toInt()

        val horizontal = littleEndianShort(51)
        val vertical = littleEndianShort(53)
        try {
            gestureDetector?.processHeadOrientation(horizontal, vertical)
        } catch (e: Exception) {
            Log.w(TAG, "Gesture detector failed: ${e.message}")
        }
    }

    private lateinit var connectionReceiver: BroadcastReceiver

    private fun resToUri(resId: Int): Uri? {
        return try {
            Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority("me.kavishdevar.librepods")
                .appendPath(applicationContext.resources.getResourceTypeName(resId))
                .appendPath(applicationContext.resources.getResourceEntryName(resId)).build()
        } catch (_: Resources.NotFoundException) {
            null
        }
    }

    @Suppress("PrivatePropertyName")
    private val VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV = "+IPHONEACCEV"

    @Suppress("PrivatePropertyName")
    private val VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV_BATTERY_LEVEL = 1

    @Suppress("PrivatePropertyName")
    private val APPLE = 0x004C

    @Suppress("PrivatePropertyName")
    private val ACTION_BATTERY_LEVEL_CHANGED =
        "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"

    @Suppress("PrivatePropertyName")
    private val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"

    @Suppress("PrivatePropertyName")
    private val PACKAGE_ASI = "com.google.android.settings.intelligence"

    @Suppress("PrivatePropertyName")
    private val ACTION_ASI_UPDATE_BLUETOOTH_DATA = "batterywidget.impl.action.update_bluetooth_data"

    @SuppressLint("MissingPermission")
    fun broadcastBatteryInformation(
        batteryList: List<Battery> = batteryNotification.getBattery()
    ) {
        systemIntegrationController.publishSystemBattery(device, batteryList)
    }

    private fun setMetadatas(d: BluetoothDevice) {
        d.let { device ->
            val instance = airpodsInstance
            if (instance != null) {
                val metadataSet = systemIntegrationController.applyBluetoothMetadata(
                    device,
                    listOf(
                        device.METADATA_MAIN_ICON to
                            resToUri(instance.model.budCaseRes).toString().toByteArray(),
                        device.METADATA_MODEL_NAME to instance.model.name.toByteArray(),
                        device.METADATA_DEVICE_TYPE to
                            device.DEVICE_TYPE_UNTETHERED_HEADSET.toByteArray(),
                        device.METADATA_UNTETHERED_CASE_ICON to
                            resToUri(instance.model.caseRes).toString().toByteArray(),
                        device.METADATA_UNTETHERED_RIGHT_ICON to
                            resToUri(instance.model.rightBudsRes).toString().toByteArray(),
                        device.METADATA_UNTETHERED_LEFT_ICON to
                            resToUri(instance.model.leftBudsRes).toString().toByteArray(),
                        device.METADATA_MANUFACTURER_NAME to
                            instance.model.manufacturer.toByteArray(),
                        device.METADATA_COMPANION_APP to packageName.toByteArray(),
                        device.METADATA_UNTETHERED_CASE_LOW_BATTERY_THRESHOLD to
                            "20".toByteArray(),
                        device.METADATA_UNTETHERED_LEFT_LOW_BATTERY_THRESHOLD to
                            "20".toByteArray(),
                        device.METADATA_UNTETHERED_RIGHT_LOW_BATTERY_THRESHOLD to
                            "20".toByteArray(),
                    ),
                )
                Log.d(TAG, "Metadata set: $metadataSet")
            }
        }
    }

    @Suppress("ClassName")
    private object bluetoothReceiver : BroadcastReceiver() {
        private val awaitingUuidAfterAcl = java.util.concurrent.ConcurrentHashMap
            .newKeySet<String>()

        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent) {
            val bluetoothDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(
                    "android.bluetooth.device.extra.DEVICE", BluetoothDevice::class.java
                )
            } else {
                intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE") as BluetoothDevice?
            }
            val action = intent.action
            val context = context?.applicationContext ?: return
            val name = context.getSharedPreferences("settings", MODE_PRIVATE)
                .getString("name", bluetoothDevice?.name)
            if (bluetoothDevice != null && !action.isNullOrEmpty()) {
                Log.d(TAG, "Received bluetooth connection broadcast: action=$action")
                val uuid = ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a")

                if (BluetoothDevice.ACTION_ACL_CONNECTED == action) {
                    if (bluetoothDevice.uuids?.contains(uuid) == true) {
                        val intent = Intent(AirPodsNotifications.AIRPODS_CONNECTION_DETECTED)
                            .setPackage(context.packageName)
                        intent.putExtra("name", name)
                        intent.putExtra("device", bluetoothDevice)
                        context.sendBroadcast(intent)
                    } else {
                        awaitingUuidAfterAcl.add(bluetoothDevice.address)
                        bluetoothDevice.fetchUuidsWithSdp()
                    }
                } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED == action) {
                    awaitingUuidAfterAcl.remove(bluetoothDevice.address)
                    val savedMac = context.getSharedPreferences("settings", MODE_PRIVATE)
                        .getString("mac_address", "").orEmpty()
                    val isKnownAirPods = bluetoothDevice.address == savedMac ||
                        bluetoothDevice.uuids?.contains(uuid) == true
                    if (isKnownAirPods) {
                        context.sendBroadcast(
                            Intent(AirPodsNotifications.AIRPODS_DISCONNECTED).apply {
                                putExtra("reason", "Bluetooth audio disconnected")
                                setPackage(context.packageName)
                            }
                        )
                    }
                } else if ("android.bluetooth.device.action.UUID" == action) {
                    // UUID discovery also fires for explicit SDP queries while a bonded device
                    // is nowhere nearby. Only treat it as connection evidence when it follows
                    // an actual ACL_CONNECTED event.
                    if (!awaitingUuidAfterAcl.remove(bluetoothDevice.address)) return
                    val savedMac = context.getSharedPreferences("settings", MODE_PRIVATE)
                        .getString("mac_address", "") ?: ""
                    val matchedByMac = savedMac.isNotEmpty() && bluetoothDevice.address == savedMac
                    val matchedByUuid = bluetoothDevice.uuids?.contains(uuid) == true
                    if (matchedByUuid || matchedByMac) {
                        val intent = Intent(AirPodsNotifications.AIRPODS_CONNECTION_DETECTED)
                            .setPackage(context.packageName)
                        intent.putExtra("name", name)
                        intent.putExtra("device", bluetoothDevice)
                        context.sendBroadcast(intent)
                    }
                }
            }
        }
    }

    val externalBroadcastFilter = IntentFilter().apply {
        addAction("me.kavishdevar.librepods.SET_ANC_MODE")
        addAction("me.kavishdevar.librepods.CONVO_DETECT")
    }
    var externalBroadcastReceiver: BroadcastReceiver? = null

    @SuppressLint("InlinedApi", "MissingPermission", "UnspecifiedRegisterReceiverFlag")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started with intent action: ${intent?.action}")

        when (intent?.action) {
            ACTION_RECONNECT_AFTER_REVERSE -> {
                Log.d(TAG, "reconnect after reversed received, taking over")
                disconnectedBecauseReversed = false
                otherDeviceTookOver = false
                takeOver("music", manualTakeOverAfterReversed = true)
            }

            ACTION_DISCONNECT -> disconnectAirPods()
            ACTION_TAKE_BACK_AUDIO -> takeBackAudio()
        }

        return START_STICKY
    }

    fun takeBackAudio() {
        otherDeviceTookOver = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            takeOver("music", manualTakeOverAfterReversed = true)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("MissingPermission", "HardwareIds")
    fun takeOver(
        takingOverFor: String,
        manualTakeOverAfterReversed: Boolean = false,
        startHeadTrackingAgain: Boolean = false
    ) {
        if (takingOverFor == "reverse") {
            aacpManager.sendControlCommand(
                AACPManager.Companion.ControlCommandIdentifiers.OWNS_CONNECTION.value, 1
            )
            aacpManager.sendMediaInformataion(
                localMac
            )
            aacpManager.sendHijackReversed(
                localMac
            )
            connectAudio(
                this@AirPodsService, device
            )
            otherDeviceTookOver = false
        }
        val ownsConnection = aacpManager.getControlCommandStatus(AACPManager.Companion.ControlCommandIdentifiers.OWNS_CONNECTION)?.value?.get(0)?.toInt()
        Log.d(
            TAG, "owns connection: $ownsConnection"
        )
        if (BluetoothConnectionManager.aacpSocket?.isConnected == true) {
            if (!XposedRemotePrefProvider.create().getBoolean("vendor_id_hook", false) || ownsConnection == 0) {
                Log.d(TAG, "not taking over, vendorid is probably not set to apple")
                return
            }
            if (aacpManager.getControlCommandStatus(AACPManager.Companion.ControlCommandIdentifiers.OWNS_CONNECTION)?.value[0]?.toInt() != 1 || (aacpManager.audioSource?.mac != localMac && aacpManager.audioSource?.type != AACPManager.Companion.AudioSourceType.NONE)) {
                if (disconnectedBecauseReversed) {
                    if (manualTakeOverAfterReversed) {
                        Log.d(TAG, "forcefully taking over despite reverse as user requested")
                        disconnectedBecauseReversed = false
                    } else {
                        Log.d(
                            TAG,
                            "connected locally, but can not hijack as other device had reversed"
                        )
                        return
                    }
                }

                Log.d(TAG, "already connected locally, hijacking connection by asking AirPods")
                aacpManager.sendControlCommand(
                    AACPManager.Companion.ControlCommandIdentifiers.OWNS_CONNECTION.value, 1
                )
                aacpManager.sendMediaInformataion(
                    localMac
                )
                aacpManager.sendSmartRoutingShowUI(
                    localMac
                )
                aacpManager.sendHijackRequest(
                    localMac
                )
                otherDeviceTookOver = false
                connectAudio(this, device)
                showIsland(
                    this,
                    minimumVisibleBattery(batteryNotification.getBattery())
                        ?: BatteryLevels.UNKNOWN_LEVEL,
                    IslandType.CONNECTED
                )

                serviceScope.launch {
                    delay(500) // a2dp takes time, and so does taking control + AirPods pause it for no reason after connecting
                    if (takingOverFor == "music") {
                        Log.d(TAG, "Resuming music after taking control")
                        MediaController.sendPlay(replayWhenPaused = true)
                    } else if (startHeadTrackingAgain) {
                        Log.d(TAG, "Starting head tracking again after taking control")
                        Handler(Looper.getMainLooper()).postDelayed({
                            startHeadTracking()
                        }, 500)
                    }
                    delay(1000) // should ideally have a callback when it's taken over because for some reason android doesn't dispatch when it's paused
                    if (takingOverFor == "music") {
                        Log.d(TAG, "resuming again just in case")
                        MediaController.sendPlay(force = true)
                    }
                }
            } else {
                Log.d(
                    TAG, "Already connected locally and already own connection, skipping takeover"
                )
            }
            return
        }

//        if (CrossDevice.isAvailable) {
//            Log.d(TAG, "CrossDevice is available, continuing")
//        }
//        else if (bleManager.getMostRecentStatus()?.isLeftInEar == true || bleManager.getMostRecentStatus()?.isRightInEar == true) {
//            Log.d(TAG, "At least one AirPod is in ear, continuing")
//        }
//        else {
//            Log.d(TAG, "CrossDevice not available and AirPods not in ear, skipping")
//            return
//        }

        if (bleManager.getMostRecentStatus()?.isLeftInEar == false && bleManager.getMostRecentStatus()?.isRightInEar == false) {
            Log.d(TAG, "Both AirPods are out of ear, not taking over audio")
            return
        }

        val shouldTakeOverPState = when (takingOverFor) {
            "music" -> config.takeoverWhenMediaStart
            "call" -> config.takeoverWhenRingingCall
            else -> false
        }

        if (!shouldTakeOverPState) {
            Log.d(TAG, "Not taking over audio, phone state takeover disabled")
            return
        }

        val shouldTakeOver = when (bleManager.getMostRecentStatus()?.connectionState) {
            "Disconnected" -> config.takeoverWhenDisconnected
            "Idle" -> config.takeoverWhenIdle
            "Music" -> config.takeoverWhenMusic
            "Call" -> config.takeoverWhenCall
            "Ringing" -> config.takeoverWhenCall
            "Hanging Up" -> config.takeoverWhenCall
            else -> false
        }

        if (!shouldTakeOver) {
            Log.d(TAG, "Not taking over audio, airpods state takeover disabled")
            return
        }

        if (takingOverFor == "music") {
            Log.d(TAG, "Pausing music so that it doesn't play through speakers")
            MediaController.pausedWhileTakingOver = true
            MediaController.sendPause(true)
        } else {
            handleIncomingCallOnceConnected = true
        }

        Log.d(TAG, "Taking over audio")
//        CrossDevice.sendRemotePacket(CrossDevicePackets.REQUEST_DISCONNECT.packet)
        Log.d(TAG, macAddress)

//        sharedPreferences.edit { putBoolean("CrossDeviceIsAvailable", false) }
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager.adapter
        device = bluetoothAdapter.bondedDevices.find {
            it.address == macAddress
        }

        if (device != null) {
            if (config.bleOnlyMode) {
                // In BLE-only mode, just show connecting status without actual L2CAP connection
                Log.d(TAG, "BLE-only mode: showing connecting status without L2CAP connection")
                updateNotificationContent(
                    true, config.deviceName, batteryNotification.getBattery()
                )
                // Set a temporary connecting state
//                isConnectedLocally = false // Keep as false since we're not actually connecting to L2CAP
            } else {
                val targetDevice = device!!
                serviceScope.launch {
                    connectToSocket(bluetoothAdapter, targetDevice)
                    if (BluetoothConnectionManager.aacpSocket?.isConnected == true) {
                        connectAudio(this@AirPodsService, targetDevice)
                    }
                }
//                isConnectedLocally = true
            }
        }
        showIsland(
            this,
            minimumVisibleBattery(batteryNotification.getBattery())
                ?: BatteryLevels.UNKNOWN_LEVEL,
            IslandType.TAKING_OVER
        )

//        CrossDevice.isAvailable = false
    }

    @SuppressLint("MissingPermission", "UnspecifiedRegisterReceiverFlag")
    private suspend fun connectToSocket(
        adapter: BluetoothAdapter, device: BluetoothDevice, manual: Boolean = false
    ) {
        val now = System.currentTimeMillis()
        synchronized(socketConnectionLock) {
            if (BluetoothConnectionManager.aacpSocket?.isConnected == true ||
                socketConnectionInProgress
            ) {
                return
            }
            if (!manual && now - lastAutomaticSocketAttemptAt < AUTOMATIC_SOCKET_RETRY_COOLDOWN_MS) {
                Log.d(TAG, "Skipping duplicate automatic AACP connection attempt")
                return
            }
            socketConnectionInProgress = true
            if (!manual) lastAutomaticSocketAttemptAt = now
        }

        connectionStateMachine.connecting(config.deviceName, manual)
        Log.d(TAG, "<LogCollector:Start> Connecting to socket")
        val uuid: ParcelUuid = ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a")
//        if (!isConnectedLocally) {
        val socket = try {
            createBluetoothSocket(adapter, device, uuid, 4097)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create BluetoothSocket: ${e.message}")
            connectionStateMachine.recovering(config.deviceName, "Controls socket unavailable")
            if (manual) sendToast("Couldn't connect to AirPods controls: ${e.localizedMessage}")
            synchronized(socketConnectionLock) {
                socketConnectionInProgress = false
            }
            return
        }

        try {
            try {
                connectBluetoothSocket(socket, AACP_CONNECT_TIMEOUT_MS)
                this@AirPodsService.device = device
                BluetoothConnectionManager.aacpSocket = socket
                connectionStateMachine.controlConnected(config.deviceName)

                // The optional ATT channel can be slow or unavailable on unmodified phones.
                // Never hold up primary controls, the connection broadcast, or first render for it.
                if (XposedRemotePrefProvider.create().getBoolean("vendor_id_hook", false)) {
                    serviceScope.launch {
                        connectOptionalAttSocket(adapter, device, socket)
                    }
                }

                // Create AirPodsInstance from stored config if available
                if (airpodsInstance == null && config.airpodsModelNumber.isNotEmpty()) {
                    val model = AirPodsModels.getModelByModelNumber(config.airpodsModelNumber)
                    if (model != null) {
                        airpodsInstance = AirPodsInstance(
                            name = config.airpodsName,
                            model = model,
                            actualModelNumber = config.airpodsModelNumber,
                            serialNumber = config.airpodsSerialNumber,
                            leftSerialNumber = config.airpodsLeftSerialNumber,
                            rightSerialNumber = config.airpodsRightSerialNumber,
                            version1 = config.airpodsVersion1,
                            version2 = config.airpodsVersion2,
                            version3 = config.airpodsVersion3,
                        )
                        setMetadatas(device)
                    }
                }

                updateNotificationContent(
                    true, config.deviceName, batteryNotification.getBattery()
                )
                Log.d(TAG, "<LogCollector:Complete:Success> Socket connected")
                sharedPreferences.edit { putBoolean("connection_successful", true) }
                if (!sharedPreferences.contains("first_connection_successful_time")) {
                    sharedPreferences.edit {
                        putLong("first_connection_successful_time", System.currentTimeMillis())
                    }
                }
                sendBroadcast(
                    Intent(AirPodsNotifications.AIRPODS_L2CAP_CONNECTED)
                        .setPackage(packageName)
                )
            } catch (e: Exception) {
                Log.d(
                    TAG, "<LogCollector:Complete:Failed> Socket not connected, ${e.message}"
                )
                throw e
            }
            if (!socket.isConnected) {
                throw IllegalStateException("AACP socket connection timed out")
            }
            this@AirPodsService.device = device
            BluetoothConnectionManager.aacpSocket?.let {
                aacpManager.sendPacket(aacpManager.createHandshakePacket())
                aacpManager.sendSetFeatureFlagsPacket()
                aacpManager.sendNotificationRequest()
                Log.d(TAG, "Requesting proximity keys")
                aacpManager.sendRequestProximityKeys((AACPManager.Companion.ProximityKeyType.IRK.value + AACPManager.Companion.ProximityKeyType.ENC_KEY.value).toByte())
                serviceScope.launch {
                    delay(200)
                    aacpManager.sendPacket(aacpManager.createHandshakePacket())
                    delay(200)
                    aacpManager.sendSetFeatureFlagsPacket()
                    delay(200)
                    aacpManager.sendNotificationRequest()
                    delay(200)
                    aacpManager.sendSomePacketIDontKnowWhatItIs()
                    delay(200)
                    aacpManager.sendRequestProximityKeys((AACPManager.Companion.ProximityKeyType.IRK.value + AACPManager.Companion.ProximityKeyType.ENC_KEY.value).toByte())
                    val startedHeadTrackingProbe = !handleIncomingCallOnceConnected
                    if (startedHeadTrackingProbe) {
                        startHeadTrackingProbe()
                    } else {
                        handleIncomingCall()
                    }
                    serviceScope.launch {
                        delay(5_000)
                        if (BluetoothConnectionManager.aacpSocket !== socket ||
                            !socket.isConnected
                        ) {
                            return@launch
                        }
                        aacpManager.sendPacket(aacpManager.createHandshakePacket())
                        aacpManager.sendSetFeatureFlagsPacket()
                        aacpManager.sendNotificationRequest()
                        aacpManager.sendRequestProximityKeys(AACPManager.Companion.ProximityKeyType.IRK.value)
                        if (startedHeadTrackingProbe) stopHeadTrackingProbe()
                    }

                    connectionStateMachine.ready(config.deviceName)
                    sendBroadcast(
                        Intent(AirPodsNotifications.AIRPODS_CONNECTED).putExtra("device", device)
                            .apply {
                                setPackage(packageName)
                            })
                    sendBroadcast(
                        Intent(AirPodsNotifications.AIRPODS_L2CAP_READY).setPackage(packageName)
                    )

                    replayAllConfigurations()

                    while (socket.isConnected) {
                        try {
                            val buffer = ByteArray(1024)
                            val bytesRead = it.inputStream.read(buffer)
                            if (bytesRead > 0) {
                                val data = buffer.copyOf(bytesRead)
                                val headTrackingPacket = isHeadTrackingData(data)
                                aacpManager.receivePacket(data)

                                if (!headTrackingPacket &&
                                    Log.isLoggable("AirPodsData", Log.VERBOSE)
                                ) {
                                    Log.v(
                                        "AirPodsData",
                                        "Data received: ${data.joinToString(" ") { "%02X".format(it) }}"
                                    )
                                }

                            } else if (bytesRead == -1) {
                                Log.d("AirPodsService", "socket closed (bytesRead = -1)")
                                notifyControlChannelDisconnected("Controls channel closed")
                                aacpManager.disconnected()
                                return@launch
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error reading data, we have probably disconnected.")
                            e.printStackTrace()
                            notifyControlChannelDisconnected("Controls channel interrupted")
                            aacpManager.disconnected()
                            return@launch
                        }

                    }
                    Log.d("AirPods Service", "socket closed")
//                        isConnectedLocally = false
                    aacpManager.disconnected()
                    updateNotificationContent(false)
                    notifyControlChannelDisconnected("Controls channel ended")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to establish AACP connection: ${e.message}")
            connectionStateMachine.recovering(config.deviceName, "AirPods controls unavailable")
            runCatching { socket.close() }
            if (BluetoothConnectionManager.aacpSocket === socket) {
                BluetoothConnectionManager.aacpSocket = null
                runCatching { BluetoothConnectionManager.attSocket?.close() }
                BluetoothConnectionManager.attSocket = null
            }
            if (manual) {
                sendToast("Couldn't connect to AirPods controls: ${e.localizedMessage}")
            }
//                isConnectedLocally = false
            this@AirPodsService.device = device
            updateNotificationContent(false)
        } finally {
            synchronized(socketConnectionLock) {
                socketConnectionInProgress = false
            }
        }
//        } else {
//            Log.d(TAG, "Already connected locally, skipping BluetoothConnectionManager.aacpSocket? connection (isConnectedLocally = $isConnectedLocally, BluetoothConnectionManager.aacpSocket?.isConnected = ${this::BluetoothConnectionManager.aacpSocket?.isInitialized && BluetoothConnectionManager.aacpSocket?.isConnected})")
//        }
    }

    private fun notifyControlChannelDisconnected(reason: String) {
        connectionStateMachine.recovering(config.deviceName, reason)
        sendBroadcast(Intent(AirPodsNotifications.AIRPODS_DISCONNECTED).apply {
            putExtra("control_only", true)
            putExtra("reason", reason)
            setPackage(packageName)
        })
    }

    private suspend fun connectBluetoothSocket(socket: BluetoothSocket, timeoutMs: Long) {
        val timeoutJob = serviceScope.launch {
            delay(timeoutMs)
            if (!socket.isConnected) {
                Log.w(TAG, "Bluetooth socket connection timed out after ${timeoutMs}ms")
                runCatching { socket.close() }
            }
        }
        try {
            socket.connect()
        } finally {
            timeoutJob.cancel()
        }
        if (!socket.isConnected) {
            throw IllegalStateException("Bluetooth socket connection timed out")
        }
    }

    private suspend fun connectOptionalAttSocket(
        adapter: BluetoothAdapter,
        device: BluetoothDevice,
        primarySocket: BluetoothSocket,
    ) {
        val candidate = try {
            createBluetoothSocket(
                adapter,
                device,
                ParcelUuid.fromString("00000000-0000-0000-0000-000000000000"),
                31,
            )
        } catch (error: Exception) {
            Log.w(TAG, "Optional ATT socket unavailable: ${error.message}")
            return
        }

        try {
            connectBluetoothSocket(candidate, ATT_CONNECT_TIMEOUT_MS)
            if (BluetoothConnectionManager.aacpSocket !== primarySocket ||
                !primarySocket.isConnected
            ) {
                candidate.close()
                return
            }
            runCatching { BluetoothConnectionManager.attSocket?.close() }
            BluetoothConnectionManager.attSocket = candidate
            attManager.startReader()
            attManager.readCharacteristic(ATTHandles.LOUD_SOUND_REDUCTION)
            attManager.readCharacteristic(ATTHandles.TRANSPARENCY)
            attManager.readCharacteristic(ATTHandles.HEARING_AID)
        } catch (error: Exception) {
            runCatching { candidate.close() }
            if (BluetoothConnectionManager.attSocket === candidate) {
                BluetoothConnectionManager.attSocket = null
            }
            // ATT-backed hearing features are optional. A failure here must not take
            // down the primary AACP controls connection.
            Log.w(TAG, "Optional ATT connection unavailable: ${error.message}")
        }
    }

    fun disconnectForCD() {
        BluetoothConnectionManager.aacpSocket?.close()
        MediaController.pausedWhileTakingOver = false
        Log.d(TAG, "Disconnected from AirPods, showing island.")
        showIsland(
            this,
            minimumVisibleBattery(batteryNotification.getBattery())
                ?: BatteryLevels.UNKNOWN_LEVEL,
            IslandType.MOVED_TO_REMOTE
        )
        val bluetoothAdapter = getSystemService(BluetoothManager::class.java).adapter
        bluetoothAdapter.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.A2DP) {
                    val connectedDevices = proxy.connectedDevices
                    if (connectedDevices.isNotEmpty()) {
                        MediaController.sendPause()
                    }
                }
                bluetoothAdapter.closeProfileProxy(profile, proxy)
            }

            override fun onServiceDisconnected(profile: Int) {}
        }, BluetoothProfile.A2DP)
//        isConnectedLocally = false
//        CrossDevice.isAvailable = true
    }

    fun disconnectAirPods() {
        connectionStateMachine.disconnecting()
        try {
            BluetoothConnectionManager.aacpSocket?.close()
        } catch(e: Exception) {
            Log.e(TAG, "error closing aacp socket ${e.message}")
        }
//        isConnectedLocally = false
        aacpManager.disconnected()
        runCatching { attManager.disconnected() }

        BluetoothConnectionManager.aacpSocket = null
        BluetoothConnectionManager.attSocket = null

        updateNotificationContent(false)
        sendBroadcast(Intent(AirPodsNotifications.AIRPODS_DISCONNECTED).apply {
            putExtra("reason", "Disconnected by user")
            setPackage(packageName)
        })

        val bluetoothAdapter = getSystemService(BluetoothManager::class.java).adapter
        if (checkSelfPermission("android.permission.BLUETOOTH_PRIVILEGED") == PackageManager.PERMISSION_GRANTED){
            bluetoothAdapter.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.A2DP) {
                        val connectedDevices = proxy.connectedDevices
                        if (connectedDevices.isNotEmpty()) {
                            MediaController.sendPause()
                        }
                    }
                    bluetoothAdapter.closeProfileProxy(profile, proxy)
                }

                override fun onServiceDisconnected(profile: Int) {}
            }, BluetoothProfile.A2DP)
            try {
                if (Build.VERSION.SDK_INT >= 37) {
                    device?.disconnect()
                } else {
                    // disconnect() only became public in API 37. OxygenOS exposes the same
                    // framework method on Android 16, so keep the older privileged path safe.
                    device?.javaClass?.getMethod("disconnect")?.invoke(device)
                }
            } catch (e: Exception) {
                Log.w(TAG, "device.disconnect() failed, $e")
            }
        }
        if (checkSelfPermission("android.permission.MODIFY_PHONE_STATE") == PackageManager.PERMISSION_GRANTED){
            bluetoothAdapter.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.HEADSET) {
                        val connectedDevices = proxy.connectedDevices
                        if (connectedDevices.isNotEmpty()) {
                            MediaController.sendPause()
                        }
                    }
                    bluetoothAdapter.closeProfileProxy(profile, proxy)
                }

                override fun onServiceDisconnected(profile: Int) {}
            }, BluetoothProfile.HEADSET)
        }
        Log.d(TAG, "Disconnected AirPods upon user request")
    }

    val earDetectionNotification = AirPodsNotifications.EarDetection()
    val ancNotification = AirPodsNotifications.ANC()
    val batteryNotification = AirPodsNotifications.BatteryNotification()
    val conversationAwarenessNotification =
        AirPodsNotifications.ConversationalAwarenessNotification()

    @Suppress("unused")
    fun setEarDetection(enabled: Boolean) {
        if (config.earDetectionEnabled != enabled) {
            config.earDetectionEnabled = enabled
            sharedPreferences.edit { putBoolean("automatic_ear_detection", enabled) }
        }
    }

    fun getBattery(): List<Battery> {
//        if (!isConnectedLocally && CrossDevice.isAvailable) {
//            batteryNotification.setBattery(CrossDevice.batteryBytes)
//        }
        return batteryNotification.getBattery()
    }

    fun connectionDiagnosticsSnapshot(): ConnectionDiagnosticsSnapshot {
        val nowElapsed = SystemClock.elapsedRealtime()
        val connection = connectionState.value
        val headPacketAge = if (lastHeadTrackingPacketAt > 0L) {
            (nowElapsed - lastHeadTrackingPacketAt).coerceAtLeast(0L)
        } else {
            Long.MAX_VALUE
        }
        val headStreamLive = isHeadTrackingActive && headPacketAge <= 2_500L
        val headPacketLabel = if (headPacketAge == Long.MAX_VALUE) {
            "No movement packets received"
        } else {
            "Received ${BatteryStateTracker.formatAge(headPacketAge)}"
        }
        val suppressed = systemIntegrationController.suppressedIntegrations()
        val generatedAt = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date())
        val leftBattery = batteryStateTracker.componentLabel(BatteryComponent.LEFT, nowElapsed)
        val rightBattery = batteryStateTracker.componentLabel(BatteryComponent.RIGHT, nowElapsed)
        val caseBattery = batteryStateTracker.componentLabel(BatteryComponent.CASE, nowElapsed)
        val freshness = batteryStateTracker.freshnessLabel(nowElapsed)
        val alertEnabled = connectionAlertStyle() == ConnectionAlertStyle.SYSTEM_LIVE_ALERT
        val model = airpodsInstance?.model?.displayName
            ?: getMostRecentBleModelName()
            ?: "Unknown AirPods model"

        val report = buildString {
            appendLine("LibrePods connection report")
            appendLine("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Phone: ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}")
            appendLine("AirPods: $model")
            appendLine("State: ${connection.state.label}")
            appendLine("Bluetooth audio: ${connection.bluetoothAudioConnected}")
            appendLine("Controls channel: ${connection.controlChannelConnected}")
            appendLine("Left battery: $leftBattery")
            appendLine("Right battery: $rightBattery")
            appendLine("Case battery: $caseBattery")
            appendLine("Battery freshness: $freshness")
            appendLine("Head movement stream: $headStreamLive")
            appendLine("Last movement packet: $headPacketLabel")
            appendLine("OnePlus Live Alert: $alertEnabled")
            appendLine(
                "Unavailable system integrations: " +
                    if (suppressed.isEmpty()) "none" else suppressed.joinToString("; ")
            )
            appendLine("Generated: $generatedAt")
            append("Bluetooth addresses and AirPods serial numbers are intentionally omitted.")
        }

        return ConnectionDiagnosticsSnapshot(
            stateLabel = connection.state.label,
            deviceName = connection.deviceName,
            bluetoothAudioConnected = connection.bluetoothAudioConnected,
            controlChannelConnected = connection.controlChannelConnected,
            leftBattery = leftBattery,
            rightBattery = rightBattery,
            caseBattery = caseBattery,
            batteryFreshnessLabel = freshness,
            headTrackingActive = headStreamLive,
            lastHeadTrackingPacketLabel = headPacketLabel,
            liveAlertEnabled = alertEnabled,
            suppressedIntegrations = suppressed,
            generatedAtLabel = generatedAt,
            redactedReport = report,
        )
    }

    /**
     * BLE model names are usually available before the full AACP information response. This
     * avoids briefly presenting an unrelated AirPods generation on a cold app launch.
     */
    fun getMostRecentBleModelName(): String? = bleManager.getMostRecentStatus()?.model

    fun getANC(): Int {
//        if (!isConnectedLocally && CrossDevice.isAvailable) {
//            ancNotification.setStatus(CrossDevice.ancBytes)
//        }
        return ancNotification.status
    }

    fun disconnectAudio(context: Context, device: BluetoothDevice?) {
        val bluetoothAdapter = context.getSystemService(BluetoothManager::class.java).adapter
        if (checkSelfPermission("android.permission.BLUETOOTH_PRIVILEGED") == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.A2DP) {
                        try {
                            if (proxy.getConnectionState(device) == BluetoothProfile.STATE_DISCONNECTED) {
                                Log.d(TAG, "Already disconnected from A2DP")
                                return
                            }
                            val method = proxy.javaClass.getMethod(
                                "setConnectionPolicy", BluetoothDevice::class.java, Int::class.java
                            )
                            Log.d(TAG, "calling A2DP.setConnectionPolicy for ${device?.address} to 0")
                            method.invoke(proxy, device, 0)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, proxy)
                        }
                    }
                }

                override fun onServiceDisconnected(profile: Int) {}
            }, BluetoothProfile.A2DP)
        } else {
            Log.d(TAG, "not disconnecting A2DP, no BLUETOOTH_PRIVILEGED permission")
        }
        if (checkSelfPermission("android.permission.MODIFY_PHONE_STATE") == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.HEADSET) {
                        try {
                            val method =
                                proxy.javaClass.getMethod(
                                    "setConnectionPolicy",
                                    BluetoothDevice::class.java,
                                    Int::class.java
                                )
                            Log.d(TAG, "calling HEADSET.setConnectionPolicy for ${device?.address} to 0")
                            method.invoke(proxy, device, 0)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, proxy)
                        }
                    }
                }

                override fun onServiceDisconnected(profile: Int) {}
            }, BluetoothProfile.HEADSET)
        } else {
            Log.d(TAG, "not disconnecting HEADSET, no MODIFIY_PHONE_STATE permission")
        }
    }

    fun connectAudio(context: Context, device: BluetoothDevice?) {
        val bluetoothAdapter = context.getSystemService(BluetoothManager::class.java).adapter

        bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.A2DP) {
                    if (context.checkSelfPermission("android.permission.BLUETOOTH_PRIVILEGED") == PackageManager.PERMISSION_GRANTED) {
                        try {
                            val policyMethod = proxy.javaClass.getMethod(
                                "setConnectionPolicy",
                                BluetoothDevice::class.java,
                                Int::class.java
                            )
                            Log.d(TAG, "calling A2DP.setConnectionPolicy for ${device?.address} to 100")
                            policyMethod.invoke(proxy, device, 100)

                            val connectMethod =
                                proxy.javaClass.getMethod("connect", BluetoothDevice::class.java)
                            connectMethod.invoke(
                                proxy, device
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, proxy)
                            if (MediaController.pausedWhileTakingOver) {
                                MediaController.sendPlay()
                            }
                        }
                    }
                    else {
                        val connectMethod =
                            proxy.javaClass.getMethod("connect", BluetoothDevice::class.java)
                        connectMethod.invoke(
                            proxy, device
                        )
                        Log.d(TAG, "not setting connection policy for A2DP, no BLUETOOTH_PRIVILEGED permission. just called connect")
                    }
                }
            }

            override fun onServiceDisconnected(profile: Int) {}
        }, BluetoothProfile.A2DP)

        bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.HEADSET) {
                    if (checkSelfPermission("android.permission.MODIFY_PHONE_STATE") == PackageManager.PERMISSION_GRANTED) {
                        try {
                            val policyMethod = proxy.javaClass.getMethod(
                                "setConnectionPolicy",
                                BluetoothDevice::class.java,
                                Int::class.java
                            )
                            Log.d(
                                TAG,
                                "calling HEADSET.setConnectionPolicy for ${device?.address} to 100"
                            )
                            policyMethod.invoke(proxy, device, 100)
                            val connectMethod =
                                proxy.javaClass.getMethod("connect", BluetoothDevice::class.java)
                            connectMethod.invoke(proxy, device)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, proxy)
                        }
                    } else {
                        Log.d(TAG, "not setting connection policy for HEADSET, no MODIFIY_PHONE_STATE permission")
                    }
                }
            }

            override fun onServiceDisconnected(profile: Int) {}
        }, BluetoothProfile.HEADSET)
    }

    fun setName(name: String) {
        aacpManager.sendRename(name)

        if (config.deviceName != name) {
            config.deviceName = name
            device?.alias = name
            sharedPreferences.edit { putString("name", name) }
        }

        updateNotificationContent(true, name, batteryNotification.getBattery())
        Log.d(TAG, "setName: $name")
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        Log.d(TAG, "Service stopped is being destroyed for some reason!")
        connectionStateMachine.disconnected("Service stopped")

        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)

        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            unregisterReceiver(externalBroadcastReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            unregisterReceiver(connectionReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            unregisterReceiver(showIslandReceiver)
        } catch (e: Exception) {
            Log.d(TAG, "Connection-alert receiver already unregistered")
        }
        if (batteryReceiverRegistered) {
            try {
                unregisterReceiver(batteryChangedIntentReceiver)
            } catch (e: Exception) {
                Log.d(TAG, "Battery receiver already unregistered")
            }
            batteryReceiverRegistered = false
        }
        try {
            bleManager.stopScanning()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (checkSelfPermission("android.permission.READ_PHONE_STATE") == PackageManager.PERMISSION_GRANTED) {
            telephonyManager.unregisterTelephonyCallback(phoneStateListener)
        }
        gestureDetector?.dispose()
        runCatching { BluetoothConnectionManager.aacpSocket?.close() }
        runCatching { BluetoothConnectionManager.attSocket?.close() }
        BluetoothConnectionManager.aacpSocket = null
        BluetoothConnectionManager.attSocket = null
        getSystemService(NotificationManager::class.java).apply {
            cancel(CONNECTION_NOTIFICATION_ID)
            cancel(LIVE_ALERT_NOTIFICATION_ID)
            cancel(SOCKET_FAILURE_NOTIFICATION_ID)
        }
        ServiceManager.setService(null)
        serviceJob.cancel()
//        isConnectedLocally = false
//        CrossDevice.isAvailable = true
        super.onDestroy()
    }

    @Volatile
    var isHeadTrackingActive = false

    fun startHeadTracking(): Boolean {
        synchronized(headTrackingCommandLock) {
            if (BluetoothConnectionManager.aacpSocket?.isConnected != true) {
                isHeadTrackingActive = false
                Log.d(TAG, "Ignoring head-tracking start without an AACP connection")
                return false
            }
            Log.d(TAG, "Starting user Head Tracking stream")
            isHeadTrackingActive = sendHeadTrackingStartPackets()
            if (isHeadTrackingActive) HeadTracking.reset()
            return isHeadTrackingActive
        }
    }

    fun stopHeadTracking() {
        synchronized(headTrackingCommandLock) {
            sendHeadTrackingStopPackets()
            isHeadTrackingActive = false
        }
        gestureDetector?.stopDetection()
    }

    private fun startHeadTrackingProbe() {
        synchronized(headTrackingCommandLock) {
            if (isHeadTrackingActive) return
            Log.d(TAG, "Probing Head Tracking stream during connection setup")
            sendHeadTrackingStartPackets()
        }
    }

    private fun stopHeadTrackingProbe() {
        synchronized(headTrackingCommandLock) {
            // A real user or gesture session may have started while the five-second
            // connection probe was running. Never let the probe stop that session.
            if (!isHeadTrackingActive) sendHeadTrackingStopPackets()
        }
    }

    private fun sendHeadTrackingStartPackets(): Boolean {
        val useAlternatePackets =
            sharedPreferences.getBoolean("use_alternate_head_tracking_packets", true)
        val alternateStarted = if (useAlternatePackets) {
            aacpManager.sendDataPacket(aacpManager.createAlternateStartHeadTrackingPacket())
        } else {
            false
        }
        val standardStarted = aacpManager.sendStartHeadTracking()
        return alternateStarted || standardStarted
    }

    private fun sendHeadTrackingStopPackets() {
        val useAlternatePackets =
            sharedPreferences.getBoolean("use_alternate_head_tracking_packets", true)
        if (useAlternatePackets) {
            aacpManager.sendDataPacket(aacpManager.createAlternateStopHeadTrackingPacket())
        }
        // startHeadTracking always sends the standard start command, even when the
        // compatibility command is enabled. Always pair it with the standard stop
        // command so the AirPods do not keep streaming IMU packets in the background.
        aacpManager.sendStopHeadTracking()
    }

    @SuppressLint("MissingPermission")
    fun reconnectFromSavedMac() {
        val bluetoothAdapter = getSystemService(BluetoothManager::class.java).adapter
        device = bluetoothAdapter.bondedDevices.find {
            it.address == macAddress
        }
        if (device != null) {
            serviceScope.launch {
                Log.d(TAG, "connecting to $macAddress")
                connectToSocket(bluetoothAdapter, device!!, manual = true)
                if (BluetoothConnectionManager.aacpSocket?.isConnected == true) {
                    connectAudio(this@AirPodsService, device!!)
                }
            }
        }
    }
}

private fun Int.dpToPx(): Int {
    val density = Resources.getSystem().displayMetrics.density
    return (this * density).toInt()
}

fun getNextMode(currentMode: Int, configByte: Int, offmodeEnabled: Boolean): Int {
    val enabledModes = buildList {
        if ((configByte and 0x01) != 0 && offmodeEnabled) add(1)
        if ((configByte and 0x04) != 0) add(3)
        if ((configByte and 0x08) != 0) add(4)
        if ((configByte and 0x02) != 0) add(2)
    }
    Log.d(TAG, "currentMode: $currentMode, config: ${configByte.toString(2)}")

    if (enabledModes.isEmpty()) return currentMode

    val currentIndex = enabledModes.indexOf(currentMode)
    val nextIndex = if (currentIndex == -1) 0 else (currentIndex + 1) % enabledModes.size

    return enabledModes[nextIndex]
}
