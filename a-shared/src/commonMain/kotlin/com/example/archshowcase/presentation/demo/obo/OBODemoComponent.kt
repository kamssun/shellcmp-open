package com.example.archshowcase.presentation.demo.obo

import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.example.archshowcase.core.trace.restore.registerScrollRestorableStore
import com.example.archshowcase.core.trace.scroll.ScrollPosition
import com.example.archshowcase.core.trace.scroll.ScrollUpdateCoordinator
import com.example.archshowcase.presentation.navigation.AppComponentContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface OBODemoComponent {
    val state: StateFlow<OBODemoStore.State>
    val scrollRestoreEvent: SharedFlow<ScrollPosition>

    fun onBackClicked()
    fun onSetEffectsPerItem(count: Int)
    fun onSetBlockTime(ms: Int)
    fun onReload()
    fun onToggleOBO(enabled: Boolean)
    fun updateScrollPosition(firstVisibleIndex: Int, offset: Int)
}

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultOBODemoComponent(
    context: AppComponentContext
) : OBODemoComponent, AppComponentContext by context, KoinComponent {

    init { loadOBODemoModule() }

    private val storeFactory: OBODemoStoreFactory by inject()

    private val scrollCoordinator: ScrollUpdateCoordinator by inject()

    private val storeWithScroll = registerScrollRestorableStore(
        name = OBODEMO_STORE_NAME,
        factory = { storeFactory.create() },
        getItemCount = { 30 },
        isUserScrolling = scrollCoordinator::isUserScrolling
    )
    private val store = storeWithScroll.first
    override val scrollRestoreEvent: SharedFlow<ScrollPosition> = storeWithScroll.second

    override val state: StateFlow<OBODemoStore.State> = store.stateFlow

    override fun onBackClicked() = navigator.pop()

    override fun onSetEffectsPerItem(count: Int) {
        store.accept(OBODemoStore.Intent.SetEffectsPerItem(count))
    }

    override fun onSetBlockTime(ms: Int) {
        store.accept(OBODemoStore.Intent.SetBlockTime(ms))
    }

    override fun onReload() {
        store.accept(OBODemoStore.Intent.Reload)
    }

    override fun onToggleOBO(enabled: Boolean) {
        store.accept(OBODemoStore.Intent.ToggleOBO(enabled))
    }

    override fun updateScrollPosition(firstVisibleIndex: Int, offset: Int) {
        scrollCoordinator.runWithUserScroll {
            store.accept(OBODemoStore.Intent.UpdateScrollPosition(firstVisibleIndex, offset))
        }
    }
}
