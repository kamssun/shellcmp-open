package com.example.archshowcase.core.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dev.icerock.moko.permissions.PermissionsController
import kotlinx.coroutines.launch

object PermissionHelper : Application.ActivityLifecycleCallbacks {

    private var controller: PermissionsController? = null

    fun install(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity !is ComponentActivity) return
        if (controller != null) return

        val ctrl = PermissionsController(activity.applicationContext)
        controller = ctrl
        ctrl.bind(activity)

        activity.lifecycleScope.launch {
            ctrl.requestStorageIfNeeded()
        }
    }

    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
