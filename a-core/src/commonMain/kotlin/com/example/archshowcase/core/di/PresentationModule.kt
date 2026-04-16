package com.example.archshowcase.core.di

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.logging.logger.Logger
import com.arkivanov.mvikotlin.logging.store.LoggingStoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.arkivanov.mvikotlin.timetravel.store.TimeTravelStoreFactory
import com.example.archshowcase.core.AppConfig
import com.example.archshowcase.core.trace.scroll.ScrollUpdateCoordinator
import com.example.archshowcase.core.trace.user.IntentTrackingStoreFactory
import com.example.archshowcase.core.util.Log
import okio.FileSystem
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * 表现层模块 - StoreFactory + ImageLoader
 */
val presentationModule = module {
    factoryOf(::ScrollUpdateCoordinator)
    single<ImageLoader> {
        val context: PlatformContext = get()
        val imageLoader = ImageLoader.Builder(context)
            .crossfade(true)
            .fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(8))
            .components { addPlatformDecoders(this) }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, percent = 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache")
                    .maxSizePercent(0.02)
                    .build()
            }
            .build()
        SingletonImageLoader.setSafe { imageLoader }
        imageLoader
    }
    single<StoreFactory> {
        if (AppConfig.enableRestore) {
            createDebugStoreFactory()
        } else {
            IntentTrackingStoreFactory(DefaultStoreFactory())
        }
    }
}

private const val TAG_TIME_TRAVEL = "TimeTravel"

private fun createDebugStoreFactory(): StoreFactory {
    startTimeTravelServer()
    val timeTravel = TimeTravelStoreFactory()
    val base = if (AppConfig.enableTimeTravelLogging) {
        LoggingStoreFactory(
            delegate = timeTravel,
            logger = object : Logger {
                override fun log(text: String) = Log.d(TAG_TIME_TRAVEL) { text }
            }
        )
    } else {
        timeTravel
    }
    return IntentTrackingStoreFactory(base)
}

internal expect fun startTimeTravelServer()

internal expect fun addPlatformDecoders(builder: coil3.ComponentRegistry.Builder)
