package com.example.archshowcase.user

import com.example.archshowcase.network.header.HeaderConstants
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class UserService actual constructor() {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    actual val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    actual fun getMemberId(): String? = _profile.value?.memberId

    actual suspend fun fetchProfile(): Result<UserProfile> {
        delay(200)
        val profile = UserProfile(
            memberId = "desktop_mock_member",
            nickname = "Desktop User",
            avatarUrl = ""
        )
        setProfile(profile)
        return Result.success(profile)
    }

    actual fun setProfile(profile: UserProfile) {
        _profile.value = profile
        HeaderConstants.currentMemberId = profile.memberId
    }

    actual fun clear() {
        _profile.value = null
        HeaderConstants.currentMemberId = ""
    }
}
