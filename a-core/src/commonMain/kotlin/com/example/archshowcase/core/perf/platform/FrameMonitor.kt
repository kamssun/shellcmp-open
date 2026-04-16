package com.example.archshowcase.core.perf.platform

import com.example.archshowcase.core.perf.model.FrameTiming

expect class FrameMonitor() {
    fun start(callback: (FrameTiming) -> Unit)
    fun stop()
}
