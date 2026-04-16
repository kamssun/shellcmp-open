package com.example.archshowcase.core.di

import androidx.compose.runtime.Composable
import org.koin.core.module.Module

/**
 * Preview 用的平台模块 - 提供 PlatformContext，非预览链路不应该调用。
 *
 * 使用 expect/actual 模式，各平台提供自己的实现
 * 需要在 @Composable 上下文中调用以访问平台 Context
 */
@Composable
expect fun createPreviewPlatformModule(): Module
