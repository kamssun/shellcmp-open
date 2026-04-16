package com.example.archshowcase.core.analytics

import com.example.archshowcase.core.analytics.model.AnalyticsEvent
import com.example.archshowcase.core.analytics.model.PageAction
import com.example.archshowcase.core.compose.exposure.ExposureTracker

/**
 * App 前后台生命周期追踪。
 * 进入后台：提交 BACKGROUND 事件 + 暂停曝光 + flush 缓冲区。
 * 回到前台：提交 FOREGROUND 事件 + 恢复曝光。
 */
expect object AppLifecycleTracker {
    fun start()
    fun stop()
}

/** 前后台切换的公共逻辑 */
internal fun onAppBackground() {
    val route = AnalyticsCollector.currentRoute
    AnalyticsCollector.collect(
        AnalyticsEvent.Page(route = route, action = PageAction.BACKGROUND)
    )
    ExposureTracker.pauseAll()
    AnalyticsCollector.flush()
}

internal fun onAppForeground() {
    val route = AnalyticsCollector.currentRoute
    // 首次启动时 currentRoute 尚未设置，跳过无意义的空 route FOREGROUND 事件
    if (route.isNotEmpty()) {
        AnalyticsCollector.collect(
            AnalyticsEvent.Page(route = route, action = PageAction.FOREGROUND)
        )
    }
    ExposureTracker.resumeAll()
}
