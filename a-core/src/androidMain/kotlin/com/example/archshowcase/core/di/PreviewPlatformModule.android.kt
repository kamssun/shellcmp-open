package com.example.archshowcase.core.di

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import coil3.PlatformContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android Preview 平台模块，非预览链路不应该调用。
 * Android Studio 运行 @Preview 时，会启动一个轻量级的 Compose 渲染环境
 * 这个环境会自动提供一个 CompositionLocalProvider，注入一个模拟的 Context（通常是 androidx.compose.ui.tooling.preview.PreviewContext 或类似实现）
 * 在 @Composable 函数内部调用 LocalContext.current 就能拿到这个模拟 Context
 */
@Composable
actual fun createPreviewPlatformModule(): Module {
    val context = LocalContext.current
    return module {
        single<PlatformContext> { context }
    }
}
