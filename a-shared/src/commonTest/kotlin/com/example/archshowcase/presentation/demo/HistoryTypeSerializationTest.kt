package com.example.archshowcase.presentation.demo

import com.example.archshowcase.presentation.demo.image.ImageHistoryType
import com.example.archshowcase.presentation.demo.network.NetworkHistoryType
import com.example.archshowcase.presentation.demo.obo.OBOHistoryType
import com.example.archshowcase.presentation.navigation.NavHistoryType
import com.example.archshowcase.presentation.settings.SettingsHistoryType
import com.example.archshowcase.core.trace.scroll.ScrollPosition
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies @SerialName discriminator configuration for polymorphic sealed hierarchies.
 * This catches: missing @SerialName annotations, duplicate discriminators, broken polymorphic dispatch.
 */
class HistoryTypeSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `SettingsHistoryType polymorphic round-trip`() {
        val original: SettingsHistoryType = SettingsHistoryType.SetOBOScheduler(true)
        val encoded = json.encodeToString<SettingsHistoryType>(original)
        val decoded = json.decodeFromString<SettingsHistoryType>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `OBOHistoryType polymorphic round-trip`() {
        val original: OBOHistoryType = OBOHistoryType.SetEffects(5)
        val encoded = json.encodeToString<OBOHistoryType>(original)
        val decoded = json.decodeFromString<OBOHistoryType>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `NetworkHistoryType polymorphic round-trip`() {
        val original: NetworkHistoryType = NetworkHistoryType.Request(count = 1, result = "ok")
        val encoded = json.encodeToString<NetworkHistoryType>(original)
        val decoded = json.decodeFromString<NetworkHistoryType>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `NavHistoryType polymorphic round-trip`() {
        val original: NavHistoryType = NavHistoryType.Push("Home")
        val encoded = json.encodeToString<NavHistoryType>(original)
        val decoded = json.decodeFromString<NavHistoryType>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `ImageHistoryType polymorphic round-trip`() {
        val original: ImageHistoryType = ImageHistoryType.Scroll(
            ScrollPosition(5, 100)
        )
        val encoded = json.encodeToString<ImageHistoryType>(original)
        val decoded = json.decodeFromString<ImageHistoryType>(encoded)
        assertEquals(original, decoded)
    }
}
