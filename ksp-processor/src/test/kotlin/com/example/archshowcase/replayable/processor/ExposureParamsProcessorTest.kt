package com.example.archshowcase.replayable.processor

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExposureParamsProcessorTest {

    @Test
    fun sensitivityFor_url_field_is_excluded() {
        assertEquals(SensitivityLevel.EXCLUDE, sensitivityFor("imageUrl"))
        assertEquals(SensitivityLevel.EXCLUDE, sensitivityFor("profileUrl"))
        assertEquals(SensitivityLevel.EXCLUDE, sensitivityFor("link"))
        assertEquals(SensitivityLevel.EXCLUDE, sensitivityFor("uri"))
    }

    @Test
    fun sensitivityFor_text_field_is_lengthOnly() {
        assertEquals(SensitivityLevel.LENGTH_ONLY, sensitivityFor("messageText"))
        assertEquals(SensitivityLevel.LENGTH_ONLY, sensitivityFor("content"))
        assertEquals(SensitivityLevel.LENGTH_ONLY, sensitivityFor("body"))
    }

    @Test
    fun sensitivityFor_email_field_is_hash() {
        assertEquals(SensitivityLevel.HASH, sensitivityFor("email"))
        assertEquals(SensitivityLevel.HASH, sensitivityFor("userPhone"))
        assertEquals(SensitivityLevel.HASH, sensitivityFor("mobile"))
    }

    @Test
    fun sensitivityFor_plain_field_is_plain() {
        assertEquals(SensitivityLevel.PLAIN, sensitivityFor("id"))
        assertEquals(SensitivityLevel.PLAIN, sensitivityFor("title"))
        assertEquals(SensitivityLevel.PLAIN, sensitivityFor("count"))
        assertEquals(SensitivityLevel.PLAIN, sensitivityFor("price"))
    }

    @Test
    fun valueExpression_plain_string_returns_direct_ref() {
        val prop = ParamsCodeGenerator.PropertyEntry("name", "kotlin.String", SensitivityLevel.PLAIN)
        assertEquals("item.name", ParamsCodeGenerator.valueExpression(prop, "item."))
    }

    @Test
    fun valueExpression_plain_int_returns_toString() {
        val prop = ParamsCodeGenerator.PropertyEntry("count", "kotlin.Int", SensitivityLevel.PLAIN)
        assertEquals("count.toString()", ParamsCodeGenerator.valueExpression(prop, ""))
    }

    @Test
    fun valueExpression_lengthOnly_returns_len_template() {
        val prop = ParamsCodeGenerator.PropertyEntry("content", "kotlin.String", SensitivityLevel.LENGTH_ONLY)
        assertEquals("\"[len=\${item.content.length}]\"", ParamsCodeGenerator.valueExpression(prop, "item."))
    }

    @Test
    fun valueExpression_hash_returns_sha256Hex16() {
        val prop = ParamsCodeGenerator.PropertyEntry("email", "kotlin.String", SensitivityLevel.HASH)
        assertEquals("sha256Hex16(email)", ParamsCodeGenerator.valueExpression(prop, ""))
    }

    @Test
    fun needsHashImport_true_when_hash_present() {
        val props = listOf(
            ParamsCodeGenerator.PropertyEntry("id", "kotlin.String", SensitivityLevel.PLAIN),
            ParamsCodeGenerator.PropertyEntry("email", "kotlin.String", SensitivityLevel.HASH),
        )
        assertTrue(ParamsCodeGenerator.needsHashImport(props))
    }

    @Test
    fun needsHashImport_false_when_no_hash() {
        val props = listOf(
            ParamsCodeGenerator.PropertyEntry("id", "kotlin.String", SensitivityLevel.PLAIN),
            ParamsCodeGenerator.PropertyEntry("text", "kotlin.String", SensitivityLevel.LENGTH_ONLY),
        )
        assertTrue(!ParamsCodeGenerator.needsHashImport(props))
    }

    @Test
    fun toSnakeCase_dataClassName() {
        assertEquals("image_item", toSnakeCase("ImageItem"))
        assertEquals("product", toSnakeCase("Product"))
        assertEquals("gift_card_item", toSnakeCase("GiftCardItem"))
    }
}
