package com.example.archshowcase.network.header

import java.util.Locale
import java.util.TimeZone
import java.util.UUID

actual class HeaderProvider actual constructor() {

    actual fun getHeaders(): Map<String, String> = buildMap {
        put(HeaderConstants.API_KEY, API_KEY_DEV)
        put(HeaderConstants.CODE_TAG, HeaderConstants.buildCodeTag("desktop", VERSION))
        put(HeaderConstants.DEVICE_ID, cachedDeviceId)
        put(HeaderConstants.LANGUAGE, Locale.getDefault().toLanguageTag())
        put(HeaderConstants.TIMESTAMP, System.currentTimeMillis().toString())
        put(HeaderConstants.TIMEZONE, TimeZone.getDefault().id)
        put(HeaderConstants.NONCESTR, HeaderConstants.generateNonce())
        val memberId = HeaderConstants.currentMemberId
        if (memberId.isNotBlank()) put(HeaderConstants.MEMBER_ID, memberId)
        val umid = HeaderConstants.currentUmid
        if (umid.isNotBlank()) put(HeaderConstants.UMID, umid)
    }

    companion object {
        private const val API_KEY_DEV = "desktop-dev-key"
        private const val VERSION = "1.0.0"

        private val cachedDeviceId: String by lazy {
            UUID.randomUUID().toString()
        }
    }
}
