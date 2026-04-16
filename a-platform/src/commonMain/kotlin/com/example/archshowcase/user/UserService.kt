package com.example.archshowcase.user

import kotlinx.coroutines.flow.StateFlow

expect class UserService() {
    val profile: StateFlow<UserProfile?>

    fun getMemberId(): String?
    suspend fun fetchProfile(): Result<UserProfile>
    fun setProfile(profile: UserProfile)
    fun clear()
}
