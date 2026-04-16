package com.example.archshowcase.core.analytics

import com.example.archshowcase.core.analytics.model.GestureType

/**
 * 交互上下文：App* 组件 onClick 同步写入，TrackingExecutor 读取判定用户行为。
 *
 * 调用栈标记机制：markUserGesture/endUserGesture 配对使用，depth 追踪嵌套深度。
 * 全链路在主线程，无需 ThreadLocal / AtomicReference。
 */
object InteractionContext {

    private var gesture: GestureInfo? = null
    private var depth = 0

    private var savedGestures = ArrayDeque<GestureInfo>()

    /** App* 组件 onClick 开头调用，写入交互上下文 */
    @PublishedApi
    internal fun markUserGesture(component: String, gestureType: GestureType = GestureType.TAP) {
        if (depth > 0 && gesture != null) {
            savedGestures.add(gesture!!)
        }
        gesture = GestureInfo(component, gestureType)
        depth++
    }

    /** App* 组件 onClick 结束时调用，清除交互上下文 */
    @PublishedApi
    internal fun endUserGesture() {
        if (--depth <= 0) {
            depth = 0
            gesture = null
            savedGestures.clear()
        } else {
            gesture = savedGestures.removeLastOrNull()
        }
    }

    /** TrackingExecutor 检查是否在用户操作调用栈内 */
    fun isUserInitiated(): Boolean = gesture != null

    /** 获取当前交互信息，不在调用栈内返回 null */
    fun currentGesture(): GestureInfo? = gesture

    /** 重置（仅测试用） */
    fun reset() {
        gesture = null
        depth = 0
        savedGestures.clear()
    }
}

data class GestureInfo(
    val component: String,
    val gestureType: GestureType,
)

inline fun withUserGesture(
    component: String,
    gestureType: GestureType = GestureType.TAP,
    block: () -> Unit,
) {
    InteractionContext.markUserGesture(component, gestureType)
    try {
        block()
    } finally {
        InteractionContext.endUserGesture()
    }
}
