package com.example.archshowcase.replayable

import kotlin.reflect.KClass

/**
 * 标记 HistoryRecord 支持回放
 *
 * **使用方式：**
 * ```kotlin
 * @Replayable
 * data class MyHistoryRecord(...) {
 *     fun applyToState(prevState: State): State = ...
 *     fun toIntent(): Any = ...
 * }
 * ```
 *
 * **参数说明：**
 * - `stateClass`: 手动指定 State 类型（通常自动推断）
 * - `storeName`: 自定义 Store 名称（为空时从父类推导）
 * - `generateStoreHelpers`: 是否生成 StoreFactory 辅助代码（默认 true）
 * - `generateExportStrategy`: 是否生成 ExportStrategy（默认 true）
 *
 * 支持 `@Repeatable`：同一 Record 可标注多个 `@Replayable`，为不同 Store 生成独立代码。
 */
@Repeatable
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Replayable(
    val stateClass: KClass<*> = Nothing::class,
    val storeName: String = "",
    val generateStoreHelpers: Boolean = true,
    val generateExportStrategy: Boolean = true
)
