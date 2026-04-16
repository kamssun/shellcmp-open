package com.example.archshowcase.core.trace.verification

import kotlinx.serialization.json.Json

/**
 * VF (Verification File) 包
 *
 * Phase 1 内存模型：包含 manifest + TTE 字节数据。
 * Phase 2 将支持文件系统目录格式。
 */
data class VfPackage(
    val manifest: VfManifest,
    val startTteBytes: ByteArray,
    val endTteBytes: ByteArray,
    val extraFiles: Map<String, ByteArray> = emptyMap(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VfPackage) return false
        return manifest == other.manifest &&
            startTteBytes.contentEquals(other.startTteBytes) &&
            endTteBytes.contentEquals(other.endTteBytes) &&
            extraFiles.keys == other.extraFiles.keys &&
            extraFiles.all { (k, v) -> other.extraFiles[k]?.contentEquals(v) == true }
    }

    override fun hashCode(): Int {
        var result = manifest.hashCode()
        result = 31 * result + startTteBytes.contentHashCode()
        result = 31 * result + endTteBytes.contentHashCode()
        result = 31 * result + extraFiles.keys.hashCode()
        return result
    }
}

/**
 * VF 打包器 / 解析器
 *
 * Phase 1：内存中的序列化/反序列化。
 * Phase 2：文件系统目录格式。
 */
object VfPackager {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    /**
     * 将 VF 包序列化为 Map（模拟目录结构）
     */
    fun pack(vf: VfPackage): Map<String, ByteArray> {
        val manifestBytes = json.encodeToString(VfManifest.serializer(), vf.manifest).encodeToByteArray()
        return buildMap {
            put("manifest.json", manifestBytes)
            put("start.tte", vf.startTteBytes)
            put("end.tte", vf.endTteBytes)
            putAll(vf.extraFiles)
        }
    }

    /**
     * 从 Map（目录结构）解析 VF 包
     */
    private val CORE_FILES = setOf("manifest.json", "start.tte", "end.tte")

    fun parse(files: Map<String, ByteArray>): Result<VfPackage> = runCatching {
        val manifestBytes = files["manifest.json"]
            ?: error("manifest.json not found in VF package")
        val manifest = json.decodeFromString(VfManifest.serializer(), manifestBytes.decodeToString())

        val startTte = files["start.tte"]
            ?: error("start.tte not found in VF package")
        val endTte = files["end.tte"]
            ?: error("end.tte not found in VF package")

        val extras = files.filterKeys { it !in CORE_FILES }

        VfPackage(
            manifest = manifest,
            startTteBytes = startTte,
            endTteBytes = endTte,
            extraFiles = extras,
        )
    }

    /**
     * 序列化 manifest 为 JSON 字符串
     */
    fun serializeManifest(manifest: VfManifest): String {
        return json.encodeToString(VfManifest.serializer(), manifest)
    }

    /**
     * 反序列化 manifest JSON
     */
    fun parseManifest(jsonString: String): Result<VfManifest> = runCatching {
        json.decodeFromString(VfManifest.serializer(), jsonString)
    }
}
