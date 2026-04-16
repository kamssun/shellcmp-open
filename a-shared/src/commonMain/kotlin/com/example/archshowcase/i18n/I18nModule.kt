package com.example.archshowcase.i18n

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val i18nModule = module {
    singleOf(::ComposeStringProvider) bind StringProvider::class
}
