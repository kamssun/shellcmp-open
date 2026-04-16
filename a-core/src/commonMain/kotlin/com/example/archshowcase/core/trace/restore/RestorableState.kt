package com.example.archshowcase.core.trace.restore

import com.arkivanov.mvikotlin.core.utils.JvmSerializable

/**
 * 可恢复状态的标记接口
 *
 * Store 的 State 实现此接口表示支持 TimeTravel 恢复。
 */
interface RestorableState : JvmSerializable {
    /**
     * 检查状态是否有效（有数据需要恢复）
     */
    fun hasValidData(): Boolean
}

