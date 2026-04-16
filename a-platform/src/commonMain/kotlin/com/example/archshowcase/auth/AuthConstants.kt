package com.example.archshowcase.auth

object AuthConstants {
    const val AUTHORIZATION_HEADER = "Authorization"
    const val BEARER_PREFIX = "Bearer"

    const val AUTH_REFRESH_API = "/base/token/refresh"
    const val TOKEN_REFRESH_INTERVAL_MS = 5_000

    const val ERROR_MISSING_ACTIVITY = "登录需要可用的 Activity"
    const val ERROR_UNSUPPORTED_LOGIN = "当前平台不支持该登录方式"
    const val ERROR_BRIDGE_NOT_SET = "iOS LoginBridge 未注入"
    const val ERROR_EMPTY_EMAIL = "邮箱不能为空"
    const val ERROR_EMPTY_CODE = "验证码不能为空"
    const val ERROR_REFRESH_FAILED = "Token 刷新失败"
    const val ERROR_REFRESH_FORBIDDEN = "Token 已失效，需重新登录"
}
