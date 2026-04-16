package com.example.archshowcase.presentation.main

import com.example.archshowcase.resources.Res
import com.example.archshowcase.resources.tab_chat
import com.example.archshowcase.resources.tab_discover
import com.example.archshowcase.resources.tab_home
import com.example.archshowcase.resources.tab_me
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource

@Serializable
enum class MainTab {
    Home, Discover, Chat, Me;

    val titleRes: StringResource
        get() = when (this) {
            Home -> Res.string.tab_home
            Discover -> Res.string.tab_discover
            Chat -> Res.string.tab_chat
            Me -> Res.string.tab_me
        }
}
