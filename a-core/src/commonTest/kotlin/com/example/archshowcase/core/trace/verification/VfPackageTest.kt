package com.example.archshowcase.core.trace.verification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VfPackageTest {

    private fun createTestManifest() = VfManifest(
        name = "image_demo_scroll",
        description = "Demo 主界面 → 图片加载 → 滚动",
        verificationText = "正常进入图片列表页且发生了滚动",
        platform = "android",
        intents = listOf(
            VfIntent(
                store = "DemoNavigationStore",
                intentType = "Push",
                params = mapOf("route" to "ImageDemo"),
                delayAfterMs = 3000,
                note = "等待图片加载"
            ),
            VfIntent(
                store = "ImageDemoStore",
                intentType = "UpdateScrollPosition",
                params = mapOf("firstVisibleIndex" to "5", "offset" to "100"),
                delayAfterMs = 1000
            )
        ),
        covers = listOf("DemoNavigationStore", "ImageDemoStore")
    )

    // ─── Manifest 序列化 ─────────────────────────────────────────

    @Test
    fun `manifest serialization round-trip preserves data`() {
        val manifest = createTestManifest()
        val jsonStr = VfPackager.serializeManifest(manifest)
        val parsed = VfPackager.parseManifest(jsonStr).getOrThrow()

        assertEquals(manifest.name, parsed.name)
        assertEquals(manifest.description, parsed.description)
        assertEquals(manifest.verificationText, parsed.verificationText)
        assertEquals(manifest.intents.size, parsed.intents.size)
        assertEquals(manifest.intents[0].store, parsed.intents[0].store)
    }

    @Test
    fun `manifest JSON contains expected fields`() {
        val manifest = createTestManifest()
        val jsonStr = VfPackager.serializeManifest(manifest)

        assertTrue(jsonStr.contains("\"name\""))
        assertTrue(jsonStr.contains("\"verification_text\""))
        assertTrue(jsonStr.contains("\"intent_type\""))
        assertTrue(jsonStr.contains("\"delay_after_ms\""))
    }

    @Test
    fun `manifest defaults are applied`() {
        val minimal = VfManifest(name = "test")
        val jsonStr = VfPackager.serializeManifest(minimal)
        val parsed = VfPackager.parseManifest(jsonStr).getOrThrow()

        assertEquals("ttr", parsed.tteFormat)
        assertEquals(0.95, parsed.screenshotCompare.ssimThreshold)
        assertEquals("sequence", parsed.networkTape.matchStrategy)
    }

    @Test
    fun `parseManifest returns failure for invalid JSON`() {
        val result = VfPackager.parseManifest("not json")
        assertTrue(result.isFailure)
    }

    // ─── VF 包打包/解析 ─────────────────────────────────────────

    @Test
    fun `pack and parse round-trip preserves data`() {
        val manifest = createTestManifest()
        val startTte = "start-tte-bytes".encodeToByteArray()
        val endTte = "end-tte-bytes".encodeToByteArray()

        val vf = VfPackage(
            manifest = manifest,
            startTteBytes = startTte,
            endTteBytes = endTte
        )

        val files = VfPackager.pack(vf)
        assertEquals(3, files.size)
        assertTrue(files.containsKey("manifest.json"))
        assertTrue(files.containsKey("start.tte"))
        assertTrue(files.containsKey("end.tte"))

        val parsed = VfPackager.parse(files).getOrThrow()
        assertEquals(vf, parsed)
    }

    @Test
    fun `parse fails when manifest is missing`() {
        val files = mapOf(
            "start.tte" to ByteArray(0),
            "end.tte" to ByteArray(0)
        )
        val result = VfPackager.parse(files)
        assertTrue(result.isFailure)
    }

    @Test
    fun `parse fails when start tte is missing`() {
        val manifest = VfPackager.serializeManifest(VfManifest(name = "test")).encodeToByteArray()
        val files = mapOf(
            "manifest.json" to manifest,
            "end.tte" to ByteArray(0)
        )
        val result = VfPackager.parse(files)
        assertTrue(result.isFailure)
    }

    @Test
    fun `parse fails when end tte is missing`() {
        val manifest = VfPackager.serializeManifest(VfManifest(name = "test")).encodeToByteArray()
        val files = mapOf(
            "manifest.json" to manifest,
            "start.tte" to ByteArray(0)
        )
        val result = VfPackager.parse(files)
        assertTrue(result.isFailure)
    }

    // ─── covers 字段 ───────────────────────────────────────────

    @Test
    fun `manifest covers round-trip preserves data`() {
        val manifest = createTestManifest()
        val jsonStr = VfPackager.serializeManifest(manifest)
        val parsed = VfPackager.parseManifest(jsonStr).getOrThrow()

        assertEquals(manifest.covers, parsed.covers)
        assertEquals("DemoNavigationStore", parsed.covers[0])
        assertEquals("ImageDemoStore", parsed.covers[1])
    }

    // ─── VfPackage equals/hashCode ──────────────────────────────

    @Test
    fun `VfPackage equality checks byte content`() {
        val manifest = VfManifest(name = "test")
        val a = VfPackage(manifest, "abc".encodeToByteArray(), "def".encodeToByteArray())
        val b = VfPackage(manifest, "abc".encodeToByteArray(), "def".encodeToByteArray())
        val c = VfPackage(manifest, "abc".encodeToByteArray(), "xyz".encodeToByteArray())

        assertEquals(a, b)
        assertTrue(a != c)
    }
}
