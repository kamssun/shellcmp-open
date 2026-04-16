package com.example.archshowcase.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import coil3.PlatformContext
import okio.Path.Companion.toPath
import org.koin.dsl.module

/**
 * Android 平台模块 - 提供 PlatformContext 和 DataStore
 */
fun createAndroidPlatformModule(context: Context) = module {
    single<PlatformContext> { context }

    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath {
            context.filesDir.resolve("settings.preferences_pb").absolutePath.toPath()
        }
    }
}
