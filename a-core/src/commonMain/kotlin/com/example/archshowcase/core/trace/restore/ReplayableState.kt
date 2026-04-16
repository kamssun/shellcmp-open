package com.example.archshowcase.core.trace.restore

import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.example.archshowcase.core.AppConfig

/**
 * 可回放状态接口
 *
 * State 实现此接口表示支持 TimeTravel 回放和自动导出。
 * KSP 会为 Record 自动生成 applyToState() 和 toIntent() 扩展函数。
 *
 * 使用方式：
 * 1. Record 数据类添加 @Replayable 注解
 * 2. State 实现此接口
 * 3. KSP 自动生成扩展函数
 * 4. Factory 的 Reducer 调用生成的扩展函数
 *
 * @param R Record 类型（必须包含 timestamp 字段）
 */
interface ReplayableState<R : JvmSerializable> : RestorableState {
    /**
     * 历史记录列表（仅 Debug 模式）
     */
    val history: AppendOnlyHistory<R>

    /**
     * 创建初始状态
     */
    fun createInitialState(): ReplayableState<R>
}

/** enableRestore 时追加历史记录，否则返回空。使用 AppendOnlyHistory 共享底座，O(1) 追加。 */
fun <R : JvmSerializable> ReplayableState<R>.appendHistory(record: R): AppendOnlyHistory<R> =
    if (AppConfig.enableRestore) history.append(record) else AppendOnlyHistory.empty()
