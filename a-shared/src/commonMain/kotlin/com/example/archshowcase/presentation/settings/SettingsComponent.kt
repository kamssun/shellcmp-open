package com.example.archshowcase.presentation.settings

import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.example.archshowcase.core.trace.restore.registerRestorableStore
import com.example.archshowcase.presentation.navigation.AppComponentContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface SettingsComponent {
    val state: StateFlow<SettingsStore.State>

    fun onBackClicked()
    fun onOBOSchedulerToggle(enabled: Boolean)
}

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultSettingsComponent(
    context: AppComponentContext
) : SettingsComponent, AppComponentContext by context, KoinComponent {

    init { loadSettingsModule() }

    private val storeFactory: SettingsStoreFactory by inject()

    private val store = registerRestorableStore(
        name = SETTINGS_STORE_NAME,
        factory = { storeFactory.create() }
    )

    override val state: StateFlow<SettingsStore.State> = store.stateFlow

    override fun onBackClicked() = navigator.pop()

    override fun onOBOSchedulerToggle(enabled: Boolean) {
        store.accept(SettingsStore.Intent.SetOBOScheduler(enabled))
    }
}
