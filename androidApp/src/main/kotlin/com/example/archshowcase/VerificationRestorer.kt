package com.example.archshowcase

import android.content.Context
import android.util.Log

/**
 * 进程重启后恢复验证状态
 *
 * 通过反射调用 debug-only 的 VerificationSession.restoreIfNeeded()，
 * release 包中不存在该类，会静默跳过。
 */
object VerificationRestorer {

    fun restore(context: Context) {
        try {
            val session = Class.forName("com.example.archshowcase.verification.VerificationSession")
            val method = session.getMethod("restoreIfNeeded", Context::class.java)
            val instance = session.getField("INSTANCE").get(null)
            method.invoke(instance, context)
        } catch (_: ClassNotFoundException) {
            // release 包无 verification 类，忽略
        } catch (e: Exception) {
            Log.w("VerificationRestorer", "Restore failed: ${e.message}")
        }
    }
}
