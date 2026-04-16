package com.example.archshowcase.presentation.demo.image

import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.example.archshowcase.core.trace.restore.AppendOnlyHistory
import com.example.archshowcase.core.trace.restore.ReplayableState
import com.example.archshowcase.core.trace.scroll.ScrollPosition
import com.example.archshowcase.core.trace.scroll.ScrollRestorableState
import com.example.archshowcase.core.trace.user.UserTraceable
import com.example.archshowcase.network.dto.ImageItem
import com.example.archshowcase.compose.select.CustomState
import com.example.archshowcase.replayable.VfResolvable
import com.example.archshowcase.presentation.demo.image.ImageDemoStore.Intent
import com.example.archshowcase.presentation.demo.image.ImageDemoStore.State

@VfResolvable
interface ImageDemoStore : Store<Intent, State, Nothing> {

    sealed interface Intent : JvmSerializable {
        data object LoadInitial : Intent
        data object LoadMore : Intent
        data object Reset : Intent
        data class UpdateScrollPosition(val firstVisibleIndex: Int, val offset: Int) : Intent
    }

    @CustomState
    data class State(
        val images: List<ImageItem> = emptyList(),
        val totalCount: Int = 0,
        val isInitialLoading: Boolean = false,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = true,
        val error: String? = null,
        override val scrollPosition: ScrollPosition = ScrollPosition(),
        override val history: AppendOnlyHistory<ImageHistoryRecord> = AppendOnlyHistory()
    ) : ReplayableState<ImageHistoryRecord>, ScrollRestorableState {
        val isEmpty: Boolean get() = images.isEmpty() && !isInitialLoading
        val canLoadMore: Boolean get() = hasMore && !isLoadingMore && !isInitialLoading

        override fun hasValidData() = history.isNotEmpty()
        override fun createInitialState() = State()
    }

    sealed interface Msg : JvmSerializable {
        data object InitialLoadingStarted : Msg
        data object LoadMoreStarted : Msg
        data object StateReset : Msg
        data class ImagesLoaded(
            val images: List<ImageItem>,
            val totalCount: Int,
            val hasMore: Boolean,
            val loadType: String,
            val timestamp: Long
        ) : Msg, UserTraceable {
            override fun toTraceString() = "ImagesLoaded(count=${images.size}, total=$totalCount)"
        }
        data class LoadFailed(val error: String) : Msg
        data class ScrollPositionUpdated(val position: ScrollPosition, val timestamp: Long) : Msg
    }
}
