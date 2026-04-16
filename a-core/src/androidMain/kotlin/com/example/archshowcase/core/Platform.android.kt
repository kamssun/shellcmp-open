package com.example.archshowcase.core

import android.app.Application
import android.content.Context
import com.example.archshowcase.core.trace.user.IntentTracker
import com.example.archshowcase.core.util.ContextProvider
import com.example.archshowcase.core.util.PermissionHelper
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual val isAndroidPlatform: Boolean = true
actual val isIosPlatform: Boolean = false
actual val isDesktopPlatform: Boolean = false

private var debugMode = false

actual fun isDebug(): Boolean = debugMode || AppRuntimeState.isInPreview

/**
 * Android 应用初始化入口，在 Application.onCreate() 中调用
 */
fun initAndroidApp(context: Context, modules: List<Module>, isDebug: Boolean) {
    val application = context.applicationContext as Application

    // 1. Activity 生命周期追踪（必须最先，后续组件依赖 ContextProvider）
    ContextProvider.install(application)

    // 2. 权限管理
    PermissionHelper.install(application)

    // 3. 检测 Debug 模式
    debugMode = isDebug

    // 4. 初始化 Koin（TimeTravelServer 在 DecomposeModule 中自动启动）
    startKoin {
        androidLogger()
        androidContext(context)
        modules(modules)
    }

    // 5. Release: 设置崩溃处理
    if (!debugMode) {
        setupCrashHandler()
    }
}

private fun setupCrashHandler() {
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        IntentTracker.dump()
        exportCrashTrace()
        defaultHandler?.uncaughtException(thread, throwable)
    }
}

private fun exportCrashTrace() {
    try {
        val context = ContextProvider.applicationContext
        val packageSuffix = context.packageName.substringAfterLast('.')
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "crash_trace_$timestamp.json"

        val dir = context.getExternalFilesDir(null)?.let { File(it, packageSuffix) }
            ?: File(context.filesDir, packageSuffix)

        if (dir.exists() || dir.mkdirs()) {
            File(dir, fileName).writeText(IntentTracker.exportJson())
        }
    } catch (_: Exception) {
    }
}
