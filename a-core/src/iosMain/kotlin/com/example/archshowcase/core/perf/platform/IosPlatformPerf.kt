package com.example.archshowcase.core.perf.platform

import com.example.archshowcase.core.perf.model.DeviceInfo
import com.example.archshowcase.core.perf.model.MemoryInfo
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIDevice
import platform.UIKit.UIScreen

/**
 * iOS 无法直接获取进程启动时的 uptime。
 * 在最早的 KMP 入口（initKoin）调用 markProcessStart() 记录近似值，
 * 与 currentUptimeMs() 在同一时钟域，差值有意义。
 */
private var processStartUptime: Long = 0L

fun markProcessStart() {
    if (processStartUptime == 0L) {
        processStartUptime = (NSProcessInfo.processInfo.systemUptime * 1000).toLong()
    }
}

actual fun processStartTimeMs(): Long = processStartUptime

actual fun currentUptimeMs(): Long =
    (NSProcessInfo.processInfo.systemUptime * 1000).toLong()

actual fun deviceRefreshRate(): Float {
    return UIScreen.mainScreen.maximumFramesPerSecond.toFloat()
}

actual fun deviceInfo(): DeviceInfo {
    val device = UIDevice.currentDevice
    val processInfo = NSProcessInfo.processInfo
    return DeviceInfo(
        model = device.model,
        os = "${device.systemName} ${device.systemVersion}",
        ramMb = (processInfo.physicalMemory / (1024u * 1024u)).toInt(),
        refreshRate = deviceRefreshRate()
    )
}

actual fun currentLooperMessage(): String? = null

actual fun snapshotFrameDiagnostics(): FrameDiagnostics = FrameDiagnostics()

actual fun markPendingJank(vsyncNs: Long, frameIndex: Long, severity: com.example.archshowcase.core.perf.model.JankSeverity) {}

actual fun currentVsyncNs(): Long = 0L

actual fun currentFrameIndex(): Long = 0L

actual fun memoryInfo(): MemoryInfo {
    val processInfo = NSProcessInfo.processInfo
    val totalMb = (processInfo.physicalMemory / (1024u * 1024u)).toInt()
    // iOS doesn't expose per-process heap info easily; report total device memory
    return MemoryInfo(usedMb = 0, totalMb = totalMb, freePercent = 0f)
}
