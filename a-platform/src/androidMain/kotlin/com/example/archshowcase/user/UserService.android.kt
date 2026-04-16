package com.example.archshowcase.user

import com.example.archshowcase.core.util.Log
import com.example.archshowcase.network.ApiRoutes
import com.example.archshowcase.network.dto.ApiResponse
import com.example.archshowcase.network.dto.MemberInfoDto
import com.example.archshowcase.network.header.HeaderConstants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual class UserService actual constructor() : KoinComponent {

    private val client: HttpClient by inject()

    private val _profile = MutableStateFlow<UserProfile?>(null)
    actual val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    actual fun getMemberId(): String? = _profile.value?.memberId

    actual suspend fun fetchProfile(): Result<UserProfile> = runCatching {
        val response = client.get(ApiRoutes.MEMBER_INFO)
            .body<ApiResponse<MemberInfoDto>>()
        val data = response.data
            ?: throw IllegalStateException("Empty member info response")
        UserProfile(
            memberId = data.member_id.orEmpty(),
            nickname = data.nickname.orEmpty(),
            avatarUrl = data.avatar_url.orEmpty(),
        )
    }.onSuccess { profile ->
        setProfile(profile)
    }.onFailure { e ->
        Log.e(TAG, e) { "fetchProfile failed" }
    }

    actual fun setProfile(profile: UserProfile) {
        _profile.value = profile
        HeaderConstants.currentMemberId = profile.memberId
    }

    actual fun clear() {
        _profile.value = null
        HeaderConstants.currentMemberId = ""
    }

    companion object {
        private const val TAG = "UserService"
    }
}
