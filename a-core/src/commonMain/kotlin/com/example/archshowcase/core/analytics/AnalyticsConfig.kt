package com.example.archshowcase.core.analytics

import com.example.archshowcase.core.analytics.model.AnalyticsEvent
import com.example.archshowcase.core.isDebug

/** 埋点系统配置 */
data class AnalyticsConfig(
    val enabled: Boolean = true,
    val reporter: AnalyticsReporter = if (isDebug()) LoggingReporter() else NoOpReporter,
    val bufferCapacity: Int = DEFAULT_BUFFER_CAPACITY,
    val batchSize: Int = if (isDebug()) 1 else DEFAULT_BATCH_SIZE,
    val flushIntervalMs: Long = DEFAULT_FLUSH_INTERVAL_MS,
    val samplingRules: Map<String, Float> = DEFAULT_SAMPLING_RULES,
) {
    companion object {
        const val DEFAULT_BUFFER_CAPACITY = 500
        const val DEFAULT_BATCH_SIZE = 20
        const val DEFAULT_FLUSH_INTERVAL_MS = 10_000L

        /** 按事件类型名采样：Intent 100%, Page 100%, Exposure 100% */
        val DEFAULT_SAMPLING_RULES = mapOf(
            "Intent" to 1.0f,
            "Page" to 1.0f,
            "Exposure" to 1.0f,
        )
    }

    /** 获取事件类型的采样率（使用 sealed when 分支，不依赖 class.simpleName，R8 安全） */
    fun samplingRate(event: AnalyticsEvent): Float = when (event) {
        is AnalyticsEvent.Intent -> samplingRules["Intent"] ?: 1.0f
        is AnalyticsEvent.Page -> samplingRules["Page"] ?: 1.0f
        is AnalyticsEvent.Exposure -> samplingRules["Exposure"] ?: 1.0f
    }
}
