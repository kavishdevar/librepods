package me.kavishdevar.librepods.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import me.kavishdevar.librepods.billing.BillingManager
import me.kavishdevar.librepods.data.xposed.XposedRemotePrefProvider
import me.kavishdevar.librepods.database.app.AppSettingsEntity
import me.kavishdevar.librepods.database.app.AppStateEntity
import me.kavishdevar.librepods.repository.AppDataRepository

data class AppUiState(
    val settings: AppSettingsEntity = AppSettingsEntity(),
    val state: AppStateEntity = AppStateEntity(),

    val vendorIdHook: Boolean = false,
    val isPremium: Boolean = false,
)

class AppSettingsViewModel(
    private val appDataRepository: AppDataRepository,
) : ViewModel() {

    private val xposedRemotePref = XposedRemotePrefProvider.create()

    private val vendorIdHook = MutableStateFlow(
        xposedRemotePref.getBoolean("vendor_id_hook", false)
    )

    val uiState = combine(
        appDataRepository.settings,
        appDataRepository.state,
        BillingManager.provider.isPremium,
        vendorIdHook,
    ) { settings, state, isPremium, vendorIdHook ->
        AppUiState(
            settings = settings,
            state = state,
            isPremium = isPremium,
            vendorIdHook = vendorIdHook,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppUiState(
            settings = appDataRepository.settings.value,
            state = appDataRepository.state.value,
            isPremium = BillingManager.provider.isPremium.value,
            vendorIdHook = vendorIdHook.value,
        )
    )

    fun updateSettings(
        transform: (AppSettingsEntity) -> AppSettingsEntity
    ) = appDataRepository.updateSettings(transform)

    fun updateState(
        transform: (AppStateEntity) -> AppStateEntity
    ) = appDataRepository.updateState(transform)

    fun setVendorIdHook(enabled: Boolean) {
        xposedRemotePref.putBoolean("vendor_id_hook", enabled)
        vendorIdHook.value = enabled
    }
}
