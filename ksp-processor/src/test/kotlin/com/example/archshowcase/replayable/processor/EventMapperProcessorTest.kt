package com.example.archshowcase.replayable.processor

import org.junit.Test
import kotlin.test.assertEquals

class EventMapperProcessorTest {

    @Test
    fun toSnakeCase_standardCamelCase() {
        assertEquals("chat_room", toSnakeCase("ChatRoom"))
    }

    @Test
    fun toSnakeCase_singleWord() {
        assertEquals("login", toSnakeCase("Login"))
    }

    @Test
    fun toSnakeCase_multipleWords() {
        assertEquals("send_text", toSnakeCase("SendText"))
    }

    @Test
    fun toSnakeCase_consecutiveUppercase() {
        assertEquals("send_im_message", toSnakeCase("SendIMMessage"))
    }

    @Test
    fun toSnakeCase_allUpperAcronymStore() {
        assertEquals("rtc_store", toSnakeCase("RTCStore"))
    }

    @Test
    fun toSnakeCase_allUpperAcronymOnly() {
        assertEquals("rtc", toSnakeCase("RTC"))
    }

    @Test
    fun toSnakeCase_alreadyLowercase() {
        assertEquals("hello", toSnakeCase("hello"))
    }

    @Test
    fun toSnakeCase_emptyString() {
        assertEquals("", toSnakeCase(""))
    }

    @Test
    fun toSnakeCase_toggleInputMode() {
        assertEquals("toggle_input_mode", toSnakeCase("ToggleInputMode"))
    }

    @Test
    fun sensitivityFor_textField_isLengthOnly() {
        assertEquals(SensitivityLevel.LENGTH_ONLY, sensitivityFor("messageText"))
    }

    @Test
    fun sensitivityFor_urlField_isExclude() {
        assertEquals(SensitivityLevel.EXCLUDE, sensitivityFor("imageUrl"))
    }

    @Test
    fun sensitivityFor_emailField_isHash() {
        assertEquals(SensitivityLevel.HASH, sensitivityFor("email"))
    }

    @Test
    fun sensitivityFor_plainField_isPlain() {
        assertEquals(SensitivityLevel.PLAIN, sensitivityFor("id"))
    }

    @Test
    fun sensitivityFor_authExact_isExclude() {
        assertEquals(SensitivityLevel.EXCLUDE, sensitivityFor("auth"))
    }

    @Test
    fun sensitivityFor_authorId_isPlain() {
        assertEquals(SensitivityLevel.PLAIN, sensitivityFor("authorId"))
    }
}
