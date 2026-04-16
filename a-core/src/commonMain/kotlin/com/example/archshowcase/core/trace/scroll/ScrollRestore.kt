package com.example.archshowcase.core.trace.scroll

import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.example.archshowcase.core.trace.restore.RestorableState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * 通用滚动位置，用于 TimeTravel 回放
 */
@Serializable
data class ScrollPosition(
    val firstVisibleIndex: Int = 0,
    val offset: Int = 0
) : JvmSerializable

/**
 * 支持滚动恢复的 State 需要实现此接口
 */
interface ScrollRestorableState : RestorableState {
    val scrollPosition: ScrollPosition
}

/**
 * 滚动恢复辅助类
 *
 * 用于 Component 层，管理 TimeTravel 回放时的滚动位置恢复。
 */
class ScrollRestoreHelper(private val scope: CoroutineScope) {

    private val _scrollRestoreEvent = MutableSharedFlow<ScrollPosition>(replay = 1)
    val scrollRestoreEvent: SharedFlow<ScrollPosition> = _scrollRestoreEvent

    fun <I : Any, S : Any, L : Any> observe(
        store: Store<I, S, L>,
        getScrollPosition: (S) -> ScrollPosition,
        getItemCount: () -> Int,
        isUserScrolling: () -> Boolean
    ) {
        scope.launch {
            store.states
                .map { getScrollPosition(it) }
                .distinctUntilChanged()
                .collect { position ->
                    // 只要不是用户滚动，且列表有数据，就尝试恢复滚动位置
                    if (!isUserScrolling() && getItemCount() > 0) {
                        _scrollRestoreEvent.emit(position)
                    }
                }
        }
    }

    fun <I : Any, S : ScrollRestorableState, L : Any> observe(
        store: Store<I, S, L>,
        getItemCount: () -> Int,
        isUserScrolling: () -> Boolean
    ) {
        observe(
            store = store,
            getScrollPosition = { it.scrollPosition },
            getItemCount = getItemCount,
            isUserScrolling = isUserScrolling
        )
    }
}
