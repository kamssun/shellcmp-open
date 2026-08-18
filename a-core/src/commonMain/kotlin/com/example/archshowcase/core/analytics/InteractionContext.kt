package com.example.archshowcase.core.analytics

import com.example.archshowcase.core.analytics.model.GestureType

/**
 * 交互上下文底层实现：AppInteraction 写入，TrackingExecutor 读取。
 *
 * 业务层只使用 App* 组件或 appClickable 系列 API，不直接接触本对象。
 */
internal object InteractionContext {

    private var gesture: GestureInfo? = null
    private var depth = 0

    private var savedGestures = ArrayDeque<GestureInfo>()

    /** 交互入口回调开始时写入上下文 */
    @PublishedApi
    internal fun markUserGesture(component: String, gestureType: GestureType = GestureType.TAP) {
        if (depth > 0 && gesture != null) {
            savedGestures.add(gesture!!)
        }
        gesture = GestureInfo(component, gestureType)
        depth++
    }

    /** 交互入口回调结束时清除上下文 */
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

internal data class GestureInfo(
    val component: String,
    val gestureType: GestureType,
)

internal inline fun withUserGesture(
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
