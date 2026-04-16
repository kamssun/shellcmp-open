package com.example.archshowcase.im.di

import com.example.archshowcase.im.AndroidChatRoomService
import com.example.archshowcase.im.AndroidImService
import com.example.archshowcase.im.service.ChatRoomService
import com.example.archshowcase.im.service.ImService
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun createImPlatformModule(): Module = module {
    singleOf(::AndroidImService) bind ImService::class
    singleOf(::AndroidChatRoomService) bind ChatRoomService::class
}
