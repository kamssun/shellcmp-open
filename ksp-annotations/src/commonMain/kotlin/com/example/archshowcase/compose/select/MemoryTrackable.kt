package com.example.archshowcase.compose.select

/**
 * 标记 Store.State data class，KSP 自动生成 memorySnapshot() 扩展 + MemorySnapshotRegistry。
 *
 * 递归扫描 State 及其嵌套对象中所有 Collection/Map 字段的 .size，
 * 用于 GC 压力检测时快速定位内存大户。
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class MemoryTrackable
