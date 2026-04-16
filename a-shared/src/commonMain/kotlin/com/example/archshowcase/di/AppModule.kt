package com.example.archshowcase.di

import com.example.archshowcase.core.di.getCoreModules
import com.example.archshowcase.i18n.i18nModule
import org.koin.core.module.Module

fun getAppModules(platformModule: Module): List<Module> {
    return getCoreModules(platformModule) + authModule + userModule + networkModule + dataModule + deviceTokenModule + i18nModule
}
