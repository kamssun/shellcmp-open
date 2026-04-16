package com.example.archshowcase.compose.select

/**
 * 组合注解：同时触发 @SelectableState + @MemoryTrackable 的代码生成。
 *
 * 生成产物：
 * - {Store}StateFields + rememberFields()（来自 SelectableState 处理器）
 * - {Store}MemorySnapshot + MemorySnapshotRegistry（来自 MemoryTrackable 处理器）
 *
 * 使用方式：替代 @SelectableState，一个注解覆盖 UI 订阅 + 内存追踪（Store.State 统一标注）。
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class CustomState
