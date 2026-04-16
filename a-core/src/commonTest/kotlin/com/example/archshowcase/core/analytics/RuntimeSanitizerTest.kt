package com.example.archshowcase.core.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RuntimeSanitizerTest {

    @Test
    fun longString_isTruncated() {
        val long = "a".repeat(500)
        val result = RuntimeSanitizer.sanitizeValue(long)
        assertTrue(result.endsWith("...[truncated]"))
        assertEquals(200 + "...[truncated]".length, result.length)
    }

    @Test
    fun normalString_unchanged() {
        assertEquals("hello", RuntimeSanitizer.sanitizeValue("hello"))
    }

    @Test
    fun phone_isMasked() {
        assertEquals("138****5678", RuntimeSanitizer.maskPhone("13812345678"))
    }

    @Test
    fun shortPhone_isMasked() {
        assertEquals("****", RuntimeSanitizer.maskPhone("12345"))
    }

    @Test
    fun email_isMasked() {
        assertEquals("t***@example.com", RuntimeSanitizer.maskEmail("test@example.com"))
    }

    @Test
    fun sanitize_detectsPhonePattern() {
        val params = mapOf("value" to "13812345678")
        val result = RuntimeSanitizer.sanitize(params)
        assertEquals("138****5678", result["value"])
    }

    @Test
    fun sanitize_detectsEmailPattern() {
        val params = mapOf("value" to "test@example.com")
        val result = RuntimeSanitizer.sanitize(params)
        assertEquals("t***@example.com", result["value"])
    }

    @Test
    fun sanitize_emptyMap_returnsEmpty() {
        assertTrue(RuntimeSanitizer.sanitize(emptyMap()).isEmpty())
    }

    @Test
    fun sanitize_9digitPhone_isMasked() {
        val params = mapOf("value" to "123456789")
        val result = RuntimeSanitizer.sanitize(params)
        assertEquals("123****6789", result["value"])
    }

    @Test
    fun sanitize_8digitNumber_notDetectedAsPhone() {
        val params = mapOf("value" to "12345678")
        val result = RuntimeSanitizer.sanitize(params)
        assertEquals("12345678", result["value"])
    }

    @Test
    fun sanitize_singleCharEmail_isMasked() {
        val params = mapOf("value" to "a@b.co")
        val result = RuntimeSanitizer.sanitize(params)
        assertEquals("***@b.co", result["value"])
    }
}
