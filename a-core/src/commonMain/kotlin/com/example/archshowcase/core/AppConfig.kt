package com.example.archshowcase.core

// 可手动调整的配置项，控制功能开关和调试行为
object AppConfig {

    private var _enableRestore: Boolean? = null
    private var _useDemoMode: Boolean? = null

    // 交互回溯（TimeTravel + 状态恢复），默认跟随 isDebug()
    var enableRestore: Boolean
        get() = _enableRestore ?: isDebug()
        set(value) { _enableRestore = value }

    // OBO 调度：Effect 逐个执行并让出主线程，避免启动卡顿
    var useOBOScheduler: Boolean = true

    // 跳过登录直接进主页，仅 debug 默认启用
    var skipLogin: Boolean = isDebug()

    // Demo 模式：true 进 Demo 首页，false 进正式 Tab 框架
    var useDemoMode: Boolean
        get() = _useDemoMode ?: false
        set(value) { _useDemoMode = value }

    // 性能监控（启动打点 / 帧卡顿 / 页面帧率），默认全环境开启
    var enablePerfMonitor: Boolean = true

    // 埋点采集（页面 / 曝光 / 交互），默认全环境开启
    var enableAnalytics: Boolean = true

    // TimeTravel 日志（LoggingStoreFactory），大数据量会拖慢主线程，按需开启
    var enableTimeTravelLogging: Boolean = false

    // GC 压力检测：阻塞 GC 频繁时输出 PSS 和 Store 集合快照
    var enableGcPressureDetection: Boolean = true

    // OBO 诊断：追踪任务耗时和来源，检测慢任务和高耗时来源
    var enableOBODiagnostics: Boolean = true
}
