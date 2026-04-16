package com.example.archshowcase.core.analytics

/** Desktop 平台无前后台概念，空实现 */
actual object AppLifecycleTracker {
    actual fun start() = Unit
    actual fun stop() = Unit
}
