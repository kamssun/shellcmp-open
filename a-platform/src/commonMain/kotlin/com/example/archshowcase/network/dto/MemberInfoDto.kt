package com.example.archshowcase.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class MemberInfoDto(
    val member_id: String? = null,
    val nickname: String? = null,
    val avatar_url: String? = null,
)
