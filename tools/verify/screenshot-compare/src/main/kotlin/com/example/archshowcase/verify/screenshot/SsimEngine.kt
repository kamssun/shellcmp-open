package com.example.archshowcase.verify.screenshot

import java.awt.image.BufferedImage

/**
 * SSIM (Structural Similarity Index) 计算引擎
 *
 * 11x11 滑动窗口，灰度图对比。mask_regions 内像素跳过。
 */
object SsimEngine {

    private const val WINDOW_SIZE = 11
    private const val C1 = 6.5025   // (0.01 * 255)^2
    private const val C2 = 58.5225  // (0.03 * 255)^2

    data class SsimResult(
        val score: Double,
        val passed: Boolean,
        val threshold: Double,
        val diffMap: Array<DoubleArray>? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SsimResult) return false
            return score == other.score && passed == other.passed && threshold == other.threshold
        }
        override fun hashCode(): Int {
            var result = score.hashCode()
            result = 31 * result + passed.hashCode()
            result = 31 * result + threshold.hashCode()
            return result
        }
    }

    /**
     * 计算两张图片的 SSIM
     *
     * @param actual 实际截图
     * @param baseline 基线截图
     * @param threshold SSIM 阈值（默认 0.95）
     * @param maskRegions 跳过的区域列表 "x,y,w,h"
     * @param generateDiffMap 是否生成差异 map
     */
    fun compare(
        actual: BufferedImage,
        baseline: BufferedImage,
        threshold: Double = 0.95,
        maskRegions: List<MaskRegion> = emptyList(),
        generateDiffMap: Boolean = false
    ): SsimResult {
        require(actual.width == baseline.width && actual.height == baseline.height) {
            "Image dimensions must match: actual=${actual.width}x${actual.height}, baseline=${baseline.width}x${baseline.height}"
        }

        val width = actual.width
        val height = actual.height
        val grayA = toGrayscale(actual)
        val grayB = toGrayscale(baseline)
        val mask = buildMask(width, height, maskRegions)

        val halfWin = WINDOW_SIZE / 2
        var ssimSum = 0.0
        var count = 0
        val diffMap = if (generateDiffMap) Array(height) { DoubleArray(width) { 1.0 } } else null

        for (y in halfWin until height - halfWin) {
            for (x in halfWin until width - halfWin) {
                if (mask[y][x]) continue

                val (meanA, meanB, varA, varB, covAB) = windowStats(grayA, grayB, x, y, halfWin)

                val numerator = (2 * meanA * meanB + C1) * (2 * covAB + C2)
                val denominator = (meanA * meanA + meanB * meanB + C1) * (varA + varB + C2)
                val localSsim = numerator / denominator

                ssimSum += localSsim
                count++
                diffMap?.let { it[y][x] = localSsim }
            }
        }

        val score = if (count > 0) ssimSum / count else 1.0
        return SsimResult(
            score = score,
            passed = score >= threshold,
            threshold = threshold,
            diffMap = diffMap
        )
    }

    private fun toGrayscale(image: BufferedImage): Array<DoubleArray> {
        val w = image.width
        val h = image.height
        return Array(h) { y ->
            DoubleArray(w) { x ->
                val rgb = image.getRGB(x, y)
                val r = (rgb shr 16) and 0xFF
                val g = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF
                0.299 * r + 0.587 * g + 0.114 * b
            }
        }
    }

    private fun buildMask(
        width: Int,
        height: Int,
        regions: List<MaskRegion>
    ): Array<BooleanArray> {
        val mask = Array(height) { BooleanArray(width) }
        for (region in regions) {
            for (y in region.y until minOf(region.y + region.h, height)) {
                for (x in region.x until minOf(region.x + region.w, width)) {
                    mask[y][x] = true
                }
            }
        }
        return mask
    }

    private data class WindowStats(
        val meanA: Double,
        val meanB: Double,
        val varA: Double,
        val varB: Double,
        val covAB: Double
    )

    private fun windowStats(
        grayA: Array<DoubleArray>,
        grayB: Array<DoubleArray>,
        cx: Int, cy: Int, halfWin: Int
    ): WindowStats {
        var sumA = 0.0; var sumB = 0.0
        var sumA2 = 0.0; var sumB2 = 0.0; var sumAB = 0.0
        var n = 0

        for (dy in -halfWin..halfWin) {
            for (dx in -halfWin..halfWin) {
                val a = grayA[cy + dy][cx + dx]
                val b = grayB[cy + dy][cx + dx]
                sumA += a; sumB += b
                sumA2 += a * a; sumB2 += b * b; sumAB += a * b
                n++
            }
        }

        val meanA = sumA / n
        val meanB = sumB / n
        val varA = sumA2 / n - meanA * meanA
        val varB = sumB2 / n - meanB * meanB
        val covAB = sumAB / n - meanA * meanB

        return WindowStats(meanA, meanB, varA, varB, covAB)
    }
}

data class MaskRegion(val x: Int, val y: Int, val w: Int, val h: Int) {
    companion object {
        fun parse(s: String): MaskRegion {
            val parts = s.split(",").map { it.trim().toInt() }
            require(parts.size == 4) { "MaskRegion format: x,y,w,h" }
            return MaskRegion(parts[0], parts[1], parts[2], parts[3])
        }
    }
}
