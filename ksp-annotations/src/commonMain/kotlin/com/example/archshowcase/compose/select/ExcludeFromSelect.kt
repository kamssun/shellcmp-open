package com.example.archshowcase.compose.select

/**
 * 标记 State 字段，KSP 生成 rememberFields() 时跳过该字段，减少无效订阅。
 * ReplayableState、ScrollRestorableState的字段已经自动被排除，不需要加。
 * **不需要驱动 UI 更新，需要加此注解。**
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class ExcludeFromSelect
