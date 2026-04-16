import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.example.archshowcase.MainView
import com.example.archshowcase.presentation.root.DefaultRootComponent
import com.example.archshowcase.core.di.desktopPlatformModule
import com.example.archshowcase.di.getAppModules
import org.koin.core.context.startKoin

fun main() {
    // 初始化 Koin
    startKoin {
        modules(getAppModules(desktopPlatformModule))
    }

    application {
        val lifecycle = LifecycleRegistry()
        val root = DefaultRootComponent(DefaultComponentContext(lifecycle = lifecycle))

        Window(
            onCloseRequest = ::exitApplication,
            title = "ArchShowcase",
            state = rememberWindowState(
                width = 400.dp,
                height = 800.dp,
                position = WindowPosition.Aligned(Alignment.CenterEnd)
            )
        ) {
            MainView(root)
        }
    }
}
