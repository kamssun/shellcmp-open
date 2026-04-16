package com.example.archshowcase.core.compose.exposure

/** 曝光事件数据 */
data class ExposureEvent(
    val componentType: String,
    val exposureKey: String,
    val listId: String = "",
    val params: Map<String, String> = emptyMap(),
)
