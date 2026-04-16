package com.example.archshowcase.core.perf

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnResume
import com.example.archshowcase.core.perf.jank.ContextCollector
import com.example.archshowcase.core.perf.jank.JankTracker
import com.example.archshowcase.core.perf.model.JankEvent
import com.example.archshowcase.core.perf.model.PageFrameMetrics
import com.example.archshowcase.core.perf.model.StartupTrace
import com.example.archshowcase.core.perf.page.PageTracker
import com.example.archshowcase.core.perf.platform.FrameMonitor
import com.example.archshowcase.core.perf.platform.deviceInfo
import com.example.archshowcase.core.perf.platform.memoryInfo
import com.example.archshowcase.core.perf.report.CompositeReporter
import com.example.archshowcase.core.perf.report.LogReporter
import com.example.archshowcase.core.perf.report.PerfReporter
import com.example.archshowcase.core.perf.report.SamplingReporter

object PerfMonitor {

    private var config: PerfConfig = PerfConfig()
    private var reporter: PerfReporter = LogReporter()
    private var frameMonitor: FrameMonitor? = null
    private var jankTracker: JankTracker? = null

    var pageTracker: PageTracker? = null
        private set

    var isRunning: Boolean = false
        private set

    fun start(config: PerfConfig) {
        if (!config.enabled || isRunning) return
        this.config = config
        isRunning = true

        reporter = buildReporter(config)

        val tracker = PageTracker(
            deviceInfoProvider = { deviceInfo() },
            onPageMetrics = { reportPageMetrics(it) }
        )
        pageTracker = tracker

        if (config.jankTrackingEnabled) {
            val contextCollector = ContextCollector(
                config = config,
                intentProvider = { com.example.archshowcase.core.trace.user.IntentTracker.snapshot() },
                snapshotProvider = { com.example.archshowcase.core.trace.restore.RestoreRegistry.getAllSnapshots() },
                memoryProvider = { memoryInfo() }
            )
            val jank = JankTracker(
                contextCollector = contextCollector,
                pageTracker = tracker,
                deviceInfoProvider = { deviceInfo() },
                onJank = { event ->
                    tracker.recordJank(event)
                    reportJank(event)
                }
            )
            jankTracker = jank

            val monitor = FrameMonitor()
            frameMonitor = monitor
            monitor.start { timing ->
                tracker.onFrame(timing)
                jank.onFrame(timing)
            }
        }
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        frameMonitor?.stop()
        frameMonitor = null
        jankTracker = null
        pageTracker = null
    }

    fun reportStartup(trace: StartupTrace) {
        if (!config.enabled || !config.startupTracingEnabled) return
        reporter.reportStartup(trace)
    }

    fun reportJank(event: JankEvent) {
        if (!config.enabled || !config.jankTrackingEnabled) return
        reporter.reportJank(event)
    }

    fun reportPageMetrics(metrics: PageFrameMetrics) {
        if (!config.enabled) return
        reporter.reportPageMetrics(metrics)
    }

    fun reportProfileStatus(status: String) {
        if (!config.enabled) return
        reporter.reportProfileStatus(status)
    }

    /**
     * 在 childFactory 中一行注册页面生命周期追踪。
     *
     * 自动处理：
     * - doOnResume → PageTracker.onTransitionEnd
     * - doOnPause  → PageTracker.onPagePaused + reportPageMetrics
     */
    fun trackPage(ctx: ComponentContext, route: String) {
        ctx.lifecycle.doOnResume {
            pageTracker?.onTransitionEnd(route)
        }
        ctx.lifecycle.doOnPause {
            val metrics = pageTracker?.onPagePaused()
            if (metrics != null) reportPageMetrics(metrics)
        }
    }

    /**
     * 通知路由切换开始（由 NavigationStackManager 调用）。
     */
    fun notifyTransitionStart(from: String, to: String) {
        pageTracker?.onTransitionStart(from, to)
    }

    private fun buildReporter(config: PerfConfig): PerfReporter {
        val composite = if (config.reporters.size == 1) config.reporters.first() else CompositeReporter(config.reporters)
        return if (config.samplingRate < 1.0f) {
            SamplingReporter(composite, config.samplingRate)
        } else {
            composite
        }
    }

    internal fun reset() {
        stop()
        config = PerfConfig()
        reporter = LogReporter()
    }
}
