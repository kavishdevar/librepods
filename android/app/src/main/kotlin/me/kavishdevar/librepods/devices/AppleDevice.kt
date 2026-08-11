package me.kavishdevar.librepods.devices

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.bluetooth.MacAddress
import me.kavishdevar.librepods.bluetooth.aacp.AACPManager
import me.kavishdevar.librepods.bluetooth.aacp.types.AppleEvent
import me.kavishdevar.librepods.bluetooth.aacp.types.ControlCommandIdentifier
import me.kavishdevar.librepods.bluetooth.att.ATTHandle
import me.kavishdevar.librepods.bluetooth.att.ATTManager
import me.kavishdevar.librepods.data.StemAction
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "AppleDevice"

@SuppressLint("MissingPermission")
class AppleDevice(
    override val bluetoothAdapter: BluetoothAdapter,
    override val bluetoothDevice: BluetoothDevice,
    currentState: ConnectionState
) : Device<AppleState, AppleSettings, AppleMetadata> {
    override val macAddress = MacAddress(bluetoothDevice.address)
    private val _state = MutableStateFlow(AppleState())
    override val state = _state.asStateFlow()

    private val _settings = MutableStateFlow(AppleSettings())
    override val settings = _settings.asStateFlow()

    private val _metadata = MutableStateFlow(AppleMetadata())
    override val metadata = _metadata.asStateFlow()

    private val _connectionState = MutableStateFlow(currentState)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<AppleEvent>()
    val events = _events.asSharedFlow()

    private val _connectionNumber = MutableStateFlow(0)
    override val connectionNumber = _connectionNumber.asStateFlow()

    fun loadInitialState(state: AppleState, settings: AppleSettings, metadata: AppleMetadata) {
        _state.value = state
        _settings.value = settings
        _metadata.value = metadata
    }

    internal inline fun updateState(
        transform: (AppleState) -> AppleState
    ) {
        _state.update(transform)
    }

    internal inline fun updateMetadata(
        transform: (AppleMetadata) -> AppleMetadata
    ) {
        _metadata.update(transform)
    }

    internal suspend fun emitEvent(event: AppleEvent) {
        _events.emit(event)
    }

    val aacp = AACPManager(this)
    val att = ATTManager(this)

    init {
        updateMetadata {
            it.copy(
                name = bluetoothDevice.alias ?: bluetoothDevice.name ?: "Unknown"
            )
        }

        if (currentState == ConnectionState.AVAILABLE) {
            connect()
        }
    }

    override fun connect(): Boolean {
        _connectionState.update {
            ConnectionState.CONNECTING
        }

        val success = aacp.connect() // && att.connect()

        CoroutineScope(Dispatchers.IO).launch {
            _state.update {
                it.copy(
                    loudSoundReductionEnabled = readATTCharacteristic(ATTHandle.LOUD_SOUND_REDUCTION)?.getOrNull(0)?.toInt() == 1,
                    transparencyData = readATTCharacteristic(ATTHandle.TRANSPARENCY)?: byteArrayOf(),
                    hearingAidData = readATTCharacteristic(ATTHandle.HEARING_AID)?: byteArrayOf()
                )
            }
        }

        _connectionState.update {
            if (success) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED
        }
        _connectionNumber.update {
            it + 1
        }

        return success
    }

    override fun disconnect() {
        _connectionState.update {
            ConnectionState.DISCONNECTING
        }

        aacp.disconnect()
//        att.disconnect()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            try {
                bluetoothDevice.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "couldn't disconnect bluetooth device", e)
            }
        }

        _connectionState.update {
            ConnectionState.DISCONNECTED
        }
    }

    fun setControlCommand(identifier: ControlCommandIdentifier, value: ByteArray): Boolean = aacp.sendControlCommand(identifier.value, value)
    fun setControlCommand(identifier: ControlCommandIdentifier, value: Byte): Boolean = aacp.sendControlCommand(identifier.value, value)
    fun setControlCommand(identifier: ControlCommandIdentifier, value: Int): Boolean = aacp.sendControlCommand(identifier.value, value)
    fun setControlCommand(identifier: ControlCommandIdentifier, value: Boolean): Boolean = aacp.sendControlCommand(identifier.value, value)

    suspend fun writeATTCharacteristic(handle: ATTHandle, value: ByteArray): Boolean {
        val success = att.writeCharacteristic(handle, value)
        if (success) {
            when (handle) {
                ATTHandle.LOUD_SOUND_REDUCTION -> _state.update { it.copy(loudSoundReductionEnabled = value.getOrNull(0)?.toInt() == 1) }
                ATTHandle.TRANSPARENCY -> _state.update { it.copy(transparencyData = value) }
                ATTHandle.HEARING_AID -> _state.update { it.copy(hearingAidData = value) }
            }
        }
        return success
    }

    // can't use att notifications; keeping ATT connected causes airpods to disconnect every few seconds for some reason
    // we will poll the characteristic every 1 seconds instead.
    fun observeATTCharacteristic(handle: ATTHandle): Job = CoroutineScope(Dispatchers.IO).launch {
        while (true) {
            val value = readATTCharacteristic(handle)
            if (value != null) {
                when (handle) {
                    ATTHandle.LOUD_SOUND_REDUCTION -> _state.update { it.copy(loudSoundReductionEnabled = value.getOrNull(0)?.toInt() == 1) }
                    ATTHandle.TRANSPARENCY -> _state.update { it.copy(transparencyData = value) }
                    ATTHandle.HEARING_AID -> _state.update { it.copy(hearingAidData = value) }
                }
            }
            delay(1000.milliseconds)
        }
    }


    suspend fun readATTCharacteristic(handle: ATTHandle): ByteArray? = att.readCharacteristic(handle)

    // TODO: handle recording session
    fun startRecording() = aacp.requestMicrophoneStream()
    fun stopRecording() = aacp.endMicrophoneStream()

    fun toggleListeningMode(modeBit: Int) {
        val currentByte = state.value.controlStates[ControlCommandIdentifier.LISTENING_MODE_CONFIGS]?.get(0)?.toInt() ?: 0
        val newValue = if ((currentByte and modeBit) != 0) {
            val temp = currentByte and modeBit.inv()
            if (countEnabledModes(temp) >= 2) temp else currentByte
        } else {
            currentByte or modeBit
        }
        setControlCommand(ControlCommandIdentifier.LISTENING_MODE_CONFIGS, newValue)
//        sharedPreferences.edit { putInt("long_press_byte", newValue) }
    }

    fun setLongPressAction(side: String, action: StemAction) {
//        val prefKey = if (side.lowercase() == "left") "left_long_press_action" else "right_long_press_action"
//        sharedPreferences.edit { putString(prefKey, action.name) }
        _settings.update {
            if (side.lowercase() == "left") it.copy(leftLongPressAction = action) else it.copy(rightLongPressAction = action)
        }
    }

    fun renameDevice(newName: String) {
        aacp.sendRename(newName)
        _metadata.update {
            it.copy(name = newName)
        }
    }

    fun startHeadTracking() {
        if (settings.value.alternateHeadTrackingPackets) {
            aacp.sendStartAlternateHeadTracking()
        } else {
            aacp.sendStartHeadTracking()
        }
        _state.update {
            it.copy(headTrackingActive = true)
        }
    }

    fun stopHeadTracking() {
        if (settings.value.alternateHeadTrackingPackets) {
            aacp.sendStopAlternateHeadTracking()
        } else {
            aacp.sendStopHeadTracking()
        }
        _state.update {
            it.copy(headTrackingActive = false)
        }
    }

    fun setHeadGesturesEnabled(enabled: Boolean) {
        _settings.update {
            it.copy(headGesturesEnabled = enabled)
        }
    }

    fun setCustomEqEnabled(enabled: Boolean) {
        aacp.setCustomEq(_state.value.customEq.copy(state = if(enabled) 2 else 1))
    }

    fun setCustomEq(low: Int, mid: Int, high: Int) {
        require(low in 0..100)
        require(mid in 0..100)
        require(high in 0..100)
        aacp.setCustomEq(_state.value.customEq.copy(low = low, mid = mid, high = high))
    }

    fun testHeadGestures() {
        if (settings.value.alternateHeadTrackingPackets) {
            aacp.sendStartAlternateHeadTracking()
        } else {
            aacp.sendStartHeadTracking()
        }
        _state.update {
            it.copy(
                detectHeadGestures = true
            )
        }
    }

    fun sendRawPacket(data: ByteArray): Boolean = aacp.sendRawPacket(data)

////    private val _cameraAction = MutableStateFlow(
////        sharedPreferences.getString("camera_action", null)
////            ?.let { value -> StemPressType.entries.find { it.name == value } })
////
////    val cameraAction: StateFlow<StemPressType?> = _cameraAction
////
////    fun setCameraAction(action: StemPressType?) {
////        sharedPreferences.edit {
////            if (action == null) remove("camera_action")
////            else putString("camera_action", action.name)
////        }
////        _cameraAction.value = action
////    }

}

private fun countEnabledModes(byteValue: Int): Int {
    var count = 0
    if ((byteValue and 0x01) != 0) count++
    if ((byteValue and 0x02) != 0) count++
    if ((byteValue and 0x04) != 0) count++
    if ((byteValue and 0x08) != 0) count++
    return count
}
