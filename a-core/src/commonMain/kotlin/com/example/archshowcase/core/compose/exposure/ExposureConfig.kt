package com.example.archshowcase.core.compose.exposure

/** 曝光追踪配置 */
data class ExposureConfig(
    /** 可见面积比例阈值 */
    val visibleAreaRatio: Float = 0.5f,
    /** 停留时间阈值（ms） */
    val dwellTimeMs: Long = 500L,
)
