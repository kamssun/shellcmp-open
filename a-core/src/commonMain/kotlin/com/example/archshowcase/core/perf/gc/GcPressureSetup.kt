package com.example.archshowcase.core.perf.gc

/**
 * GC 压力检测的 Store 快照提供者（跨层桥接）。
 *
 * a-shared 层设置 provider，androidMain 的 GcPressureDetector 读取。
 * Desktop/iOS 无 GcPressureDetector，天然不会读取。
 */
object GcPressureSetup {
    var storeSnapshotProvider: (() -> Map<String, Map<String, Int>>)? = null
}
