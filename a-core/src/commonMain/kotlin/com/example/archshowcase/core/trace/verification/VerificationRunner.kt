package com.example.archshowcase.core.trace.verification

import com.example.archshowcase.core.trace.restore.RestorableState

/**
 * Store 级别验证运行器（Phase 1）
 *
 * 编排完整验证流程：解析 VF → 提取状态 → 创建 Store → dispatch → 截图对比。
 *
 * 使用 [VerificationContext] 抽象 Store 创建和 dispatch 的具体实现，
 * 使 Runner 不依赖具体的 Store 类型。
 */
class VerificationRunner(
    private val context: VerificationContext
) {

    /**
     * 执行完整验证流程
     */
    suspend fun run(vf: VfPackage): VerificationReport {
        val scenarioName = vf.manifest.name

        // 1. 提取 TTE-A 状态（起始状态）
        val startStates = TteStateExtractor.extract(vf.startTteBytes).getOrElse { e ->
            return VerificationReport.error(scenarioName, "Failed to extract start TTE: ${e.message}")
        }

        // 2. 提取 TTE-B 状态（期望的最终状态）
        TteStateExtractor.extract(vf.endTteBytes).getOrElse { e ->
            return VerificationReport.error(scenarioName, "Failed to extract end TTE: ${e.message}")
        }

        // 3. 用起始状态创建 Store 并 dispatch Intent 序列
        context.createAndDispatch(
            startStates = startStates,
            intents = vf.manifest.intents
        ).getOrElse { e ->
            return VerificationReport.error(scenarioName, "Failed to dispatch intents: ${e.message}")
        }

        // 4. SSIM 截图对比由外部脚本执行，Runner 仅返回空报告
        return VerificationReport(
            scenarioName = scenarioName,
            levelResults = emptyList()
        )
    }
}

/**
 * 验证上下文
 *
 * 抽象 Store 创建和 dispatch 的具体实现。
 * 由各场景（测试或真机）提供具体实现。
 */
interface VerificationContext {
    /**
     * 用起始状态创建 Store，dispatch Intent 序列，返回最终状态
     */
    suspend fun createAndDispatch(
        startStates: Map<String, RestorableState>,
        intents: List<VfIntent>
    ): Result<Map<String, RestorableState>>
}
