package com.example.archshowcase.im.di

import com.example.archshowcase.core.AppRuntimeState
import com.example.archshowcase.core.di.featureModuleOf
import com.example.archshowcase.im.api.ChatRoomApi
import com.example.archshowcase.im.api.DefaultChatRoomApi
import com.example.archshowcase.im.api.MockChatRoomApi
import com.example.archshowcase.im.service.ImService
import org.koin.core.module.Module

expect fun createImPlatformModule(): Module

private val imFeature = featureModuleOf<ImService> {
    single<ChatRoomApi> {
        if (AppRuntimeState.isInPreview) MockChatRoomApi() else DefaultChatRoomApi()
    }
    includes(createImPlatformModule())
}

fun loadImModule() = imFeature.load()
