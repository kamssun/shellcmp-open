package com.example.archshowcase.presentation.chat.list

import com.example.archshowcase.core.di.featureModuleOf
import com.example.archshowcase.chat.di.loadChatDataModule
import org.koin.core.module.dsl.singleOf

private val conversationListFeature = featureModuleOf<ConversationListStoreFactory> {
    singleOf(::ConversationListStoreFactory)
}

fun loadConversationListModule() {
    loadChatDataModule()
    conversationListFeature.load()
}
