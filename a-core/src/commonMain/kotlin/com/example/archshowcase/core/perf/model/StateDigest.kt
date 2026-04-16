package com.example.archshowcase.core.perf.model

import com.example.archshowcase.core.trace.scroll.ScrollPosition

data class StateDigest(
    val storeName: String,
    val stateClassName: String,
    val hasValidData: Boolean,
    val historySize: Int? = null,
    val scrollPosition: ScrollPosition? = null,
    val stateHashCode: Int
)
