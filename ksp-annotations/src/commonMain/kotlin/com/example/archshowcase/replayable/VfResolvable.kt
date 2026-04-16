package com.example.archshowcase.replayable

/**
 * 标记 Store interface 自动生成 VF Intent 解析代码
 *
 * 生成内容：
 * - `resolve{Store}Intent(vfIntent: VfIntent): Store.Intent?` 解析函数
 * - 聚合到 `GeneratedIntentResolverRegistry`
 *
 * **使用方式：**
 * ```kotlin
 * @VfResolvable
 * interface ImageDemoStore : Store<Intent, State, Label> {
 *     sealed interface Intent : JvmSerializable {
 *         data object LoadInitial : Intent
 *         data class UpdateScrollPosition(val firstVisibleIndex: Int, val offset: Int) : Intent
 *     }
 * }
 * ```
 *
 * @param storeName 自定义 Store 名称（为空时从接口类名推导）
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class VfResolvable(
    val storeName: String = ""
)
