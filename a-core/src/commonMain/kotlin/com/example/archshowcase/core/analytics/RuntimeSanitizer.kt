package com.example.archshowcase.core.analytics

/** 运行时脱敏兜底：长字符串截断 + 疑似手机号/邮箱正则检测 */
object RuntimeSanitizer {

    private const val MAX_STRING_LENGTH = 200
    private val PHONE_REGEX = Regex("^\\d{9,16}$")
    private val EMAIL_REGEX = Regex("^[^@]+@[^@]+\\.[^@]+$")

    fun sanitize(params: Map<String, String>): Map<String, String> {
        if (params.isEmpty()) return params
        return params.mapValues { (_, value) -> sanitizeValue(value) }
    }

    internal fun sanitizeValue(value: String): String {
        return when {
            value.length > MAX_STRING_LENGTH -> value.take(MAX_STRING_LENGTH) + "...[truncated]"
            PHONE_REGEX.matches(value) -> maskPhone(value)
            EMAIL_REGEX.matches(value) -> maskEmail(value)
            else -> value
        }
    }

    internal fun maskPhone(phone: String): String {
        if (phone.length < 7) return "****"
        val prefix = phone.take(3)
        val suffix = phone.takeLast(4)
        return "$prefix****$suffix"
    }

    internal fun maskEmail(email: String): String {
        val parts = email.split("@", limit = 2)
        if (parts.size != 2) return "***@***"
        val local = parts[0]
        val domain = parts[1]
        val masked = if (local.length > 1) "${local[0]}***" else "***"
        return "$masked@$domain"
    }
}
