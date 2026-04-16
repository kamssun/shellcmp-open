package com.example.archshowcase.verify.screenshot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * SSIM 截图对比报告生成器
 */
object ReportGenerator {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    data class UnifiedReport(
        val scenario: String,
        @SerialName("overall_passed") val overallPassed: Boolean,
        val levels: List<LevelSummary>
    )

    @Serializable
    data class LevelSummary(
        val level: String,
        val passed: Boolean,
        val total: Int = 0,
        val failed: Int = 0,
        val ssim: Double = 0.0,
        val threshold: Double = 0.0,
        @SerialName("ai_verdict") val aiVerdict: String = ""
    )

    /**
     * 从 SSIM 结果文件生成统一报告
     *
     * @param ssimFiles (label, file) 对列表，如 [("SSIM_START", file1), ("SSIM_END", file2)]
     */
    fun generate(
        scenarioName: String,
        ssimFiles: List<Pair<String, File>>,
        outputDir: File
    ) {
        val levels = mutableListOf<LevelSummary>()

        for ((label, f) in ssimFiles) {
            if (f.exists()) {
                val ssimResult = json.decodeFromString<SsimResult>(f.readText())
                levels.add(LevelSummary(
                    level = label,
                    passed = ssimResult.passed,
                    ssim = ssimResult.score,
                    threshold = ssimResult.threshold
                ))
            }
        }

        val overallPassed = levels.all { it.passed }
        val report = UnifiedReport(
            scenario = scenarioName,
            overallPassed = overallPassed,
            levels = levels
        )

        outputDir.mkdirs()

        // JSON
        val jsonOutput = json.encodeToString(UnifiedReport.serializer(), report)
        File(outputDir, "report.json").writeText(jsonOutput)

        // Markdown
        File(outputDir, "report.md").writeText(toMarkdown(report))
    }

    private fun toMarkdown(report: UnifiedReport): String = buildString {
        appendLine("# Verification Report: ${report.scenario}")
        appendLine()
        appendLine("**Overall**: ${if (report.overallPassed) "PASSED" else "FAILED"}")
        appendLine()
        appendLine("| Level | Passed | SSIM | Threshold |")
        appendLine("|-------|--------|------|-----------|")
        report.levels.forEach { l ->
            appendLine("| ${l.level} | ${l.passed} | ${if (l.ssim > 0) "%.4f".format(l.ssim) else "-"} | ${if (l.threshold > 0) l.threshold else "-"} |")
        }
    }

    @Serializable
    data class SsimResult(
        val score: Double,
        val passed: Boolean,
        val threshold: Double,
        @SerialName("diff_image") val diffImage: String = ""
    )
}
