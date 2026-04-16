package com.example.archshowcase.core.analytics

import com.example.archshowcase.core.analytics.model.AnalyticsEvent
import com.example.archshowcase.core.util.Log

/** 埋点上报接口，具体后端实现由调用方注入 */
fun interface AnalyticsReporter {
    fun report(events: List<AnalyticsEvent>)
}

/** 默认空实现，静默丢弃 */
object NoOpReporter : AnalyticsReporter {
    override fun report(events: List<AnalyticsEvent>) = Unit
}

/** 日志输出实现，开发阶段用于验证埋点管线 */
class LoggingReporter(private val tag: String = "Analytics") : AnalyticsReporter {
    override fun report(events: List<AnalyticsEvent>) {
        events.forEach { event ->
            Log.d(tag) { format(event) }
        }
    }

    private fun format(event: AnalyticsEvent): String = when (event) {
        is AnalyticsEvent.Intent ->
            "Intent | ${event.route} | ${event.storeName}.${event.intentName} | ${event.gestureType} | ${event.params}"
        is AnalyticsEvent.Page ->
            "Page | ${event.route} | ${event.action} | from=${event.fromRoute} | nav=${event.navigationAction} | ${event.durationMs}ms"
        is AnalyticsEvent.Exposure ->
            "Exposure | ${event.route} | ${event.componentType}:${event.exposureKey} | list=${event.listId} | ${event.params}"
    }
}
