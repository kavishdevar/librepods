package me.kavishdevar.librepods.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.database.widget.WidgetConfigDao
import me.kavishdevar.librepods.database.widget.WidgetConfigEntity

class WidgetConfigRepository(
    private val widgetConfigDao: WidgetConfigDao
) {
    private val _widgetConfigs = MutableStateFlow<List<WidgetConfigEntity>>(emptyList())
    val widgetConfigs: StateFlow<List<WidgetConfigEntity>> = _widgetConfigs.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            _widgetConfigs.value = widgetConfigDao.getAll()
        }
    }

    fun setWidgetConfig(widgetConfig: WidgetConfigEntity) {
        val existingConfig = _widgetConfigs.value.find { it.appWidgetId == widgetConfig.appWidgetId }
        if (existingConfig != null) {
            val updatedConfigs = _widgetConfigs.value.map {
                if (it.appWidgetId == widgetConfig.appWidgetId) widgetConfig else it
            }
            _widgetConfigs.value = updatedConfigs
        } else {
            _widgetConfigs.value += widgetConfig
        }

        scope.launch {
            widgetConfigDao.upsert(widgetConfig)
        }
    }
}
