package com.example.archshowcase.replayable

import kotlin.reflect.KClass

/**
 * 注册自定义参数类型的字符串解析适配器
 *
 * 用于 @VfResolvable 处理器：当 Intent 参数类型不是基本类型（String/Int/Boolean/Long/Float）
 * 或 Enum 时，需要通过此注解指定如何从字符串解析。
 *
 * **使用方式：**
 * ```kotlin
 * @VfParamAdapter(forType = Route::class, fromString = "Route.fromSerialName")
 * @VfParamAdapter(forType = MyType::class, fromString = "MyType.parse")
 * object VfParamAdapters
 * ```
 *
 * @param forType 目标类型
 * @param fromString 静态方法全名，接收 String 返回目标类型
 */
@Repeatable
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class VfParamAdapter(
    val forType: KClass<*>,
    val fromString: String
)
