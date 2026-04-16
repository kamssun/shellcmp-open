package com.example.archshowcase.core.perf.model

enum class JankSeverity {
    SLIGHT,
    MODERATE,
    SEVERE,
    FROZEN;

    companion object {
        fun fromDroppedFrames(count: Int): JankSeverity? = when {
            count < 2 -> null
            count <= 3 -> SLIGHT
            count <= 8 -> MODERATE
            count <= 25 -> SEVERE
            else -> FROZEN
        }
    }
}
