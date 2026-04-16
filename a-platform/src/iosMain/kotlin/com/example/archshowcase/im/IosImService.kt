package com.example.archshowcase.im

import com.example.archshowcase.core.util.Log
import com.example.archshowcase.im.model.ImConfig
import com.example.archshowcase.im.model.ImStatusCode
import com.example.archshowcase.im.service.ImService
import com.example.archshowcase.getImBridgeOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IosImService : ImService {

    private val _statusFlow = MutableStateFlow(ImStatusCode.LOGGED_OUT)
    override val statusFlow: StateFlow<ImStatusCode> = _statusFlow.asStateFlow()

    private val statusCallback = object : ImStatusCallback {
        override fun onStatusChanged(statusOrdinal: Int) {
            _statusFlow.value = ImStatusCode.entries.getOrElse(statusOrdinal) { ImStatusCode.LOGGED_OUT }
        }
    }

    private var token: String = ""
    private var imConfig: String = ""
    private var deviceId: String = ""

    fun setTokenInfo(token: String, imConfig: String, deviceId: String) {
        this.token = token
        this.imConfig = imConfig
        this.deviceId = deviceId
    }

    override fun initialize(config: ImConfig) {
        val bridge = getImBridgeOrNull()
        if (bridge == null) {
            Log.w(TAG) { "ImBridge not set, skipping initialization" }
            return
        }

        bridge.setStatusCallback(statusCallback)
        bridge.initialize(
            isDebug = config.isDebug,
            apiKey = config.apiKey,
            codeTag = config.codeTag,
            xlogKey = config.xlogKey,
            memberId = config.memberId,
            nickName = config.nickName,
            token = token,
            imConfig = imConfig,
            deviceId = deviceId
        )
    }

    override fun login(onSuccess: () -> Unit, onError: (Int, String) -> Unit) {
        val bridge = getImBridgeOrNull()
        if (bridge == null) {
            onError(-1, "ImBridge not set")
            return
        }

        bridge.login(object : ImBridgeCallback {
            override fun onSuccess(message: String) {
                _statusFlow.value = ImStatusCode.LOGGED_IN
                onSuccess()
            }

            override fun onError(code: Int, message: String) {
                onError(code, message)
            }
        })
    }

    override fun logout() {
        getImBridgeOrNull()?.let { bridge ->
            bridge.setStatusCallback(null)
            bridge.logout()
        }
        _statusFlow.value = ImStatusCode.LOGGED_OUT
    }

    override fun isLoggedIn(): Boolean {
        return getImBridgeOrNull()?.isLoggedIn() ?: false
    }

    override fun destroy() {
        getImBridgeOrNull()?.let { bridge ->
            bridge.setStatusCallback(null)
            bridge.destroy()
        }
        _statusFlow.value = ImStatusCode.LOGGED_OUT
    }

    companion object {
        private const val TAG = "IosImService"
    }
}
