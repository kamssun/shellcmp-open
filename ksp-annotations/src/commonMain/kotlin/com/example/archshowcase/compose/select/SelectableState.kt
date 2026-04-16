package com.example.archshowcase.compose.select

/**
 * 标记 Store.State data class，KSP 自动生成 StateFields holder + rememberFields() 扩展。
 *
 * 使用方式：在 Store 内部的 State data class 上标注：
 * ```kotlin
 * interface MyStore : Store<Intent, State, Nothing> {
 *     @SelectableState
 *     data class State(val count: Int = 0)
 * }
 * ```
 *
 * 生成产物：
 * - `MyStoreStateFields` — 持有 `State<T>` 委托属性
 * - `StateFlow<MyStore.State>.rememberFields()` — Composable 扩展
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class SelectableState
