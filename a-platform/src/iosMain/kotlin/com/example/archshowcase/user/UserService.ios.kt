package com.example.archshowcase.user

import com.example.archshowcase.getUserBridgeOrNull
import com.example.archshowcase.network.header.HeaderConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class UserService actual constructor() {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    actual val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    actual fun getMemberId(): String? = _profile.value?.memberId

    actual suspend fun fetchProfile(): Result<UserProfile> {
        val bridge = getUserBridgeOrNull()
            ?: return Result.failure(IllegalStateException(UserConstants.ERROR_BRIDGE_NOT_SET))

        return suspendCancellableCoroutine { continuation ->
            bridge.fetchProfile(object : UserProfileCallback {
                override fun onSuccess(memberId: String, nickname: String, avatarUrl: String) {
                    val profile = UserProfile(
                        memberId = memberId,
                        nickname = nickname,
                        avatarUrl = avatarUrl
                    )
                    setProfile(profile)
                    if (continuation.isActive) {
                        continuation.resume(Result.success(profile))
                    }
                }

                override fun onFailure(message: String) {
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(IllegalStateException(message)))
                    }
                }
            })
        }
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
