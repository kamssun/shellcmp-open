package com.example.archshowcase.core.util

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import java.lang.ref.WeakReference

object ContextProvider : Application.ActivityLifecycleCallbacks {

    private lateinit var app: Application
    private var activityRef: WeakReference<ComponentActivity>? = null

    val applicationContext: Context get() = app

    val current: ComponentActivity? get() = activityRef?.get()

    fun install(application: Application) {
        app = application
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityResumed(activity: Activity) {
        (activity as? ComponentActivity)?.let { activityRef = WeakReference(it) }
    }

    override fun onActivityPaused(activity: Activity) {
        if (activityRef?.get() === activity) activityRef = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
