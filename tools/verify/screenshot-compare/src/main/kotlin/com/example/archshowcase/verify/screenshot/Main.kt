package com.example.archshowcase.verify.screenshot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.imageio.ImageIO

/**
 * CLI 入口
 *
 * 两种模式：
 * 1. 截图对比: --actual <file> --baseline <file> --output <dir> [--threshold 0.95] [--mask x,y,w,h] [--save-baseline]
 * 2. 报告整合: --report --manifest <file> [--ssim <file>] --output <dir>
 */
fun main(args: Array<String>) {
    val argMap = parseArgs(args)

    if ("report" in argMap) {
        runReport(argMap)
    } else {
        runScreenshotCompare(argMap)
    }
}

private fun runScreenshotCompare(args: Map<String, String>) {
    val actualPath = args["actual"] ?: error("Missing --actual")
    val outputDir = File(args["output"] ?: error("Missing --output"))
    val threshold = args["threshold"]?.toDoubleOrNull() ?: 0.95
    val masks = args["mask"]?.split(";")?.map { MaskRegion.parse(it) } ?: emptyList()
    val saveBaseline = "save-baseline" in args

    val actualFile = File(actualPath)
    require(actualFile.exists()) { "Actual file not found: $actualPath" }

    val baselinePath = args["baseline"]
    val baselineFile = baselinePath?.let { File(it) }

    // 如果没有基线且指定了 save-baseline，保存当前截图为基线
    if (baselineFile == null || !baselineFile.exists()) {
        if (saveBaseline) {
            outputDir.mkdirs()
            val baselineTarget = File(outputDir, "baseline.png")
            actualFile.copyTo(baselineTarget, overwrite = true)
            println("""{"score": 1.0, "passed": true, "threshold": $threshold, "baseline_saved": "${baselineTarget.absolutePath}"}""")
            return
        }
        error("Baseline file not found: $baselinePath. Use --save-baseline on first run.")
    }

    val actual = ImageIO.read(actualFile)
    val baseline = ImageIO.read(baselineFile)

    val result = SsimEngine.compare(
        actual = actual,
        baseline = baseline,
        threshold = threshold,
        maskRegions = masks,
        generateDiffMap = true
    )

    outputDir.mkdirs()

    // 生成差异热力图
    val diffImagePath = if (result.diffMap != null) {
        val diffFile = File(outputDir, "diff_heatmap.png")
        DiffHeatmap.generate(result.diffMap, diffFile)
        diffFile.name
    } else ""

    // 输出 SSIM JSON
    val json = Json { prettyPrint = true; encodeDefaults = true }
    val ssimResult = SsimOutput(
        score = result.score,
        passed = result.passed,
        threshold = result.threshold,
        diffImage = diffImagePath
    )
    val outputJson = json.encodeToString(SsimOutput.serializer(), ssimResult)
    File(outputDir, "result_ssim.json").writeText(outputJson)
    println(outputJson)
}

private fun runReport(args: Map<String, String>) {
    val outputDir = File(args["output"] ?: error("Missing --output"))
    val manifestFile = args["manifest"]?.let { File(it) }
    val scenarioName = if (manifestFile?.exists() == true) {
        val json = Json { ignoreUnknownKeys = true }
        val manifest = json.decodeFromString<ManifestName>(manifestFile.readText())
        manifest.name
    } else "unknown"

    val ssimFiles = mutableListOf<Pair<String, File>>()
    args["ssim-start"]?.let { ssimFiles.add("SSIM_START" to File(it)) }
    args["ssim-end"]?.let { ssimFiles.add("SSIM_END" to File(it)) }
    // 兼容旧的 --ssim 单参数
    if (ssimFiles.isEmpty()) {
        args["ssim"]?.let { ssimFiles.add("SSIM" to File(it)) }
    }

    ReportGenerator.generate(
        scenarioName = scenarioName,
        ssimFiles = ssimFiles,
        outputDir = outputDir
    )
    println("Report generated in: ${outputDir.absolutePath}")
}

private fun parseArgs(args: Array<String>): Map<String, String> {
    val result = mutableMapOf<String, String>()
    var i = 0
    while (i < args.size) {
        val key = args[i].removePrefix("--")
        if (i + 1 < args.size && !args[i + 1].startsWith("--")) {
            result[key] = args[i + 1]
            i += 2
        } else {
            result[key] = ""
            i++
        }
    }
    return result
}

@Serializable
private data class SsimOutput(
    val score: Double,
    val passed: Boolean,
    val threshold: Double,
    @SerialName("diff_image") val diffImage: String = ""
)

@Serializable
private data class ManifestName(val name: String = "unknown")
