package com.example.archshowcase.core.perf.platform

import android.app.ActivityManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Process
import android.os.SystemClock
import com.example.archshowcase.core.perf.model.DeviceInfo
import com.example.archshowcase.core.perf.model.MemoryInfo
import com.example.archshowcase.core.util.ContextProvider

actual fun processStartTimeMs(): Long =
    Process.getStartElapsedRealtime()

actual fun currentUptimeMs(): Long =
    SystemClock.elapsedRealtime()

actual fun deviceRefreshRate(): Float {
    val context = ContextProvider.applicationContext
    val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    return dm?.getDisplay(0)?.refreshRate ?: 60f
}

actual fun deviceInfo(): DeviceInfo {
    val context = ContextProvider.applicationContext
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    am?.getMemoryInfo(memInfo)
    return DeviceInfo(
        model = "${Build.MANUFACTURER} ${Build.MODEL}",
        os = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        ramMb = (memInfo.totalMem / (1024 * 1024)).toInt(),
        refreshRate = deviceRefreshRate()
    )
}

actual fun currentLooperMessage(): String? {
    val messages = com.example.archshowcase.core.perf.jank.MessageMonitor.slowMessages
    return if (messages.isEmpty()) null else messages.joinToString("; ")
}

actual fun snapshotFrameDiagnostics(): FrameDiagnostics =
    com.example.archshowcase.core.perf.jank.FrameDiagnosticsCollector.snapshot()

actual fun markPendingJank(vsyncNs: Long, frameIndex: Long, severity: com.example.archshowcase.core.perf.model.JankSeverity) {
    // atrace 标记：Perfetto 中搜索 "PERF:JANK" 直接定位卡顿帧
    android.os.Trace.beginSection("PERF:JANK #$frameIndex")
    android.os.Trace.endSection()
    com.example.archshowcase.core.perf.jank.FrameMetricsCollector.markPendingJank(vsyncNs, frameIndex, severity)
}

actual fun currentVsyncNs(): Long =
    com.example.archshowcase.core.perf.jank.MessageMonitor.lastFrameVsyncNs

actual fun currentFrameIndex(): Long =
    com.example.archshowcase.core.perf.jank.MessageMonitor.frameIndex

actual fun memoryInfo(): MemoryInfo {
    val runtime = Runtime.getRuntime()
    val totalMb = (runtime.maxMemory() / (1024 * 1024)).toInt()
    val usedMb = ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)).toInt()
    val freePercent = if (totalMb > 0) (totalMb - usedMb).toFloat() / totalMb * 100f else 0f
    return MemoryInfo(usedMb = usedMb, totalMb = totalMb, freePercent = freePercent)
}
