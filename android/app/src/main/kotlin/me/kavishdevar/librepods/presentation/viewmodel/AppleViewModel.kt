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

package me.kavishdevar.librepods.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.billing.BillingManager
import me.kavishdevar.librepods.bluetooth.aacp.types.ControlCommandIdentifier
import me.kavishdevar.librepods.bluetooth.att.ATTHandle
import me.kavishdevar.librepods.data.StemAction
import me.kavishdevar.librepods.data.recording.Recording
import me.kavishdevar.librepods.data.xposed.XposedRemotePrefProvider
import me.kavishdevar.librepods.devices.AppleDevice
import me.kavishdevar.librepods.devices.AppleMetadata
import me.kavishdevar.librepods.devices.AppleSettings
import me.kavishdevar.librepods.devices.AppleState
import me.kavishdevar.librepods.repository.RecordingRepository

data class AppleUiState(
    val state: AppleState = AppleState(),
    val settings: AppleSettings = AppleSettings(),
    val metadata: AppleMetadata = AppleMetadata(),

    val isPremium: Boolean = false,
    val vendorIdHook: Boolean = false,
    val recordings: List<Recording> = emptyList(),
)

class AppleViewModel(
    private val device: AppleDevice,
    private val recordingRepository: RecordingRepository,
) : ViewModel(), DeviceViewModel {
    val billingManager = BillingManager

    val events = device.events

    private var attObserveJob: Job? = null

    val uiState = combine(
        device.state,
        device.settings,
        device.metadata,
        billingManager.provider.isPremium,
    ) { state, settings, metadata, isPremium ->
        AppleUiState(
            state = state,
            settings = settings,
            metadata = metadata,
            isPremium = isPremium,
            vendorIdHook = XposedRemotePrefProvider.create().getBoolean(
                "vendor_id_hook",
                false
            ) // TODO: make this a Flow, even if it means polling every few seconds
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppleUiState(
            state = device.state.value,
            settings = device.settings.value,
            metadata = device.metadata.value,
            isPremium = billingManager.provider.isPremium.value,
            vendorIdHook = XposedRemotePrefProvider.create()
                .getBoolean("vendor_id_hook", false)
        )
    )

    fun disconnect() = device.disconnect()
    fun recordings(): List<Recording> = recordingRepository.recordings()

    fun setControlCommand(identifier: ControlCommandIdentifier, value: ByteArray): Boolean =
        device.setControlCommand(identifier, value)

    fun setControlCommand(identifier: ControlCommandIdentifier, value: Byte): Boolean =
        device.setControlCommand(identifier, value)

    fun setControlCommand(identifier: ControlCommandIdentifier, value: Int): Boolean =
        device.setControlCommand(identifier, value)

    fun setControlCommand(identifier: ControlCommandIdentifier, value: Boolean): Boolean =
        device.setControlCommand(identifier, value)

    fun writeATTCharacteristic(handle: ATTHandle, value: ByteArray) {
        viewModelScope.launch {
            device.writeATTCharacteristic(handle, value)
        }
    }

    fun observeATTCharacteristic(handle: ATTHandle) {
            attObserveJob = device.observeATTCharacteristic(handle)
    }
    fun stopObservingATTCharacteristic() {
        attObserveJob?.cancel()
        attObserveJob = null
    }

    fun startRecording() = device.startRecording()
    fun stopRecording() = device.stopRecording()

    fun toggleListeningMode(modeBit: Int) = device.toggleListeningMode(modeBit)

    fun setLongPressAction(side: String, action: StemAction) = device.setLongPressAction(side, action)

    fun renameDevice(newName: String) = device.renameDevice(newName)


    fun startHeadTracking() = device.startHeadTracking()
    fun stopHeadTracking() = device.stopHeadTracking()

    fun setHeadGesturesEnabled(enabled: Boolean) = device.setHeadGesturesEnabled(enabled)

    fun setCustomEqEnabled(enabled: Boolean) = device.setCustomEqEnabled(enabled)
    fun setCustomEq(low: Int, mid: Int, high: Int) = device.setCustomEq(low, mid, high)

    fun testHeadGestures() = device.testHeadGestures()

    fun sendRawPacket(data: ByteArray): Boolean = device.sendRawPacket(data)
}
