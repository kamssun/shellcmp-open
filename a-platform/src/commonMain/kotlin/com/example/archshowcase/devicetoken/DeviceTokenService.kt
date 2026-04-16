package com.example.archshowcase.devicetoken

expect class DeviceTokenService() {
    fun getUmid(): String?
    fun sign(fields: Map<String, String>): String?
    fun reportAction(action: DeviceTokenAction)
}
