package com.example.archshowcase.network.header

import kotlin.concurrent.Volatile
import kotlin.random.Random

object HeaderConstants {
    const val API_KEY = "Apikey"
    const val CODE_TAG = "Codetag"
    const val DEVICE_ID = "Deviceid"
    const val MEMBER_ID = "Memberid"
    const val NONCESTR = "Noncestr"
    const val LANGUAGE = "Language"
    const val TIMESTAMP = "Timestamp"
    const val TIMEZONE = "timezone"
    const val UMID = "umid"
    const val SIGN_TOKEN = "SignToken"
    const val ML_KEY_EXT = "MlKeyExt"

    const val APP_NAME = "archshowcase"
    const val DEVICE_ID_STORAGE_KEY = "com.example.archshowcase.deviceId"

    @Volatile
    var currentMemberId: String = ""

    @Volatile
    var currentUmid: String = ""

    fun buildCodeTag(platform: String, version: String): String =
        "$APP_NAME-$platform-$version"

    private const val NONCE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

    fun generateNonce(): String = buildString(32) {
        repeat(32) { append(NONCE_CHARS[Random.nextInt(NONCE_CHARS.length)]) }
    }
}
