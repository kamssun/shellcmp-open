package com.example.archshowcase.auth

interface LoginTokenCallback {
    fun onSuccess(accessToken: String, refreshToken: String)
    fun onFailure(message: String)
}

interface LoginUnitCallback {
    fun onSuccess()
    fun onFailure(message: String)
}

interface LoginBridgeListener {
    fun onForcedLogout(reason: String)
}

interface LoginBridge {
    fun isLoggedIn(): Boolean
    fun getAccessToken(): String?

    fun login(type: LoginType, callback: LoginTokenCallback)
    fun sendEmailCode(email: String, callback: LoginUnitCallback)
    fun verifyEmailCode(email: String, code: String, callback: LoginTokenCallback)
    fun refreshToken(callback: LoginTokenCallback)
    fun logout(callback: LoginUnitCallback)

    fun setListener(listener: LoginBridgeListener?)
}
