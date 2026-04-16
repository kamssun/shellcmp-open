package com.example.archshowcase.verification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.archshowcase.core.AppRuntimeState
import com.example.archshowcase.core.trace.restore.RestoreRegistry
import com.example.archshowcase.core.trace.verification.NetworkTape
import com.example.archshowcase.core.trace.verification.TteStateExtractor
import com.example.archshowcase.network.VerificationTapeHolder
import kotlinx.serialization.json.Json
import com.example.archshowcase.verification.VerificationSession.TAG
import java.io.File

/**
 * Debug-only BroadcastReceiver 处理 ADB 验证命令
 *
 * 2 个 Action：
 * - VERIFY_INIT: 加载 VF 包 → 预填充状态 → recreate Activity
 * - VERIFY_DISPATCH: 按 index dispatch 单个 Intent
 */
class VerificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Received action: ${intent.action}")

        when (intent.action) {
            ACTION_INIT -> handleInit(context, intent)
            ACTION_DISPATCH -> handleDispatch(context, intent)
            else -> Log.w(TAG, "Unknown action: ${intent.action}")
        }
    }

    private fun handleInit(context: Context, intent: Intent) {
        // 验证模式下关闭 LeakCanary，避免 heap dump 干扰截图时序
        leakcanary.LeakCanary.config = leakcanary.LeakCanary.config.copy(dumpHeap = false)

        val vfPath = intent.getStringExtra("vf_path")
        if (vfPath.isNullOrBlank()) {
            Log.e(TAG, "VERIFY_INIT: missing vf_path extra")
            Log.i(TAG, "VERIFY_READY")
            return
        }

        val vfDir = File(vfPath)
        val vfPackage = VfFileReader.read(vfDir).getOrElse { e ->
            Log.e(TAG, "VERIFY_INIT: failed to read VF: ${e.message}")
            Log.i(TAG, "VERIFY_READY")
            return
        }

        // 提取 TTE-A 起始状态（空 TTE-A 表示从默认初始状态开始）
        val startStates = if (vfPackage.startTteBytes.isEmpty()) {
            Log.i(TAG, "VERIFY_INIT: empty start TTE, using default initial state")
            emptyMap()
        } else {
            TteStateExtractor.extract(vfPackage.startTteBytes).getOrElse { e ->
                Log.e(TAG, "VERIFY_INIT: failed to extract start TTE: ${e.message}")
                Log.i(TAG, "VERIFY_READY")
                return
            }
        }

        // 清空上次验证残留，确保 Store 从干净状态开始
        RestoreRegistry.clearAllSnapshots()
        VerificationTapeHolder.clear()
        AppRuntimeState.verificationMode = false

        // 预填充 RestoreRegistry（空则不填充，Store 将使用默认状态）
        startStates.forEach { (name, state) ->
            RestoreRegistry.updateSnapshotOrCreate(name, state)
        }

        // 设置验证模式（有预填充状态时才启用，否则让 Store 用默认初始值）
        AppRuntimeState.verificationMode = startStates.isNotEmpty()

        // 初始化会话 + 持久化路径（进程重启后可恢复）
        VerificationSession.init(vfPackage, startStates)
        VerificationSession.persistVfPath(context, vfPath)

        // 加载 NetworkTape（如有）并重建 HttpClient（用 MockEngine 回放）
        val tapeBytes = vfPackage.extraFiles["network_tape.json"]
        if (tapeBytes != null) {
            try {
                val tapeJson = Json { ignoreUnknownKeys = true }
                val tape = tapeJson.decodeFromString(
                    NetworkTape.serializer(),
                    tapeBytes.decodeToString()
                )
                VerificationTapeHolder.load(tape)
                Log.i(TAG, "NetworkTape loaded: ${tape.recordings.size} recordings")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load NetworkTape: ${e.message}")
            }
        }

        // recreate/launch Activity 并等待 onResume
        val app = context.applicationContext as android.app.Application
        ReceiverHelper.waitForActivityResume(app) {
            Log.i(TAG, "VERIFY_READY")
        }
        ReceiverHelper.recreateOrLaunch(context)
    }

    private fun handleDispatch(context: Context, intent: Intent) {
        val index = intent.getIntExtra("index", -1)
        val manifest = VerificationSession.manifest
        if (manifest == null) {
            Log.e(TAG, "VERIFY_DISPATCH: no active session")
            Log.i(TAG, "VERIFY_READY")
            return
        }
        if (index < 0 || index >= manifest.intents.size) {
            Log.e(TAG, "VERIFY_DISPATCH: index $index out of range [0, ${manifest.intents.size})")
            Log.i(TAG, "VERIFY_READY")
            return
        }

        val vfIntent = manifest.intents[index]
        ReceiverHelper.mainHandler.post {
            ReceiverHelper.dispatchVfIntent(vfIntent)
            ReceiverHelper.mainHandler.post {
                Log.i(TAG, "VERIFY_READY")
            }
        }
    }

    companion object {
        const val ACTION_INIT = "com.example.archshowcase.VERIFY_INIT"
        const val ACTION_DISPATCH = "com.example.archshowcase.VERIFY_DISPATCH"
    }
}
