package com.example.archshowcase.core.perf.platform

import com.example.archshowcase.core.perf.model.DeviceInfo
import com.example.archshowcase.core.perf.model.JankSeverity
import com.example.archshowcase.core.perf.model.MemoryInfo

expect fun processStartTimeMs(): Long

expect fun currentUptimeMs(): Long

expect fun deviceRefreshRate(): Float

expect fun deviceInfo(): DeviceInfo

expect fun memoryInfo(): MemoryInfo

/** Android: Looper message 描述；其他平台: null */
expect fun currentLooperMessage(): String?

/** 帧间 GC / CPU 调度诊断；每帧调用一次，返回上一帧区间的诊断 */
expect fun snapshotFrameDiagnostics(): FrameDiagnostics

/** 标记卡顿帧等待 FrameMetrics 异步输出分解（Android 有效，其他平台 no-op） */
expect fun markPendingJank(vsyncNs: Long, frameIndex: Long, severity: JankSeverity)

/** 当前帧的 vsync timestamp（ns），由 FrameCallback 在主线程写入 */
expect fun currentVsyncNs(): Long

/** 当前帧序号（用于对齐 btrace） */
expect fun currentFrameIndex(): Long

data class FrameDiagnostics(
    /** 总 GC 次数（含并发 + 阻塞） */
    val gcCount: Int = 0,
    /** 总 GC 耗时（GC 线程时间，非主线程暂停时间） */
    val gcTimeMs: Long = 0,
    /** 阻塞式 GC 次数（全程暂停主线程） */
    val blockingGcCount: Int = 0,
    /** 阻塞式 GC 暂停总时长（直接导致卡顿） */
    val blockingGcTimeMs: Long = 0,
    /** 主线程 CPU 时间 */
    val cpuTimeMs: Long = 0,
    /** 墙钟时间 */
    val wallTimeMs: Long = 0
) {
    val cpuUsagePercent: Int
        get() = if (wallTimeMs > 0) ((cpuTimeMs * 100) / wallTimeMs).toInt().coerceIn(0, 100) else 100

    val hasGc: Boolean get() = gcCount > 0
}
