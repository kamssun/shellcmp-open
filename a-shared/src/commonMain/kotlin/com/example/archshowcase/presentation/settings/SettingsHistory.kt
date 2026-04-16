package com.example.archshowcase.presentation.settings

import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.example.archshowcase.core.trace.restore.appendHistory
import com.example.archshowcase.replayable.Replayable
import kotlinx.serialization.Serializable

/**
 * Settings 的 HistoryType
 * 用于回溯系统记录用户操作
 */
@Serializable
sealed interface SettingsHistoryType : JvmSerializable {
    @Serializable
    data class SetOBOScheduler(val enabled: Boolean) : SettingsHistoryType
}

/**
 * Settings 的 HistoryRecord
 * 记录用户操作历史，用于时间旅行和回溯
 */
@Replayable(stateClass = SettingsStore.State::class)
@Serializable
data class SettingsHistoryRecord(
    val type: SettingsHistoryType,
    val timestamp: Long
) : JvmSerializable {

    /**
     * 应用此记录到前一个状态，生成新状态
     */
    fun applyToState(prevState: SettingsStore.State): SettingsStore.State = when (type) {
        is SettingsHistoryType.SetOBOScheduler -> prevState.copy(
            useOBOScheduler = type.enabled,
            history = prevState.appendHistory(this)
        )
    }

    /**
     * 转换为 Intent 用于回放
     */
    fun toIntent(): Any = when (type) {
        is SettingsHistoryType.SetOBOScheduler -> SettingsStore.Intent.SetOBOScheduler(type.enabled)
    }
}
