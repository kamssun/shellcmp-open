package com.example.archshowcase.core.di

import androidx.compose.runtime.Composable
import coil3.PlatformContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * iOS Preview 平台模块，非预览链路不应该调用。
 *
 * iOS 的 PlatformContext 是一个单例对象（INSTANCE），不依赖运行时环境
 * 不像 Android 的 Context 需要从 Application/Activity 生命周期获取
 * Preview 和正式运行时都能直接访问这个单例
 */
@Composable
actual fun createPreviewPlatformModule(): Module = module {
    single<PlatformContext> { PlatformContext.INSTANCE }
}
