// Stub implementation — SDK dependencies removed for open-source showcase
package com.example.archshowcase.im

import com.example.archshowcase.core.util.Log
import com.example.archshowcase.im.model.ImConfig
import com.example.archshowcase.im.model.ImStatusCode
import com.example.archshowcase.im.service.ImService as AppImService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidImService : AppImService {

    private val _statusFlow = MutableStateFlow(ImStatusCode.LOGGED_OUT)
    override val statusFlow: StateFlow<ImStatusCode> = _statusFlow.asStateFlow()

    override fun initialize(config: ImConfig) {
        // Stub: IM SDK not available
        Log.d(TAG) { "Stub: IM SDK initialize (no-op)" }
    }

    override fun login(onSuccess: () -> Unit, onError: (Int, String) -> Unit) {
        // Stub: IM SDK not available — report failure
        Log.d(TAG) { "Stub: IM login (not available)" }
        onError(-1, "IM SDK stub — not available")
    }

    override fun logout() {
        _statusFlow.value = ImStatusCode.LOGGED_OUT
    }

    override fun isLoggedIn(): Boolean {
        return false
    }

    override fun destroy() {
        _statusFlow.value = ImStatusCode.LOGGED_OUT
    }

    companion object {
        private const val TAG = "AndroidImService"
    }
}
