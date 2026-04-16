package com.example.archshowcase.replayable

/**
 * 标记 sealed interface 自动生成路由序列化代码
 *
 * 生成内容：
 * - `serialName` 扩展属性（穷举 when，编译器保证完整性）
 * - `fromSerialName` 伴生扩展函数（O(1) 查找 + 参数解析）
 *
 * @param fallback fromSerialName 未匹配时的回退路由名（空字符串取第一个 data object）
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class RouteRegistry(val fallback: String = "")
