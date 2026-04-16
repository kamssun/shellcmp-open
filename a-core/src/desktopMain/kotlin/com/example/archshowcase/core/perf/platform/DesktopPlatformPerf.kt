package com.example.archshowcase.core.perf.platform

import com.example.archshowcase.core.perf.model.DeviceInfo
import com.example.archshowcase.core.perf.model.MemoryInfo

private val processStartNanos: Long = System.nanoTime()

actual fun processStartTimeMs(): Long = 0L

actual fun currentUptimeMs(): Long =
    (System.nanoTime() - processStartNanos) / 1_000_000

actual fun deviceRefreshRate(): Float = 60f

actual fun deviceInfo(): DeviceInfo = DeviceInfo(
    model = "Desktop",
    os = System.getProperty("os.name") + " " + System.getProperty("os.version"),
    ramMb = (Runtime.getRuntime().maxMemory() / (1024 * 1024)).toInt(),
    refreshRate = 60f
)

actual fun currentLooperMessage(): String? = null

actual fun snapshotFrameDiagnostics(): FrameDiagnostics = FrameDiagnostics()

actual fun markPendingJank(vsyncNs: Long, frameIndex: Long, severity: com.example.archshowcase.core.perf.model.JankSeverity) {}

actual fun currentVsyncNs(): Long = 0L

actual fun currentFrameIndex(): Long = 0L

actual fun memoryInfo(): MemoryInfo {
    val runtime = Runtime.getRuntime()
    val totalMb = (runtime.maxMemory() / (1024 * 1024)).toInt()
    val usedMb = ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)).toInt()
    val freePercent = if (totalMb > 0) (totalMb - usedMb).toFloat() / totalMb * 100f else 0f
    return MemoryInfo(usedMb = usedMb, totalMb = totalMb, freePercent = freePercent)
}
