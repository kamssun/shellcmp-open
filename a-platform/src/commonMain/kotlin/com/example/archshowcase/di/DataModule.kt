package com.example.archshowcase.di

import com.example.archshowcase.core.AppRuntimeState
import com.example.archshowcase.data.settings.DataStoreSettingsRepository
import com.example.archshowcase.data.settings.InMemorySettingsRepository
import com.example.archshowcase.data.settings.SettingsRepository
import com.example.archshowcase.network.api.ImageRepository
import com.example.archshowcase.network.api.UserRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * 数据模块 - Repository
 */
val dataModule = module {
    singleOf(::ImageRepository)
    singleOf(::UserRepository)

    single<SettingsRepository> {
        if (AppRuntimeState.isInPreview) InMemorySettingsRepository() else DataStoreSettingsRepository()
    }
}


