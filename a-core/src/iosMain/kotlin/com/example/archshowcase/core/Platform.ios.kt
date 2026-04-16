package com.example.archshowcase.core

import kotlin.experimental.ExperimentalNativeApi

actual val isAndroidPlatform: Boolean = false
actual val isIosPlatform: Boolean = true
actual val isDesktopPlatform: Boolean = false

@OptIn(ExperimentalNativeApi::class)
actual fun isDebug(): Boolean = kotlin.native.Platform.isDebugBinary
