package com.example.archshowcase.core.trace.verification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * VF (Verification File) 的 manifest.json 数据模型
 *
 * 描述验证场景的元数据、Intent 序列和验证配置。
 */
@Serializable
data class VfManifest(
    val name: String,
    val description: String = "",
    @SerialName("verification_text")
    val verificationText: String = "",
    val platform: String = "multiplatform",
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("tte_format")
    val tteFormat: String = "ttr",

    val intents: List<VfIntent> = emptyList(),

    @SerialName("screenshot_compare")
    val screenshotCompare: ScreenshotCompareConfig = ScreenshotCompareConfig(),
    @SerialName("network_tape")
    val networkTape: NetworkTapeConfig = NetworkTapeConfig(),

    val covers: List<String> = emptyList()
)

@Serializable
data class VfIntent(
    val store: String,
    @SerialName("intent_type")
    val intentType: String,
    val params: Map<String, String> = emptyMap(),
    @SerialName("delay_after_ms")
    val delayAfterMs: Long = 0,
    val note: String = ""
)

@Serializable
data class ScreenshotCompareConfig(
    @SerialName("ssim_threshold")
    val ssimThreshold: Double = 0.95,
    @SerialName("mask_regions")
    val maskRegions: List<String> = emptyList()
)

@Serializable
data class NetworkTapeConfig(
    @SerialName("match_strategy")
    val matchStrategy: String = "sequence",
    val note: String = ""
)
