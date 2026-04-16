package com.example.archshowcase.core.di

import androidx.compose.runtime.Composable
import coil3.PlatformContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Desktop Preview 平台模块
 */
@Composable
actual fun createPreviewPlatformModule(): Module {
    return module {
        single<PlatformContext> { PlatformContext.INSTANCE }
    }
}
