package com.example.archshowcase.presentation.settings

import com.example.archshowcase.core.di.featureModuleOf
import org.koin.core.module.dsl.singleOf

private val settingsFeature = featureModuleOf<SettingsStoreFactory> {
    singleOf(::SettingsStoreFactory)
}

fun loadSettingsModule() = settingsFeature.load()
