package com.example.archshowcase.devicetoken

import com.example.archshowcase.getDeviceTokenBridgeOrNull
import com.example.archshowcase.network.header.HeaderConstants

actual class DeviceTokenService actual constructor() {

    actual fun getUmid(): String? {
        val umid = getDeviceTokenBridgeOrNull()?.getUmid()
        if (umid != null) HeaderConstants.currentUmid = umid
        return umid
    }

    actual fun sign(fields: Map<String, String>): String? =
        getDeviceTokenBridgeOrNull()?.sign(fields)

    actual fun reportAction(action: DeviceTokenAction) {
        getDeviceTokenBridgeOrNull()?.reportAction(action.name)
    }
}
