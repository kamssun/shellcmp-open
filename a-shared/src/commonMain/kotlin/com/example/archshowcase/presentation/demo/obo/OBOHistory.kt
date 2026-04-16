package com.example.archshowcase.presentation.demo.obo

import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.example.archshowcase.core.trace.restore.appendHistory
import com.example.archshowcase.core.trace.scroll.ScrollPosition
import com.example.archshowcase.replayable.Replayable
import kotlinx.serialization.Serializable

/**
 * OBODemo 的 ActionType
 * 用于回溯系统记录用户操作
 */
@Serializable
sealed interface OBOHistoryType : JvmSerializable {
    @Serializable
    data class SetEffects(val count: Int) : OBOHistoryType
    @Serializable
    data class SetBlockTime(val ms: Int) : OBOHistoryType
    @Serializable
    data object Reload : OBOHistoryType
    @Serializable
    data class ToggleOBO(val enabled: Boolean) : OBOHistoryType
    @Serializable
    data class Scroll(val position: ScrollPosition) : OBOHistoryType
}

/**
 * OBODemo 的 ActionRecord
 * 记录用户操作历史，用于时间旅行和回溯
 */
@Replayable(stateClass = OBODemoStore.State::class)
@Serializable
data class OBOHistoryRecord(
    val type: OBOHistoryType,
    val timestamp: Long
) : JvmSerializable {

    /**
     * 应用此记录到前一个状态，生成新状态
     */
    fun applyToState(prevState: OBODemoStore.State): OBODemoStore.State = when (type) {
        is OBOHistoryType.SetEffects -> prevState.copy(
            effectsPerItem = type.count,
            history = prevState.appendHistory(this)
        )
        is OBOHistoryType.SetBlockTime -> prevState.copy(
            blockTimeMs = type.ms,
            history = prevState.appendHistory(this)
        )
        OBOHistoryType.Reload -> prevState.copy(
            reloadTrigger = prevState.reloadTrigger + 1,
            history = prevState.appendHistory(this)
        )
        is OBOHistoryType.ToggleOBO -> prevState.copy(
            useOBO = type.enabled,
            history = prevState.appendHistory(this)
        )
        is OBOHistoryType.Scroll -> prevState.copy(
            scrollPosition = type.position,
            history = prevState.appendHistory(this)
        )
    }

    /**
     * 转换为 Intent 用于回放
     */
    fun toIntent(): Any = when (type) {
        is OBOHistoryType.SetEffects -> OBODemoStore.Intent.SetEffectsPerItem(type.count)
        is OBOHistoryType.SetBlockTime -> OBODemoStore.Intent.SetBlockTime(type.ms)
        OBOHistoryType.Reload -> OBODemoStore.Intent.Reload
        is OBOHistoryType.ToggleOBO -> OBODemoStore.Intent.ToggleOBO(type.enabled)
        is OBOHistoryType.Scroll -> OBODemoStore.Intent.UpdateScrollPosition(
            type.position.firstVisibleIndex,
            type.position.offset
        )
    }
}
