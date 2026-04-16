package com.example.archshowcase

import android.provider.Settings
import androidx.compose.runtime.Composer
import androidx.compose.runtime.tooling.ComposeStackTraceMode
import com.bytedance.rheatrace.RheaTrace3
import com.example.archshowcase.core.di.createAndroidPlatformModule
import com.example.archshowcase.core.initAndroidApp
import com.example.archshowcase.core.perf.startup.ApplicationStartupHelper
import com.example.archshowcase.core.perf.startup.StartupTracer
import com.example.archshowcase.auth.AuthInitializer
import com.example.archshowcase.auth.AuthInitializerConfig
import com.example.archshowcase.devicetoken.DeviceTokenInitializer
import com.example.archshowcase.devicetoken.DeviceTokenInitializerConfig
import com.example.archshowcase.di.getAppModules
import com.example.archshowcase.im.ImInitializer
import com.example.archshowcase.im.ImInitializerConfig
import com.example.archshowcase.network.header.HeaderConstants
import com.example.archshowcase.network.header.HeaderProvider
import com.example.archshowcase.core.scheduler.OBOScheduler
import androidx.emoji2.text.DefaultEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat


class Application : android.app.Application() {

    companion object {
        private const val OBO_TAG = "Application"
    }

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(base)
        RheaTrace3.init(base)
        ApplicationStartupHelper.onAttachBaseContextEnd()
    }

    override fun onCreate() {
        super.onCreate()

        ApplicationStartupHelper.beginStartupTrace()

        StartupTracer.traced("app_create") {
            val isDebug = BuildConfig.DEBUG
            Composer.setDiagnosticStackTraceMode(
                if (isDebug) ComposeStackTraceMode.SourceInformation
                else ComposeStackTraceMode.Auto
            )

            StartupTracer.traced("di_init") {
                initAndroidApp(
                    context = this,
                    modules = getAppModules(createAndroidPlatformModule(this)),
                    isDebug = isDebug
                )
            }

            val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                .orEmpty()
                .ifBlank { packageName }

            StartupTracer.traced("sdk_init") {
                StartupTracer.traced("sdk_auth") {
                    AuthInitializer.initialize(
                        context = this,
                        config = AuthInitializerConfig(
                            serverUrl = BuildConfig.AUTH_SERVER_URL,
                            appKey = BuildConfig.APP_KEY,
                            deviceId = deviceId,
                            googleServerClientId = BuildConfig.GOOGLE_SERVER_CLIENT_ID,
                            enableLogging = isDebug
                        )
                    )
                }

                HeaderProvider.configure(
                    apiKey = BuildConfig.APP_KEY,
                    codeTag = HeaderConstants.buildCodeTag("android", BuildConfig.VERSION_NAME),
                    deviceId = deviceId
                )

                StartupTracer.traced("sdk_device_token") {
                    DeviceTokenInitializer.initialize(
                        config = DeviceTokenInitializerConfig(
                            aliKey = BuildConfig.RISKY_ALI_KEY,
                            signKey = BuildConfig.RISKY_SIGN_KEY,
                            signKeyNew = BuildConfig.RISKY_SIGN_KEY_NEW,
                            aliyunOptions = mapOf(
                                "Intl" to "1",
                                "CustomUrl" to "https://your-risk-sdk-endpoint.example.com",
                                "CustomHost" to "your-risk-sdk-endpoint.example.com",
                            )
                        )
                    )
                }

                StartupTracer.traced("sdk_im") {
                    ImInitializer.preInit(
                        config = ImInitializerConfig(
                            isDebug = isDebug,
                            apiKey = BuildConfig.IM_API_KEY,
                            codeTag = BuildConfig.IM_CODE_TAG,
                            xlogKey = BuildConfig.IM_XLOG_KEY
                        )
                    )
                }
            }
        }

        ProfileStatusReporter.start()

        // EmojiCompat 延迟初始化：已在 Manifest 屏蔽自动初始化，通过 OBO 让出主线程后加载
        OBOScheduler.post(OBO_TAG) {
            DefaultEmojiCompatConfig.create(this)?.let { config ->
                EmojiCompat.init(config)
            }
        }

        StartupTracer.markPhaseStart("system_activity_launch")
    }
}
