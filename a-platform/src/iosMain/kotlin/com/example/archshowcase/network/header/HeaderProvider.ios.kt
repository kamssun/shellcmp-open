package com.example.archshowcase.network.header

import platform.Foundation.NSBundle
import platform.Foundation.NSDate
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUUID
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.localTimeZone
import platform.Foundation.regionCode
import platform.Foundation.timeIntervalSince1970

actual class HeaderProvider actual constructor() {

    actual fun getHeaders(): Map<String, String> = buildMap {
        put(HeaderConstants.API_KEY, appKey)
        put(HeaderConstants.CODE_TAG, HeaderConstants.buildCodeTag("ios", appVersion))
        put(HeaderConstants.DEVICE_ID, cachedDeviceId)
        put(HeaderConstants.LANGUAGE, buildLanguageTag())
        put(HeaderConstants.TIMESTAMP, currentTimestampMs())
        put(HeaderConstants.TIMEZONE, NSTimeZone.localTimeZone.name)
        put(HeaderConstants.NONCESTR, HeaderConstants.generateNonce())
        val memberId = HeaderConstants.currentMemberId
        if (memberId.isNotBlank()) put(HeaderConstants.MEMBER_ID, memberId)
        val umid = HeaderConstants.currentUmid
        if (umid.isNotBlank()) put(HeaderConstants.UMID, umid)
    }

    private val appKey: String
        get() = NSBundle.mainBundle.objectForInfoDictionaryKey(PLIST_APP_KEY) as? String ?: ""

    private val appVersion: String
        get() = NSBundle.mainBundle.objectForInfoDictionaryKey(PLIST_VERSION) as? String ?: "1.0.0"

    companion object {
        private const val PLIST_APP_KEY = "APP_KEY"
        private const val PLIST_VERSION = "CFBundleShortVersionString"
    }

    private val cachedDeviceId: String by lazy {
        val key = HeaderConstants.DEVICE_ID_STORAGE_KEY
        val defaults = NSUserDefaults.standardUserDefaults
        defaults.stringForKey(key) ?: run {
            val newId = NSUUID().UUIDString
            defaults.setObject(newId, forKey = key)
            newId
        }
    }

    private fun buildLanguageTag(): String {
        val locale = NSLocale.currentLocale
        val language = locale.languageCode
        val region = locale.regionCode ?: ""
        return if (region.isNotEmpty()) "$language-$region" else language
    }

    private fun currentTimestampMs(): String {
        return (NSDate().timeIntervalSince1970 * 1000).toLong().toString()
    }
}
