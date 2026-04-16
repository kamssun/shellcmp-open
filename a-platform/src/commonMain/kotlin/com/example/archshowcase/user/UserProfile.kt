package com.example.archshowcase.user

data class UserProfile(
    val memberId: String,
    val nickname: String = "",
    val avatarUrl: String = "",
)
