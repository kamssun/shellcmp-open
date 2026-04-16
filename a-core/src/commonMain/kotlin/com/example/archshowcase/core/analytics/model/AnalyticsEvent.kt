package com.example.archshowcase.core.analytics.model

import com.example.archshowcase.core.perf.platform.currentUptimeMs

/** 统一事件模型：Intent（用户交互）、Page（页面进入/离开）、Exposure（元素曝光） */
sealed class AnalyticsEvent {
    abstract val route: String
    abstract val timestamp: Long
    abstract val sessionId: String

    /** 用户交互 → 业务动作 */
    data class Intent(
        override val route: String,
        override val timestamp: Long = currentUptimeMs(),
        override val sessionId: String = "",
        val storeName: String,
        val intentName: String,
        val gestureType: GestureType,
        val params: Map<String, String> = emptyMap(),
    ) : AnalyticsEvent()

    /** 页面进入/离开/前后台 */
    data class Page(
        override val route: String,
        override val timestamp: Long = currentUptimeMs(),
        override val sessionId: String = "",
        val action: PageAction,
        val fromRoute: String = "",
        val navigationAction: NavigationAction = NavigationAction.UNKNOWN,
        val durationMs: Long = 0L,
    ) : AnalyticsEvent()

    /** 元素曝光 */
    data class Exposure(
        override val route: String,
        override val timestamp: Long = currentUptimeMs(),
        override val sessionId: String = "",
        val componentType: String,
        val exposureKey: String,
        val listId: String = "",
        val params: Map<String, String> = emptyMap(),
    ) : AnalyticsEvent()
}

enum class GestureType {
    TAP, DOUBLE_TAP, LONG_PRESS, SWITCH, CHIP, IME_ACTION, UNKNOWN
}

enum class PageAction {
    ENTER, LEAVE, BACKGROUND, FOREGROUND
}

enum class NavigationAction {
    PUSH, POP, REPLACE_ALL, UNKNOWN
}
