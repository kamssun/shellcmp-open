package com.example.archshowcase.chat.di

import com.example.archshowcase.core.AppRuntimeState
import com.example.archshowcase.core.di.featureModuleOf
import com.example.archshowcase.chat.db.ChatDatabase
import com.example.archshowcase.chat.local.ChatDao
import com.example.archshowcase.chat.local.DatabaseDriverFactory
import com.example.archshowcase.chat.repository.ChatRepository
import com.example.archshowcase.chat.repository.MockChatRepository
import com.example.archshowcase.chat.repository.PreviewChatRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind

expect fun createChatPlatformModule(): Module

// TODO: replace MockChatRepository with ImChatRepository when IM SDK is ready
private val chatDataFeature = featureModuleOf<ChatRepository> {
    if (AppRuntimeState.isInPreview) {
        // Preview (layoutlib): 纯内存，不依赖 SQLDelight
        singleOf(::PreviewChatRepository) bind ChatRepository::class
    } else {
        single { ChatDatabase(get<DatabaseDriverFactory>().create()) }
        singleOf(::ChatDao)
        singleOf(::MockChatRepository) bind ChatRepository::class
        includes(createChatPlatformModule())
    }
}

fun loadChatDataModule() = chatDataFeature.load()
