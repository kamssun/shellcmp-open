package com.example.archshowcase.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.archshowcase.presentation.demo.home.DemoHomeComponent
import com.example.archshowcase.presentation.demo.home.DemoHomeContent
import com.example.archshowcase.presentation.navigation.Route
import org.junit.Rule
import org.junit.Test

class HomeContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeContent_displaysTitle() {
        composeTestRule.setContent {
            DemoHomeContent(component = FakeDemoHomeComponent())
        }

        composeTestRule.onNodeWithText("Decompose + MVIKotlin Demo").assertIsDisplayed()
    }

    @Test
    fun homeContent_displaysAllButtons() {
        composeTestRule.setContent {
            DemoHomeContent(component = FakeDemoHomeComponent())
        }

        composeTestRule.onNodeWithText("1. 网络请求 (MVIKotlin Store)").assertIsDisplayed()
        composeTestRule.onNodeWithText("2. 图片加载 (Coil)").assertIsDisplayed()
        composeTestRule.onNodeWithText("3. 响应式布局 (Material3 Adaptive)").assertIsDisplayed()
        composeTestRule.onNodeWithText("4. 崩溃报告 (CrashKiOS)").assertIsDisplayed()
    }

    @Test
    fun homeContent_networkButtonClick_triggersCallback() {
        var navigatedRoute: Route? = null
        val component = FakeDemoHomeComponent(onNavigate = { navigatedRoute = it })

        composeTestRule.setContent {
            DemoHomeContent(component = component)
        }

        composeTestRule.onNodeWithText("1. 网络请求 (MVIKotlin Store)").performClick()
        assert(navigatedRoute == Route.NetworkDemo)
    }

    @Test
    fun homeContent_imageButtonClick_triggersCallback() {
        var navigatedRoute: Route? = null
        val component = FakeDemoHomeComponent(onNavigate = { navigatedRoute = it })

        composeTestRule.setContent {
            DemoHomeContent(component = component)
        }

        composeTestRule.onNodeWithText("2. 图片加载 (Coil)").performClick()
        assert(navigatedRoute == Route.ImageDemo)
    }

    private class FakeDemoHomeComponent(
        private val onNavigate: (Route) -> Unit = {}
    ) : DemoHomeComponent {
        override fun onNavigate(route: Route) = onNavigate.invoke(route)
    }
}
