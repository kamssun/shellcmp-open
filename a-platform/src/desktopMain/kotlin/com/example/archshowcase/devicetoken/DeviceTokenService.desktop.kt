package com.example.archshowcase.devicetoken

import com.example.archshowcase.core.util.Log
import com.example.archshowcase.network.header.HeaderConstants

private const val TAG = "DeviceToken"
private const val MOCK_UMID = "desktop-mock-umid"

actual class DeviceTokenService actual constructor() {

    init {
        HeaderConstants.currentUmid = MOCK_UMID
    }

    actual fun getUmid(): String? = MOCK_UMID

    actual fun sign(fields: Map<String, String>): String? = null

    actual fun reportAction(action: DeviceTokenAction) {
        Log.d(TAG) { "reportAction: $action (desktop mock)" }
    }
}
