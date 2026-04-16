package com.example.archshowcase.core.perf.model

data class TransitionInfo(
    val fromRoute: String,
    val toRoute: String,
    val startedAtMs: Long
)
