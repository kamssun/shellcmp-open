package com.example.archshowcase.verify.screenshot

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * 差异热力图 PNG 生成器
 *
 * SSIM 值高（相似）→ 绿色，低（差异大）→ 红色
 */
object DiffHeatmap {

    /**
     * 从 SSIM diffMap 生成热力图
     *
     * @param diffMap 每个像素位置的局部 SSIM 值（0~1）
     * @param outputFile 输出 PNG 文件
     */
    fun generate(diffMap: Array<DoubleArray>, outputFile: File) {
        val height = diffMap.size
        val width = if (height > 0) diffMap[0].size else 0
        if (width == 0 || height == 0) return

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val ssim = diffMap[y][x].coerceIn(0.0, 1.0)
                val color = ssimToColor(ssim)
                image.setRGB(x, y, color.rgb)
            }
        }

        outputFile.parentFile?.mkdirs()
        ImageIO.write(image, "PNG", outputFile)
    }

    /**
     * SSIM → 颜色映射
     * 1.0 (完全匹配) → 绿色
     * 0.5 → 黄色
     * 0.0 (完全不同) → 红色
     */
    private fun ssimToColor(ssim: Double): Color {
        val r: Float
        val g: Float
        if (ssim > 0.5) {
            r = (2.0 * (1.0 - ssim)).toFloat()
            g = 1.0f
        } else {
            r = 1.0f
            g = (2.0 * ssim).toFloat()
        }
        return Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), 0f)
    }
}
