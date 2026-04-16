package com.example.archshowcase.core.di

import org.koin.core.module.Module

fun getCoreModules(platformModule: Module): List<Module> =
    listOf(platformModule, presentationModule)
