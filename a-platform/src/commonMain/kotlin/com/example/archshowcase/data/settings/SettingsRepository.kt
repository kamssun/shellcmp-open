package com.example.archshowcase.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.example.archshowcase.core.AppConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * 设置 Repository 接口
 */
interface SettingsRepository {
    val useOBOScheduler: Flow<Boolean>
    suspend fun setUseOBOScheduler(enabled: Boolean)
}

/**
 * DataStore 实现，用于正式环境
 */
class DataStoreSettingsRepository : SettingsRepository, KoinComponent {

    private val dataStore: DataStore<Preferences> by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val oboKey = booleanPreferencesKey("use_obo_scheduler")

    override val useOBOScheduler: Flow<Boolean> = dataStore.data.map { it[oboKey] ?: true }

    init {
        scope.launch {
            useOBOScheduler.distinctUntilChanged().collect {
                AppConfig.useOBOScheduler = it
            }
        }
    }

    override suspend fun setUseOBOScheduler(enabled: Boolean) {
        dataStore.edit { it[oboKey] = enabled }
    }
}

/**
 * 内存实现，用于 Preview
 */
class InMemorySettingsRepository : SettingsRepository {
    private val _useOBOScheduler = MutableStateFlow(true)
    override val useOBOScheduler: Flow<Boolean> = _useOBOScheduler

    override suspend fun setUseOBOScheduler(enabled: Boolean) {
        _useOBOScheduler.value = enabled
        AppConfig.useOBOScheduler = enabled
    }
}
