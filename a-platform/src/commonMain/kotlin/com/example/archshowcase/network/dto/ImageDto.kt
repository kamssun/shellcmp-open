package com.example.archshowcase.network.dto

import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import kotlinx.serialization.Serializable

@Serializable
data class ImageItem(
    val id: String,
    val url: String,
    val title: String
) : JvmSerializable

@Serializable
data class ImagePageResponse(
    val items: List<ImageItem>,
    val total: Int,
    val hasMore: Boolean
) : JvmSerializable
