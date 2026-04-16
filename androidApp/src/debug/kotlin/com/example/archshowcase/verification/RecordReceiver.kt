package com.example.archshowcase.verification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.os.Environment
import com.example.archshowcase.core.util.ScreenshotCapture
import com.example.archshowcase.presentation.timetravel.TimeTravelComponentHolder
import java.io.File

/**
 * Debug-only BroadcastReceiver：通过 adb broadcast 触发 VF 录制。
 *
 * 用于回溯模式下（浮窗录制按钮不可见时），通过外部命令控制录制：
 * ```
 * adb shell am broadcast -a com.example.archshowcase.VF_RECORD_START \
 *   -n com.example.archshowcase/com.example.archshowcase.verification.RecordReceiver
 * adb shell am broadcast -a com.example.archshowcase.VF_RECORD_END \
 *   -n com.example.archshowcase/com.example.archshowcase.verification.RecordReceiver
 * ```
 */
class RecordReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Received action: ${intent.action}")

        when (intent.action) {
            ACTION_START -> handleStart()
            ACTION_END -> handleEnd(context, intent)
            else -> Log.w(TAG, "Unknown action: ${intent.action}")
        }
    }

    private fun handleStart() {
        if (TimeTravelComponentHolder.instance == null) {
            Log.e(TAG, "VF_RECORD_START: TimeTravelComponent not available")
            return
        }

        ReceiverHelper.mainHandler.postDelayed({
            val component = TimeTravelComponentHolder.instance
            if (component == null) {
                Log.e(TAG, "VF_RECORD_START: component destroyed during settle delay")
                return@postDelayed
            }
            val screenshot = ScreenshotCapture.capture()
            component.onExportVfStart(screenshot)
            Log.i(TAG, "VF_RECORD_READY")
        }, SETTLE_DELAY_MS)
    }

    private fun handleEnd(context: Context, intent: Intent) {
        if (TimeTravelComponentHolder.instance == null) {
            Log.e(TAG, "VF_RECORD_END: TimeTravelComponent not available")
            return
        }

        val verificationText = intent.getStringExtra("verification_text") ?: ""

        ReceiverHelper.mainHandler.postDelayed({
            val component = TimeTravelComponentHolder.instance
            if (component == null) {
                Log.e(TAG, "VF_RECORD_END: component destroyed during settle delay")
                return@postDelayed
            }
            val screenshot = ScreenshotCapture.capture()
            val vfFiles = component.onExportVfEnd(verificationText, screenshot)

            if (vfFiles == null) {
                Log.e(TAG, "VF_RECORD_END: export returned null (not in recording state?)")
                return@postDelayed
            }

            val timestamp = System.currentTimeMillis()
            val vfDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "vf/vf_$timestamp"
            )
            vfDir.mkdirs()

            vfFiles.forEach { (name, bytes) ->
                File(vfDir, name).writeBytes(bytes)
            }

            Log.i(TAG, "VF_RECORD_DONE path=${vfDir.absolutePath}")
        }, SETTLE_DELAY_MS)
    }

    companion object {
        private const val TAG = "RecordReceiver"
        private const val SETTLE_DELAY_MS = 300L
        const val ACTION_START = "com.example.archshowcase.VF_RECORD_START"
        const val ACTION_END = "com.example.archshowcase.VF_RECORD_END"
    }
}
