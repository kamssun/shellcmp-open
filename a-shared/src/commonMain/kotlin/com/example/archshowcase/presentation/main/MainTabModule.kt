package com.example.archshowcase.presentation.main

import com.example.archshowcase.core.di.featureModuleOf
import org.koin.core.module.dsl.singleOf

private val mainTabFeature = featureModuleOf<MainTabStoreFactory> {
    singleOf(::MainTabStoreFactory)
}

fun loadMainTabModule() = mainTabFeature.load()
