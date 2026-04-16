package com.example.archshowcase.user

interface UserProfileCallback {
    fun onSuccess(memberId: String, nickname: String, avatarUrl: String)
    fun onFailure(message: String)
}

interface UserBridge {
    fun fetchProfile(callback: UserProfileCallback)
}
