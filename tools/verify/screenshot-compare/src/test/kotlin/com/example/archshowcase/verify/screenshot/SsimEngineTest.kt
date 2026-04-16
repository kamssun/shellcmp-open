package com.example.archshowcase.verify.screenshot

import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SsimEngineTest {

    private fun createSolidImage(width: Int, height: Int, color: Color): BufferedImage {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.color = color
        g.fillRect(0, 0, width, height)
        g.dispose()
        return img
    }

    @Test
    fun `identical images return SSIM of 1_0`() {
        val img = createSolidImage(100, 100, Color.RED)
        val result = SsimEngine.compare(img, img, threshold = 0.95)
        assertEquals(1.0, result.score, 0.001)
        assertTrue(result.passed)
    }

    @Test
    fun `completely different images return low SSIM`() {
        val black = createSolidImage(100, 100, Color.BLACK)
        val white = createSolidImage(100, 100, Color.WHITE)
        val result = SsimEngine.compare(black, white, threshold = 0.95)
        assertTrue(result.score < 0.1)
        assertFalse(result.passed)
    }

    @Test
    fun `similar images with small difference pass threshold`() {
        val img1 = createSolidImage(100, 100, Color(128, 128, 128))
        val img2 = createSolidImage(100, 100, Color(130, 130, 130))
        val result = SsimEngine.compare(img1, img2, threshold = 0.9)
        assertTrue(result.score > 0.9)
        assertTrue(result.passed)
    }

    @Test
    fun `mask region excludes pixels from calculation`() {
        val img1 = createSolidImage(100, 100, Color.GRAY)
        // 创建一个右半部分不同的图片
        val img2 = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
        val g = img2.createGraphics()
        g.color = Color.GRAY
        g.fillRect(0, 0, 50, 100)
        g.color = Color.WHITE
        g.fillRect(50, 0, 50, 100)
        g.dispose()

        // 不 mask → 分数较低
        val withoutMask = SsimEngine.compare(img1, img2, threshold = 0.5)

        // mask 右半部分 → 分数应该更高
        val withMask = SsimEngine.compare(
            img1, img2, threshold = 0.5,
            maskRegions = listOf(MaskRegion(50, 0, 50, 100))
        )
        assertTrue(withMask.score > withoutMask.score)
    }

    @Test
    fun `diff map is generated when requested`() {
        val img = createSolidImage(100, 100, Color.BLUE)
        val result = SsimEngine.compare(img, img, generateDiffMap = true)
        assertTrue(result.diffMap != null)
        assertEquals(100, result.diffMap!!.size)
    }

    @Test
    fun `MaskRegion parses correctly`() {
        val region = MaskRegion.parse("10,20,30,40")
        assertEquals(MaskRegion(10, 20, 30, 40), region)
    }
}
