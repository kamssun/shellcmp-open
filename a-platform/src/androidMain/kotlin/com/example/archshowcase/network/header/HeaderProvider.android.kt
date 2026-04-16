package com.example.archshowcase.network.header

import java.util.Locale
import java.util.TimeZone

actual class HeaderProvider actual constructor() {

    actual fun getHeaders(): Map<String, String> = buildMap {
        put(HeaderConstants.API_KEY, Config.apiKey)
        put(HeaderConstants.CODE_TAG, Config.codeTag)
        put(HeaderConstants.DEVICE_ID, Config.deviceId)
        put(HeaderConstants.LANGUAGE, Locale.getDefault().toLanguageTag())
        put(HeaderConstants.TIMESTAMP, System.currentTimeMillis().toString())
        put(HeaderConstants.TIMEZONE, TimeZone.getDefault().id)
        put(HeaderConstants.NONCESTR, HeaderConstants.generateNonce())
        val memberId = HeaderConstants.currentMemberId
        if (memberId.isNotBlank()) put(HeaderConstants.MEMBER_ID, memberId)
        val umid = HeaderConstants.currentUmid
        if (umid.isNotBlank()) put(HeaderConstants.UMID, umid)
    }

    companion object Config {
        var apiKey: String = ""
            private set
        var codeTag: String = ""
            private set
        var deviceId: String = ""
            private set

        fun configure(apiKey: String, codeTag: String, deviceId: String) {
            Config.apiKey = apiKey
            Config.codeTag = codeTag
            Config.deviceId = deviceId
        }
    }
}
