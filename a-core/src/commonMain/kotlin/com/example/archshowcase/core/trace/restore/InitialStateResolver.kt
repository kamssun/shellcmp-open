package com.example.archshowcase.core.trace.restore

import com.arkivanov.mvikotlin.core.store.StoreEventType
import com.arkivanov.mvikotlin.timetravel.TimeTravelState
import com.arkivanov.mvikotlin.timetravel.controller.timeTravelController
import com.example.archshowcase.core.AppConfig
import com.example.archshowcase.core.AppRuntimeState

/**
 * 初始状态解析器
 *
 * 根据 TimeTravel 状态决定 Store 的初始 State。
 * MVIKotlin TimeTravel 的依赖收拢于此。
 *
 * 策略：
 * 1. TimeTravel 活动时，从当前位置获取状态
 * 2. TimeTravel 中没有该 Store 的事件 → 使用缓存（新页面）
 * 3. 当前位置在第一个 STATE 之前：
 *    - wasRegistered=true（Move to Start 后）→ 返回 null（初始状态）
 *    - wasRegistered=false（首次导入）→ 使用缓存（最终状态）
 * 4. 都没有 → 返回 null
 */
object InitialStateResolver {

    @Suppress("UNCHECKED_CAST")
    fun <S : RestorableState> resolve(storeName: String): S? {
        if (!AppConfig.enableRestore) return null
        val ttState = timeTravelController.state
        val wasRegistered = RestoreRegistry.wasEverRegistered(storeName)

        // 非回溯中
        if (ttState.mode == TimeTravelState.Mode.IDLE) {
            // 验证模式：从 RestoreRegistry 读取预填充状态
            if (AppRuntimeState.verificationMode) {
                val cached = RestoreRegistry.getSnapshot<S>(storeName)
                return if (cached?.hasValidData() == true) cached else null
            }
            return null
        }

        val currentIndex = ttState.selectedEventIndex
        val firstStateIndex = ttState.events.indexOfFirst {
            it.storeName == storeName && it.type == StoreEventType.STATE
        }

        // TimeTravel 中没有该 Store 的 STATE 事件 → 新页面，使用缓存
        if (firstStateIndex == -1) {
            val cached = RestoreRegistry.getSnapshot<S>(storeName)
            return if (cached?.hasValidData() == true) cached else null
        }

        // 当前位置在该 Store 第一个 STATE 之前
        if (currentIndex < firstStateIndex) {
            // Move to Start 后重建 → 使用初始状态
            if (wasRegistered) return null
            // 首次导入 → fall through 使用缓存
        } else {
            // 从当前位置获取状态
            val stateFromTT = ttState.events
                .take(currentIndex + 1)
                .lastOrNull { it.storeName == storeName && it.type == StoreEventType.STATE }
                ?.value as? S
            if (stateFromTT?.hasValidData() == true) return stateFromTT
        }

        val cached = RestoreRegistry.getSnapshot<S>(storeName)
        return if (cached?.hasValidData() == true) cached else null
    }
}
