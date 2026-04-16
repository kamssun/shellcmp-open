package com.example.archshowcase.core

actual val isAndroidPlatform: Boolean = false
actual val isIosPlatform: Boolean = false
actual val isDesktopPlatform: Boolean = true

actual fun isDebug(): Boolean = true
