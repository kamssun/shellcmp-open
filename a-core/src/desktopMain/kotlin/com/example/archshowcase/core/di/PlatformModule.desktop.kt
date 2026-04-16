package com.example.archshowcase.core.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import coil3.PlatformContext
import okio.Path.Companion.toPath
import org.koin.dsl.module
import java.io.File

/**
 * Desktop 平台模块 - 提供 PlatformContext 和 DataStore
 */
val desktopPlatformModule = module {
    single<PlatformContext> { PlatformContext.INSTANCE }

    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath {
            val userHome = System.getProperty("user.home")
            val appDir = File(userHome, ".archshowcase")
            if (!appDir.exists()) appDir.mkdirs()
            File(appDir, "settings.preferences_pb").absolutePath.toPath()
        }
    }
}
