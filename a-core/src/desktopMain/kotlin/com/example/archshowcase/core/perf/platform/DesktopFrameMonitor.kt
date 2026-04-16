package com.example.archshowcase.core.perf.platform

import com.example.archshowcase.core.perf.model.FrameTiming

actual class FrameMonitor actual constructor() {
    actual fun start(callback: (FrameTiming) -> Unit) {
        // Desktop: no-op, frame monitoring not supported
    }

    actual fun stop() {
        // Desktop: no-op
    }
}
