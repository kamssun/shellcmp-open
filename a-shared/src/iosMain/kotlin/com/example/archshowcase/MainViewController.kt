package com.example.archshowcase

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.example.archshowcase.presentation.root.DefaultRootComponent
import platform.UIKit.UIViewController
import com.example.archshowcase.core.di.iosPlatformModule
import com.example.archshowcase.core.perf.model.StartupType
import com.example.archshowcase.core.perf.platform.deviceInfo
import com.example.archshowcase.core.perf.platform.processStartTimeMs
import com.example.archshowcase.core.perf.platform.markProcessStart
import com.example.archshowcase.core.perf.startup.StartupTracer
import com.example.archshowcase.core.perf.PerfMonitor
import com.example.archshowcase.di.getAppModules
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.context.startKoin
import platform.Foundation.NSRunLoop
import platform.Foundation.NSRunLoopCommonModes
import platform.QuartzCore.CADisplayLink
import platform.darwin.NSObject
import platform.objc.sel_registerName

fun initKoin() {
    markProcessStart()
    StartupTracer.begin(StartupType.COLD, startTimeMs = processStartTimeMs())
    StartupTracer.traced("di_init") {
        startKoin {
            modules(getAppModules(iosPlatformModule))
        }
    }
}

// Bridge setters/getters 已移至 b-platform BridgeRegistry.kt
// Swift 端 setLoginBridge() 等调用保持不变（同包名，api 传递）

@OptIn(ExperimentalForeignApi::class)
fun MainViewController(): UIViewController = StartupTracer.traced("view_controller_create") {
    val root = StartupTracer.traced("root_component") {
        DefaultRootComponent(DefaultComponentContext(lifecycle = LifecycleRegistry()))
    }

    val viewController = StartupTracer.traced("compose_setup") {
        ComposeUIViewController { MainView(root) }
    }

    // 首帧回调完成启动追踪（对齐 Android 的 Choreographer.postFrameCallback）
    StartupTracer.markPhaseStart("first_frame")
    val callback = FirstFrameCallback {
        StartupTracer.markPhaseEnd("first_frame")
        val trace = StartupTracer.finish(deviceInfo())
        if (trace != null) {
            PerfMonitor.reportStartup(trace)
        }
    }
    val link = CADisplayLink.displayLinkWithTarget(
        target = callback,
        selector = sel_registerName("onFirstFrame")!!
    )
    callback.displayLink = link
    link.addToRunLoop(NSRunLoop.mainRunLoop, NSRunLoopCommonModes)

    viewController
}

private class FirstFrameCallback(
    private val onFrame: () -> Unit
) : NSObject() {
    var displayLink: CADisplayLink? = null

    @kotlinx.cinterop.ObjCAction
    fun onFirstFrame() {
        displayLink?.invalidate()
        displayLink = null
        onFrame()
    }
}
