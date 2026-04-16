package com.example.archshowcase.core.perf.model

data class Phase(
    val name: String,
    val startMs: Long,
    val endMs: Long,
    val metadata: Map<String, String> = emptyMap()
) {
    val durationMs: Long get() = endMs - startMs
}
