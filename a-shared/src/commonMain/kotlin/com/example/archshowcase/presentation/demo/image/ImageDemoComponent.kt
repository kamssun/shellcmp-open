package com.example.archshowcase.presentation.demo.image

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

interface ImageDemoComponent {
    val state: StateFlow<ImageDemoStore.State>
    val scrollRestoreEvent: SharedFlow<ScrollPosition>

    fun onBack()
    fun loadInitial()
    fun loadMore()
    fun updateScrollPosition(firstVisibleIndex: Int, offset: Int)
}

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultImageDemoComponent(
    context: AppComponentContext
) : ImageDemoComponent, AppComponentContext by context, KoinComponent {

    init {
        loadImageModule()
    }

    private val storeFactory: ImageDemoStoreFactory by inject()

    private val scrollCoordinator: ScrollUpdateCoordinator by inject()

    private val storeWithScroll = registerScrollRestorableStore(
        name = IMAGE_DEMO_STORE_NAME,
        factory = { storeFactory.create() },
        getItemCount = { state -> state.images.size },
        isUserScrolling = scrollCoordinator::isUserScrolling
    )
    private val store = storeWithScroll.first
    override val scrollRestoreEvent: SharedFlow<ScrollPosition> = storeWithScroll.second

    override val state: StateFlow<ImageDemoStore.State> = store.stateFlow

    override fun loadInitial() {
        store.accept(ImageDemoStore.Intent.LoadInitial)
    }

    override fun loadMore() {
        store.accept(ImageDemoStore.Intent.LoadMore)
    }

    override fun updateScrollPosition(firstVisibleIndex: Int, offset: Int) {
        scrollCoordinator.runWithUserScroll {
            store.accept(ImageDemoStore.Intent.UpdateScrollPosition(firstVisibleIndex, offset))
        }
    }
}
