package com.example.archshowcase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.defaultComponentContext
import com.example.archshowcase.core.AppRuntimeState
import com.example.archshowcase.core.perf.startup.StartupLifecycleObserver
import com.example.archshowcase.core.perf.startup.StartupTracer
import com.example.archshowcase.presentation.root.DefaultRootComponent

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        StartupTracer.markPhaseEnd("system_activity_launch")
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        VerificationRestorer.restore(this)
        DevBookmarkRestorer.restore()

        StartupLifecycleObserver.install(this) {
            val root = StartupTracer.traced("root_component") {
                DefaultRootComponent(
                    defaultComponentContext(discardSavedState = AppRuntimeState.verificationMode)
                )
            }
            StartupTracer.traced("compose_setup") {
                setContent { MainView(root) }
            }
        }
    }
}
