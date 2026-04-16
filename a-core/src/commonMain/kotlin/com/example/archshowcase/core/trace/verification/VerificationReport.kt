package com.example.archshowcase.core.trace.verification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 验证层级
 */
@Serializable
enum class VerificationLevel {
    SSIM      // SSIM 截图对比
}

/**
 * 单条验证结果
 */
@Serializable
data class VerificationItem(
    val level: VerificationLevel,
    @SerialName("store_name") val storeName: String,
    val field: String,
    val expected: String,
    val actual: String,
    val passed: Boolean,
    val message: String = ""
)

/**
 * 单个验证层级的汇总结果
 */
@Serializable
data class LevelResult(
    val level: VerificationLevel,
    val passed: Boolean,
    val items: List<VerificationItem>
) {
    val failedCount: Int get() = items.count { !it.passed }
    val totalCount: Int get() = items.size
}

/**
 * 完整验证报告
 */
@Serializable
data class VerificationReport(
    @SerialName("scenario_name") val scenarioName: String,
    @SerialName("level_results") val levelResults: List<LevelResult>,
    @SerialName("overall_passed") val overallPassed: Boolean = levelResults.all { it.passed },
    @SerialName("error_message") val errorMessage: String? = null
) {
    companion object {
        internal val json = Json { prettyPrint = true; encodeDefaults = true }

        fun error(scenarioName: String, message: String) = VerificationReport(
            scenarioName = scenarioName,
            levelResults = emptyList(),
            overallPassed = false,
            errorMessage = message
        )
    }
}

fun VerificationReport.toJson(): String =
    VerificationReport.json.encodeToString(VerificationReport.serializer(), this)

fun VerificationReport.toMarkdown(): String = buildString {
    appendLine("# Verification Report: $scenarioName")
    appendLine()
    appendLine("**Overall**: ${if (overallPassed) "PASSED" else "FAILED"}")
    errorMessage?.let { appendLine("**Error**: $it") }
    appendLine()
    appendLine("| Level | Passed | Total | Failed |")
    appendLine("|-------|--------|-------|--------|")
    levelResults.forEach { lr ->
        appendLine("| ${lr.level} | ${lr.passed} | ${lr.totalCount} | ${lr.failedCount} |")
    }
    val failedItems = levelResults.flatMap { it.items.filter { i -> !i.passed } }
    if (failedItems.isNotEmpty()) {
        appendLine()
        appendLine("## Failed Items")
        appendLine()
        failedItems.forEach { item ->
            appendLine("- **${item.storeName}.${item.field}**: expected=`${item.expected}`, actual=`${item.actual}` ${item.message}")
        }
    }
}
