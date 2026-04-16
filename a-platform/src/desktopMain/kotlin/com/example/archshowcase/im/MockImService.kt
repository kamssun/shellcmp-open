package com.example.archshowcase.im

import com.example.archshowcase.core.util.Log
import com.example.archshowcase.im.model.ImConfig
import com.example.archshowcase.im.model.ImStatusCode
import com.example.archshowcase.im.service.ImService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockImService : ImService {

    private val _statusFlow = MutableStateFlow(ImStatusCode.LOGGED_OUT)
    override val statusFlow: StateFlow<ImStatusCode> = _statusFlow.asStateFlow()

    override fun initialize(config: ImConfig) {
        Log.d(TAG) { "Mock IM initialized: debug=${config.isDebug}" }
    }

    override fun login(onSuccess: () -> Unit, onError: (Int, String) -> Unit) {
        Log.d(TAG) { "Mock IM login" }
        _statusFlow.value = ImStatusCode.LOGGED_IN
        onSuccess()
    }

    override fun logout() {
        Log.d(TAG) { "Mock IM logout" }
        _statusFlow.value = ImStatusCode.LOGGED_OUT
    }

    override fun isLoggedIn(): Boolean = _statusFlow.value == ImStatusCode.LOGGED_IN

    override fun destroy() {
        Log.d(TAG) { "Mock IM destroy" }
        _statusFlow.value = ImStatusCode.LOGGED_OUT
    }

    companion object {
        private const val TAG = "MockImService"
    }
}
