package com.example.archshowcase.chat.di

import com.example.archshowcase.chat.local.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun createChatPlatformModule(): Module = module {
    single { DatabaseDriverFactory(get()) }
}
