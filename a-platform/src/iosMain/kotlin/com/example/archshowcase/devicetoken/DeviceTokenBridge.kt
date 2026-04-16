package com.example.archshowcase.devicetoken

interface DeviceTokenBridge {
    fun initialize(appKey: String)
    fun getUmid(): String?
    fun sign(fields: Map<String, String>): String?
    fun reportAction(action: String)
}
