package com.example.archshowcase.macrobenchmark

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

private const val TARGET_PACKAGE = "com.example.archshowcase"
private const val ARG_EMAIL = "benchmarkEmail"
private const val ARG_CODE = "benchmarkVerificationCode"
private const val ARG_TIMEOUT_SECONDS = "benchmarkLoginTimeoutSeconds"
private const val DEFAULT_LOGIN_TIMEOUT_MS = 300_000L
private const val SHORT_WAIT_MS = 5_000L

private const val EMAIL_LOGIN_BUTTON = "login_email_button"
private const val EMAIL_FIELD = "login_email_field"
private const val SEND_CODE_BUTTON = "login_send_code_button"
private const val CODE_FIELD = "login_code_field"
private const val VERIFY_LOGIN_BUTTON = "login_verify_button"
private const val MAIN_CONTENT = "main_content"
private val MAIN_TAB_TEXTS = arrayOf("首页", "发现", "聊天", "我")

internal object RealLoginPreparer {
    private const val TAG = "RealLoginPreparer"

    fun requireMainScreen(timeoutMs: Long = SHORT_WAIT_MS) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        waitForMainScreen(device, timeoutMs)
    }

    fun prepare(scope: MacrobenchmarkScope? = null) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        val email = args.getString(ARG_EMAIL).orEmpty()
        val code = args.getString(ARG_CODE).orEmpty()
        val manualLogin = email.isBlank()
        val timeoutMs = args.getString(ARG_TIMEOUT_SECONDS)
            ?.toLongOrNull()
            ?.coerceAtLeast(30)
            ?.times(1_000)
            ?: DEFAULT_LOGIN_TIMEOUT_MS

        val device = UiDevice.getInstance(instrumentation)
        if (scope == null) {
            device.pressHome()
            launchTargetApp(instrumentation.context, device)
        } else {
            scope.pressHome()
            scope.startActivityAndWait()
        }

        if (device.wait(Until.hasObject(selector(MAIN_CONTENT)), SHORT_WAIT_MS)) {
            return
        }

        if (manualLogin) {
            Log.i(TAG, "Complete real login on the device within ${timeoutMs / 1000}s; benchmark will continue after main screen appears.")
            waitForMainScreen(device, timeoutMs)
            return
        }

        click(device, EMAIL_LOGIN_BUTTON, timeoutMs)
        enterText(device, EMAIL_FIELD, email, timeoutMs)
        click(device, SEND_CODE_BUTTON, timeoutMs)

        if (code.isNotBlank()) {
            enterText(device, CODE_FIELD, code, timeoutMs)
            click(device, VERIFY_LOGIN_BUTTON, timeoutMs)
        } else {
            Log.i(TAG, "Verification code was sent. Complete login on the device within ${timeoutMs / 1000}s.")
        }

        waitForMainScreen(device, timeoutMs)
    }

    private fun selector(resourceId: String) = By.res(TARGET_PACKAGE, resourceId)

    private fun click(device: UiDevice, resourceId: String, timeoutMs: Long) {
        device.waitForObject(resourceId, timeoutMs).click()
    }

    private fun enterText(device: UiDevice, resourceId: String, value: String, timeoutMs: Long) {
        val field = device.waitForObject(resourceId, timeoutMs)
        field.click()
        field.text = value
    }

    private fun launchTargetApp(context: Context, device: UiDevice) {
        val intent = checkNotNull(context.packageManager.getLaunchIntentForPackage(TARGET_PACKAGE)) {
            "Could not resolve launch intent for $TARGET_PACKAGE"
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        check(device.wait(Until.hasObject(By.pkg(TARGET_PACKAGE)), SHORT_WAIT_MS)) {
            "Could not launch $TARGET_PACKAGE"
        }
    }

    private fun waitForMainScreen(device: UiDevice, timeoutMs: Long) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            if (device.hasMainScreenMarker()) return
            SystemClock.sleep(500)
        }
        error("Real login did not reach the main screen within ${timeoutMs / 1000}s.")
    }

    private fun UiDevice.hasMainScreenMarker(): Boolean {
        if (wait(Until.hasObject(selector(MAIN_CONTENT)), 100)) return true
        return MAIN_TAB_TEXTS.any { text ->
            wait(Until.hasObject(By.text(text)), 100)
        }
    }

    private fun UiDevice.waitForObject(resourceId: String, timeoutMs: Long): UiObject2 =
        checkNotNull(wait(Until.findObject(selector(resourceId)), timeoutMs)) {
            "Could not find UI object: $resourceId"
        }
}
