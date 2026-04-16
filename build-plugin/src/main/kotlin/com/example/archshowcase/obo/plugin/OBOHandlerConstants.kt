package com.example.archshowcase.obo.plugin

/** OBO Handler 拦截器共享常量（Transform + Report task 共用） */
object OBOHandlerConstants {
    private const val HANDLER = "android/os/Handler"
    const val COMPAT = "com/example/archshowcase/core/scheduler/OBOCompat"

    /** 不拦截的包前缀：OBO 自身 + 系统框架 + Debug 工具 */
    val SKIP_PREFIXES = listOf(
        // 项目自身代码（按规范写的，用 Handler 有特殊原因）
        "com.example.archshowcase.",
        // Android 框架（含 Compose 渲染管线、生命周期、动画）
        "androidx.",
        "android.",
        "com.google.android.material.",
        // Kotlin 协程调度器（Dispatchers.Main）
        "kotlinx.coroutines.",
        // Debug 工具
        "leakcanary.",
        "curtains.",
        // Compose 周边
        "com.google.accompanist.",
        // 架构组件（Decompose / MVIKotlin）
        "com.arkivanov.",
    )

    /** 要检测的 Handler 方法名 */
    val TARGET_METHODS = setOf(
        "post", "postAtTime", "postDelayed",
        "sendMessage", "sendMessageDelayed", "sendMessageAtTime",
        "sendEmptyMessage", "sendEmptyMessageDelayed", "sendEmptyMessageAtTime",
        "postAtFrontOfQueue", "sendMessageAtFrontOfQueue",
    )

    /** 判断一个 INVOKEVIRTUAL 指令是否为 Handler 目标方法 */
    fun isHandlerCall(owner: String, name: String): Boolean =
        owner == HANDLER && name in TARGET_METHODS
}
