package com.example.archshowcase.core

expect fun isDebug(): Boolean

expect val isAndroidPlatform: Boolean
expect val isIosPlatform: Boolean
expect val isDesktopPlatform: Boolean