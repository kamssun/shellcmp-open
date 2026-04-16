package com.example.archshowcase.core.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import coil3.PlatformContext
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * iOS 平台模块 - 提供 PlatformContext 和 DataStore
 */
@OptIn(ExperimentalForeignApi::class)
val iosPlatformModule = module {
    single<PlatformContext> { PlatformContext.INSTANCE }

    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath {
            val dir = NSFileManager.defaultManager.URLForDirectory(
                NSDocumentDirectory, NSUserDomainMask, null, true, null
            )!!.path!!
            "$dir/settings.preferences_pb".toPath()
        }
    }
}
