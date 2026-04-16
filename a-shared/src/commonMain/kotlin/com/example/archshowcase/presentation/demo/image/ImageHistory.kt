package com.example.archshowcase.presentation.demo.image

import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.example.archshowcase.core.trace.restore.appendHistory
import com.example.archshowcase.core.trace.scroll.ScrollPosition
import com.example.archshowcase.replayable.Replayable
import com.example.archshowcase.network.dto.ImageItem
import kotlinx.serialization.Serializable

/**
 * ImageDemo 的 ActionType
 * 用于回溯系统记录用户操作
 */
@Serializable
sealed interface ImageHistoryType : JvmSerializable {
    @Serializable
    data class Scroll(val position: ScrollPosition) : ImageHistoryType

    @Serializable
    data class Load(
        val loadType: String,
        val images: List<ImageItem>,  // 保存完整图片数据用于回溯
        val totalCount: Int,
        val hasMore: Boolean
    ) : ImageHistoryType
}

/**
 * ImageDemo 的 ActionRecord
 * 记录用户操作历史，用于时间旅行和回溯
 */
@Replayable(stateClass = ImageDemoStore.State::class)
@Serializable
data class ImageHistoryRecord(
    val type: ImageHistoryType,
    val timestamp: Long
) : JvmSerializable {

    /**
     * 应用此记录到前一个状态，生成新状态
     */
    fun applyToState(prevState: ImageDemoStore.State): ImageDemoStore.State = when (type) {
        is ImageHistoryType.Scroll -> prevState.copy(
            scrollPosition = type.position,
            history = prevState.appendHistory(this)
        )
        is ImageHistoryType.Load -> prevState.copy(
            images = type.images,
            totalCount = type.totalCount,
            hasMore = type.hasMore,
            isInitialLoading = false,
            isLoadingMore = false,
            error = null,
            history = prevState.appendHistory(this)
        )
    }

    /**
     * 转换为 Intent 用于回放
     */
    fun toIntent(): Any = when (type) {
        is ImageHistoryType.Scroll -> ImageDemoStore.Intent.UpdateScrollPosition(
            type.position.firstVisibleIndex,
            type.position.offset
        )
        is ImageHistoryType.Load -> when (type.loadType) {
            "initial" -> ImageDemoStore.Intent.LoadInitial
            "more" -> ImageDemoStore.Intent.LoadMore
            else -> ImageDemoStore.Intent.LoadInitial
        }
    }
}
