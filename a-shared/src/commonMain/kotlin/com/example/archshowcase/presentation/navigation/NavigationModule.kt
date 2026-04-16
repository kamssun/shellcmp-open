package com.example.archshowcase.presentation.navigation

import com.example.archshowcase.core.di.featureModuleOf
import org.koin.core.module.dsl.singleOf

private val navigationFeature = featureModuleOf<NavigationStoreFactory> {
    singleOf(::NavigationStoreFactory)
}

fun loadNavigationModule() = navigationFeature.load()
