package com.example.archshowcase.core.trace.verification

import kotlin.test.Test
import kotlin.test.assertEquals

class IntentParamsExtractorTest {

    @Test
    fun `data object without parentheses returns empty params`() {
        val result = IntentParamsExtractor.parseToString("LoadInitial", "LoadInitial")
        assertEquals(emptyMap(), result)
    }

    @Test
    fun `data object Pop returns empty params`() {
        val result = IntentParamsExtractor.parseToString("Pop", "Pop")
        assertEquals(emptyMap(), result)
    }

    @Test
    fun `single param extraction`() {
        val result = IntentParamsExtractor.parseToString("Push(route=ImageDemo)", "Push")
        assertEquals(mapOf("route" to "ImageDemo"), result)
    }

    @Test
    fun `multiple params extraction`() {
        val result = IntentParamsExtractor.parseToString(
            "UpdateScrollPosition(firstVisibleIndex=5, offset=100)",
            "UpdateScrollPosition"
        )
        assertEquals(mapOf("firstVisibleIndex" to "5", "offset" to "100"), result)
    }

    @Test
    fun `nested brackets in value`() {
        val result = IntentParamsExtractor.parseToString(
            "ReplaceAll(routes=[Home, ImageDemo])",
            "ReplaceAll"
        )
        assertEquals(mapOf("routes" to "[Home, ImageDemo]"), result)
    }

    @Test
    fun `nested parentheses in value`() {
        val result = IntentParamsExtractor.parseToString(
            "SetConfig(config=Config(a=1, b=2))",
            "SetConfig"
        )
        assertEquals(mapOf("config" to "Config(a=1, b=2)"), result)
    }

    @Test
    fun `empty parentheses returns empty params`() {
        val result = IntentParamsExtractor.parseToString("Foo()", "Foo")
        assertEquals(emptyMap(), result)
    }

    @Test
    fun `extract returns intentType and params`() {
        data class TestIntent(val id: Int, val name: String)

        val (type, params) = IntentParamsExtractor.extract("TestStore", TestIntent(42, "hello"))
        assertEquals("TestIntent", type)
        assertEquals("42", params["id"])
        assertEquals("hello", params["name"])
    }

    @Test
    fun `extract data object returns empty params`() {
        val intent = object {
            override fun toString() = "Reload"
        }
        val (_, params) = IntentParamsExtractor.extract("TestStore", intent)
        assertEquals(emptyMap(), params)
    }

    @Test
    fun `malformed string without closing paren returns empty`() {
        val result = IntentParamsExtractor.parseToString("Broken(key=val", "Broken")
        assertEquals(emptyMap(), result)
    }

    @Test
    fun `value without equals sign is skipped`() {
        val result = IntentParamsExtractor.parseToString("Foo(bareValue)", "Foo")
        assertEquals(emptyMap(), result)
    }
}
