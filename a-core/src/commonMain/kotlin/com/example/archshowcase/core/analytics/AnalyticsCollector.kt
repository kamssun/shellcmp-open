package com.example.archshowcase.core.analytics

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnResume
import com.example.archshowcase.core.analytics.model.AnalyticsEvent
import com.example.archshowcase.core.analytics.model.NavigationAction
import com.example.archshowcase.core.analytics.model.PageAction
import com.example.archshowcase.core.compose.exposure.ExposureTracker
import com.example.archshowcase.core.perf.platform.currentUptimeMs
import com.example.archshowcase.core.util.Log
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * 统一埋点收集管线：collect → RingBuffer → 采样 → flushIfNeeded → Reporter。
 *
 * 非线程安全，仅主线程调用。
 */
object AnalyticsCollector {

    private var config = AnalyticsConfig()
    private var buffer = RingBuffer<AnalyticsEvent>(config.bufferCapacity)
    private var lastFlushMs = 0L
    private var sessionId: String = ""

    /** KSP 生成的 EventMapperRegistry.map 引用，由 AnalyticsSetup 注入 */
    var eventMapper: ((storeName: String, intent: Any) -> TrackingEvent?)? = null
        internal set

    var currentRoute: String = ""
        private set

    fun trackPage(ctx: ComponentContext, route: String) {
        var pageEnterMs: Long? = null
        ctx.lifecycle.doOnResume {
            val previousRoute = currentRoute
            currentRoute = route
            ExposureTracker.resetPage()
            pageEnterMs = currentUptimeMs()
            collect(AnalyticsEvent.Page(
                route = route,
                action = PageAction.ENTER,
                fromRoute = previousRoute,
                navigationAction = NavigationActionContext.current,
            ))
            NavigationActionContext.current = NavigationAction.UNKNOWN
        }
        ctx.lifecycle.doOnPause {
            val enterMs = pageEnterMs ?: return@doOnPause
            val durationMs = currentUptimeMs() - enterMs
            collect(AnalyticsEvent.Page(
                route = route,
                action = PageAction.LEAVE,
                durationMs = durationMs,
                navigationAction = NavigationActionContext.current,
            ))
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun start(config: AnalyticsConfig) {
        this.config = config
        this.buffer = RingBuffer(config.bufferCapacity)
        this.lastFlushMs = currentUptimeMs()
        this.sessionId = Uuid.random().toString()
    }

    fun collect(event: AnalyticsEvent) {
        if (!config.enabled) return
        if (sessionId.isEmpty()) return
        if (!EventSampler.shouldSample(event, config)) return
        buffer.add(enrichSessionId(sanitizeParams(event)))
        flushIfNeeded()
    }

    /** 统一入口脱敏：所有事件类型的 params 均经过 RuntimeSanitizer */
    private fun sanitizeParams(event: AnalyticsEvent): AnalyticsEvent {
        val params = when (event) {
            is AnalyticsEvent.Intent -> event.params
            is AnalyticsEvent.Exposure -> event.params
            is AnalyticsEvent.Page -> return event // Page 无业务 params
        }
        if (params.isEmpty()) return event
        val sanitized = RuntimeSanitizer.sanitize(params)
        return when (event) {
            is AnalyticsEvent.Intent -> event.copy(params = sanitized)
            is AnalyticsEvent.Exposure -> event.copy(params = sanitized)
            is AnalyticsEvent.Page -> event
        }
    }

    private fun enrichSessionId(event: AnalyticsEvent): AnalyticsEvent = when (event) {
        is AnalyticsEvent.Intent -> event.copy(sessionId = sessionId)
        is AnalyticsEvent.Page -> event.copy(sessionId = sessionId)
        is AnalyticsEvent.Exposure -> event.copy(sessionId = sessionId)
    }

    fun flush() {
        if (buffer.isEmpty) return
        val batch = buffer.drain()
        try {
            config.reporter.report(batch)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("AnalyticsCollector") { "flush failed: ${e.message}" }
        }
        lastFlushMs = currentUptimeMs()
    }

    private fun flushIfNeeded() {
        val shouldFlush = buffer.size >= config.batchSize ||
            (currentUptimeMs() - lastFlushMs >= config.flushIntervalMs && !buffer.isEmpty)
        if (shouldFlush) flush()
    }

    /** 测试用重置 */
    internal fun reset() {
        config = AnalyticsConfig()
        buffer = RingBuffer(config.bufferCapacity)
        lastFlushMs = 0L
        sessionId = ""
        eventMapper = null
        currentRoute = ""
    }
}
