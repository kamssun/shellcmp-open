package com.example.archshowcase.presentation.chat.room

import com.example.archshowcase.core.di.featureModuleOf
import com.example.archshowcase.chat.di.loadChatDataModule
import org.koin.core.module.dsl.singleOf

private val chatRoomFeature = featureModuleOf<ChatRoomStoreFactory> {
    singleOf(::ChatRoomStoreFactory)
}

fun loadChatRoomModule() {
    loadChatDataModule()
    chatRoomFeature.load()
}
