package com.example.archshowcase.verification

import android.content.Context
import android.util.Log
import com.example.archshowcase.core.AppRuntimeState
import com.example.archshowcase.core.trace.restore.RestorableState
import com.example.archshowcase.core.trace.verification.NetworkTape
import com.example.archshowcase.core.trace.verification.TteStateExtractor
import com.example.archshowcase.core.trace.restore.RestoreRegistry
import com.example.archshowcase.core.trace.verification.VfManifest
import com.example.archshowcase.core.trace.verification.VfPackage
import com.example.archshowcase.network.VerificationTapeHolder
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * 验证会话单例
 *
 * 持有当前 VF manifest，在 broadcast action 之间共享数据。
 * 支持进程重启后恢复：通过 SharedPreferences 持久化 VF 路径。
 */
object VerificationSession {

    private const val PREFS_NAME = "verification"
    private const val KEY_VF_PATH = "vf_path"

    private val currentManifest = AtomicReference<VfManifest?>(null)

    val manifest: VfManifest? get() = currentManifest.get()

    fun init(vfPackage: VfPackage, states: Map<String, RestorableState>) {
        currentManifest.set(vfPackage.manifest)
        Log.i(TAG, "Session initialized: ${vfPackage.manifest.name}")
    }

    /** INIT 时调用：持久化 VF 路径，进程重启后可恢复 */
    fun persistVfPath(context: Context, vfPath: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_VF_PATH, vfPath).apply()
    }

    /**
     * App 启动时调用：如果有持久化的 VF 路径，恢复验证状态
     * @return true 表示成功恢复验证模式
     */
    fun restoreIfNeeded(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val vfPath = prefs.getString(KEY_VF_PATH, null) ?: return false

        // 立即清除，确保只恢复一次（仅服务于 VERIFY_INIT 的 recreate 进程重启）
        clearPersistedVfPath(context)

        Log.i(TAG, "Restoring verification from persisted path: $vfPath")

        val vfPackage = VfFileReader.read(File(vfPath)).getOrElse { e ->
            Log.e(TAG, "Failed to restore VF: ${e.message}")
            clearPersistedVfPath(context)
            return false
        }

        val states = if (vfPackage.startTteBytes.isEmpty()) {
            emptyMap()
        } else {
            TteStateExtractor.extract(vfPackage.startTteBytes).getOrElse { emptyMap() }
        }

        RestoreRegistry.clearAllSnapshots()
        states.forEach { (name, state) ->
            RestoreRegistry.updateSnapshotOrCreate(name, state)
        }
        AppRuntimeState.verificationMode = states.isNotEmpty()

        init(vfPackage, states)

        val tapeBytes = vfPackage.extraFiles["network_tape.json"]
        if (tapeBytes != null) {
            try {
                val tapeJson = Json { ignoreUnknownKeys = true }
                val tape = tapeJson.decodeFromString(
                    NetworkTape.serializer(),
                    tapeBytes.decodeToString()
                )
                VerificationTapeHolder.load(tape)
                AppRuntimeState.verificationMode = true
                Log.i(TAG, "NetworkTape restored: ${tape.recordings.size} recordings")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to restore NetworkTape: ${e.message}")
            }
        }

        return true
    }

    private fun clearPersistedVfPath(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_VF_PATH).apply()
    }

    const val TAG = "ShellVerify"
}
