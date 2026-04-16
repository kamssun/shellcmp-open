package com.example.archshowcase.presentation.demo.image

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.archshowcase.core.trace.restore.RestoreRegistry
import com.example.archshowcase.core.trace.scroll.ScrollPosition
import com.example.archshowcase.network.api.ImageRepository
import com.example.archshowcase.network.dto.ImageItem
import com.example.archshowcase.presentation.demo.image.ImageDemoStore.Intent
import com.example.archshowcase.presentation.demo.image.ImageDemoStore.Msg
import com.example.archshowcase.presentation.demo.image.ImageDemoStore.State
import com.example.archshowcase.core.scheduler.oboLaunch
import kotlin.time.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ImageDemoStoreFactory : KoinComponent {
    private val storeFactory: StoreFactory by inject()
    private val repository: ImageRepository by inject()
    fun create(): ImageDemoStore {
        val (initialState, needsBootstrap) = resolveImageDemoStoreInitialState()

        return object : ImageDemoStore,
            Store<Intent, State, Nothing> by storeFactory.create(
                name = IMAGE_DEMO_STORE_NAME,
                initialState = initialState,
                executorFactory = { ExecutorImpl(repository) },
                reducer = ReducerImpl
            ) {}
    }

    companion object {
        private const val PAGE_SIZE = 20
        private const val OBO_TAG = "ImageDemoStore"
    }

    private class ExecutorImpl(
        private val repository: ImageRepository
    ) : CoroutineExecutor<Intent, Nothing, State, Msg, Nothing>() {

        override fun executeIntent(intent: Intent) {
            when (intent) {
                is Intent.LoadInitial -> loadInitial()
                is Intent.LoadMore -> loadMore()
                is Intent.Reset -> reset()
                is Intent.UpdateScrollPosition -> updateScrollPosition(intent)
            }
        }

        private fun loadInitial() {
            val currentState = state()
            if (currentState.isInitialLoading || currentState.images.isNotEmpty()) return

            scope.oboLaunch(OBO_TAG) {
                dispatch(Msg.InitialLoadingStarted)

                repository.getImages(offset = 0, limit = PAGE_SIZE)
                    .onSuccess { response ->
                        dispatch(Msg.ImagesLoaded(
                            images = response.items,
                            totalCount = response.total,
                            hasMore = response.hasMore,
                            loadType = "initial",
                            timestamp = Clock.System.now().toEpochMilliseconds()
                        ))
                    }
                    .onFailure { error ->
                        dispatch(Msg.LoadFailed(error.message ?: "加载失败"))
                    }
            }
        }

        private fun loadMore() {
            val currentState = state()
            if (!currentState.canLoadMore) return

            scope.oboLaunch(OBO_TAG) {
                dispatch(Msg.LoadMoreStarted)
                val offset = currentState.images.size

                repository.getImages(offset = offset, limit = PAGE_SIZE)
                    .onSuccess { response ->
                        val newImages = currentState.images + response.items
                        dispatch(Msg.ImagesLoaded(
                            images = newImages,
                            totalCount = response.total,
                            hasMore = response.hasMore,
                            loadType = "more",
                            timestamp = Clock.System.now().toEpochMilliseconds()
                        ))
                    }
                    .onFailure { error ->
                        dispatch(Msg.LoadFailed(error.message ?: "加载更多失败"))
                    }
            }
        }

        private fun reset() {
            dispatch(Msg.StateReset)
        }

        private fun updateScrollPosition(intent: Intent.UpdateScrollPosition) {
            val position = ScrollPosition(intent.firstVisibleIndex, intent.offset)
            val timestamp = Clock.System.now().toEpochMilliseconds()
            dispatch(Msg.ScrollPositionUpdated(position, timestamp))
        }
    }

    private object ReducerImpl : Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State = when (msg) {
            is Msg.StateReset -> State()

            is Msg.InitialLoadingStarted -> copy(
                isInitialLoading = true,
                error = null
            )

            is Msg.LoadMoreStarted -> copy(
                isLoadingMore = true,
                error = null
            )

            is Msg.ImagesLoaded -> {
                val record = ImageHistoryRecord(
                    type = ImageHistoryType.Load(
                        loadType = msg.loadType,
                        images = msg.images,
                        totalCount = msg.totalCount,
                        hasMore = msg.hasMore
                    ),
                    timestamp = msg.timestamp
                )
                record.applyToState(this)
            }

            is Msg.LoadFailed -> copy(
                isInitialLoading = false,
                isLoadingMore = false,
                error = msg.error
            )

            is Msg.ScrollPositionUpdated -> {
                val record = ImageHistoryRecord(
                    type = ImageHistoryType.Scroll(msg.position),
                    timestamp = msg.timestamp
                )
                record.applyToState(this)
            }
        }
    }
}
