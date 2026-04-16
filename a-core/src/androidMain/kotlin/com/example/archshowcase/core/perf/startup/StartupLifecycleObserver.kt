package com.example.archshowcase.core.perf.startup

import android.view.Choreographer
import androidx.activity.ComponentActivity
import com.example.archshowcase.core.perf.PerfMonitor
import com.example.archshowcase.core.perf.jank.FrameMetricsCollector
import com.example.archshowcase.core.perf.platform.deviceInfo

/**
 * Activity 层启动阶段自动打点。
 *
 * 自动覆盖阶段：activity_create（包裹回调）+ first_frame（Choreographer 回调）。
 * 回调内部的子阶段（root_component / compose_setup）由调用方自行用 `StartupTracer.traced` 标记。
 */
object StartupLifecycleObserver {

    /**
     * @param activity 目标 Activity
     * @param block 业务方回调：在 activity_create 阶段内执行（创建 RootComponent + setContent）
     */
    fun install(
        activity: ComponentActivity,
        block: () -> Unit
    ) {
        FrameMetricsCollector.install(activity)

        StartupTracer.traced("activity_create") {
            block()
        }

        StartupTracer.markPhaseStart("first_frame")
        Choreographer.getInstance().postFrameCallback {
            // 零时长 trace marker：让 first_frame 时间点在 Perfetto 中可见
            android.os.Trace.beginSection("first_frame")
            android.os.Trace.endSection()

            StartupTracer.markPhaseEnd("first_frame")
            val trace = StartupTracer.finish(deviceInfo())
            if (trace != null) {
                PerfMonitor.reportStartup(trace)
            }
        }
    }
}
