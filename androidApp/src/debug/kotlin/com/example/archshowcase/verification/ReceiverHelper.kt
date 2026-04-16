package com.example.archshowcase.verification

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.arkivanov.mvikotlin.core.store.Store
import com.example.archshowcase.core.trace.restore.RestoreRegistry
import com.example.archshowcase.core.trace.verification.GeneratedIntentResolverRegistry
import com.example.archshowcase.core.trace.verification.VfIntent
import com.example.archshowcase.core.util.ContextProvider

/**
 * Verification / Recording 共享工具
 *
 * 抽取 VerificationReceiver 和 RecordReceiver 的重复逻辑。
 */
object ReceiverHelper {

    val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 等待 Activity resume 后执行回调
     */
    fun waitForActivityResume(app: Application, onResumed: (Activity) -> Unit) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(a: Activity) {
                app.unregisterActivityLifecycleCallbacks(this)
                mainHandler.post { onResumed(a) }
            }
            override fun onActivityCreated(a: Activity, s: Bundle?) = Unit
            override fun onActivityStarted(a: Activity) = Unit
            override fun onActivityPaused(a: Activity) = Unit
            override fun onActivityStopped(a: Activity) = Unit
            override fun onActivitySaveInstanceState(a: Activity, s: Bundle) = Unit
            override fun onActivityDestroyed(a: Activity) = Unit
        })
    }

    /**
     * recreate 或启动 Activity
     */
    fun recreateOrLaunch(context: android.content.Context) {
        val activity = ContextProvider.current
        if (activity != null) {
            mainHandler.post { activity.recreate() }
        } else {
            Log.w(VerificationSession.TAG, "No foreground activity, launching MainActivity")
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            launchIntent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            launchIntent?.let { context.startActivity(it) }
        }
    }

    /**
     * 解析并 dispatch VfIntent 到对应 Store
     *
     * @return true 表示成功 dispatch
     */
    fun dispatchVfIntent(vfIntent: VfIntent): Boolean {
        val store = RestoreRegistry.findStore(vfIntent.store)
        if (store == null) {
            Log.e(VerificationSession.TAG, "Store '${vfIntent.store}' not found in RestoreRegistry")
            return false
        }

        val intent = GeneratedIntentResolverRegistry.resolve(vfIntent)
        if (intent != null) {
            @Suppress("UNCHECKED_CAST")
            (store as Store<Any, *, *>).accept(intent)
            return true
        } else {
            Log.e(VerificationSession.TAG, "Unresolved intent ${vfIntent.store}/${vfIntent.intentType}")
            return false
        }
    }
}
