package com.example.archshowcase

import android.os.Build
import androidx.profileinstaller.ProfileVerifier
import com.example.archshowcase.core.perf.PerfMonitor

object ProfileStatusReporter {

    fun start() {
        if (Build.VERSION.SDK_INT < 28) return
        val future = ProfileVerifier.getCompilationStatusAsync()
        future.addListener({ report(future.get()) }, Runnable::run)
    }

    private fun report(status: ProfileVerifier.CompilationStatus) {
        val tag = when (status.profileInstallResultCode) {
            ProfileVerifier.CompilationStatus.RESULT_CODE_COMPILED_WITH_PROFILE -> "COMPILED"
            ProfileVerifier.CompilationStatus.RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION -> "ENQUEUED"
            ProfileVerifier.CompilationStatus.RESULT_CODE_COMPILED_WITH_PROFILE_NON_MATCHING -> "CLOUD"
            ProfileVerifier.CompilationStatus.RESULT_CODE_NO_PROFILE_INSTALLED -> "NONE"
            ProfileVerifier.CompilationStatus.RESULT_CODE_ERROR_NO_PROFILE_EMBEDDED -> "NO_EMBED"
            else -> "UNKNOWN(${status.profileInstallResultCode})"
        }
        PerfMonitor.reportProfileStatus(tag)
    }
}
