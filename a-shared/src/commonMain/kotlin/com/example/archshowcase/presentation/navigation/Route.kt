package com.example.archshowcase.presentation.navigation

import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.example.archshowcase.replayable.RouteRegistry
import kotlinx.serialization.Serializable

/**
 * 统一路由定义，用于 Decompose 导航和 MVIKotlin TimeTravel
 *
 * 新增路由只需在此添加 1 行，KSP 自动生成 serialName / fromSerialName。
 */
@RouteRegistry(fallback = "Home")
@Serializable
sealed interface Route : JvmSerializable {
    @Serializable data object Login : Route
    @Serializable data object EmailLogin : Route
    @Serializable data object Home : Route
    @Serializable data object NetworkDemo : Route
    @Serializable data object ImageDemo : Route
    @Serializable data class Detail(val id: String) : Route
    @Serializable data object AdaptiveDemo : Route
    @Serializable data object CrashDemo : Route
    @Serializable data object Settings : Route
    @Serializable data object OBODemo : Route
    @Serializable data object Live : Route
    @Serializable data object Payment : Route
    @Serializable data object Main : Route
    @Serializable data class ChatRoom(val conversationId: String) : Route

    companion object
}
