package me.kavishdevar.librepods.repository

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.database.app.AppSettingsDao
import me.kavishdevar.librepods.database.app.AppSettingsEntity
import me.kavishdevar.librepods.database.app.AppStateDao
import me.kavishdevar.librepods.database.app.AppStateEntity

class AppDataRepository(
    private val settingsDao: AppSettingsDao,
    private val stateDao: AppStateDao,
) {
    private val initialized = CompletableDeferred<Unit>()

    suspend fun awaitInitialized() {
        initialized.await()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _settings = MutableStateFlow(AppSettingsEntity())
    val settings: StateFlow<AppSettingsEntity> = _settings.asStateFlow()

    private val _state = MutableStateFlow(AppStateEntity())
    val state: StateFlow<AppStateEntity> = _state.asStateFlow()

    init {
        scope.launch {
            _settings.value = settingsDao.get() ?: AppSettingsEntity()
            _state.value = stateDao.get() ?: AppStateEntity()
            initialized.complete(Unit)
        }
    }

    fun updateSettings(
        transform: (AppSettingsEntity) -> AppSettingsEntity
    ) {
        val newSettings = transform(_settings.value)

        _settings.value = newSettings

        scope.launch {
            settingsDao.upsert(newSettings)
        }
    }

    fun updateState(
        transform: (AppStateEntity) -> AppStateEntity
    ) {
        val newState = transform(_state.value)

        _state.value = newState

        scope.launch {
            stateDao.upsert(newState)
        }
    }
}
