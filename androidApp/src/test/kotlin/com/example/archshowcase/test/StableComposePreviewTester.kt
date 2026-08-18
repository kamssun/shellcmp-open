package com.example.archshowcase.test

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.github.takahirom.roborazzi.AndroidComposePreviewTester
import com.github.takahirom.roborazzi.ComposePreviewTester
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.InternalRoborazziApi
import com.github.takahirom.roborazzi.RoboComposePreviewOptionVariation
import com.github.takahirom.roborazzi.RoborazziActivity
import com.github.takahirom.roborazzi.RoborazziRecordFilePathStrategy
import com.github.takahirom.roborazzi.annotations.RoboComposePreviewOptions
import com.github.takahirom.roborazzi.composeTestRule
import com.github.takahirom.roborazzi.manualAdvance
import com.github.takahirom.roborazzi.provideRoborazziContext
import com.github.takahirom.roborazzi.roborazziDefaultNamingStrategy
import com.github.takahirom.roborazzi.roborazziRecordFilePathStrategy
import com.github.takahirom.roborazzi.roborazziSystemPropertyOutputDirectory
import com.github.takahirom.roborazzi.toRoborazziComposeOptions
import com.example.archshowcase.core.AppRuntimeState
import sergio.sastre.composable.preview.scanner.android.AndroidComposablePreviewScanner
import sergio.sastre.composable.preview.scanner.android.screenshotid.AndroidPreviewScreenshotIdBuilder
import java.io.File

@OptIn(ExperimentalRoborazziApi::class, InternalRoborazziApi::class)
class StableComposePreviewTester :
    ComposePreviewTester<ComposePreviewTester.TestParameter.JUnit4TestParameter.AndroidPreviewJUnit4TestParameter> {

    override fun options(): ComposePreviewTester.Options =
        ComposePreviewTester.defaultOptionsFromPlugin.copy(
            testLifecycleOptions = ComposePreviewTester.Options.JUnit4TestLifecycleOptions(
                composeRuleFactory = {
                    @Suppress("UNCHECKED_CAST", "DEPRECATION") // Keep immediate effect execution for preview parity with Android Studio.
                    createAndroidComposeRule<RoborazziActivity>() as AndroidComposeTestRule<ActivityScenarioRule<out ComponentActivity>, *>
                }
            )
        )

    override fun testParameters(): List<ComposePreviewTester.TestParameter.JUnit4TestParameter.AndroidPreviewJUnit4TestParameter> {
        val options = options()
        val lifecycleOptions = options.testLifecycleOptions as ComposePreviewTester.Options.JUnit4TestLifecycleOptions
        return AndroidComposablePreviewScanner()
            .scanPackageTrees(*options.scanOptions.packages.toTypedArray())
            .includeAnnotationInfoForAllOf(RoboComposePreviewOptions::class.java)
            .let { scanner ->
                if (options.scanOptions.includePrivatePreviews) scanner.includePrivatePreviews() else scanner
            }
            .getPreviews()
            .flatMap { preview ->
                val annotationOptions = Class.forName(preview.declaringClass).declaredMethods
                    .firstOrNull { it.name == preview.methodName }
                    ?.getAnnotation(RoboComposePreviewOptions::class.java)
                    ?: RoboComposePreviewOptions()
                annotationOptions.manualClockOptions
                    .map { RoboComposePreviewOptionVariation(it) }
                    .ifEmpty { listOf(RoboComposePreviewOptionVariation()) }
                    .map { optionVariation ->
                        ComposePreviewTester.TestParameter.JUnit4TestParameter.AndroidPreviewJUnit4TestParameter(
                            composeTestRuleFactory = { lifecycleOptions.composeRuleFactory() },
                            preview = preview,
                            composeRoboComposePreviewOptionVariation = optionVariation,
                        )
                    }
            }
    }

    override fun test(testParameter: ComposePreviewTester.TestParameter.JUnit4TestParameter.AndroidPreviewJUnit4TestParameter) {
        AppRuntimeState.previewRenderImages = System.getProperty(PROP_RENDER_IMAGES).toBoolean()

        val preview = testParameter.preview
        val pathPrefix =
            if (roborazziRecordFilePathStrategy() == RoborazziRecordFilePathStrategy.RelativePathFromCurrentDirectory) {
                roborazziSystemPropertyOutputDirectory() + File.separator
            } else {
                ""
            }
        val name = roborazziDefaultNamingStrategy().generateOutputName(
            preview.declaringClass,
            AndroidPreviewScreenshotIdBuilder(preview).ignoreClassName().build()
        )
        val optionVariation = testParameter.composeRoboComposePreviewOptionVariation
        val filePath = "$pathPrefix$name${optionVariation.nameWithPrefix()}.${provideRoborazziContext().imageExtension}"
        val composeOptions = preview.toRoborazziComposeOptions().builder()
            .apply {
                composeTestRule(testParameter.composeTestRule)
                optionVariation.manualClockOptions?.let {
                    manualAdvance(testParameter.composeTestRule, it.advanceTimeMillis)
                }
            }
            .build()

        AndroidComposePreviewTester.DefaultCapturer().capture(
            AndroidComposePreviewTester.CaptureParameter(
                preview = preview,
                filePath = filePath,
                roborazziComposeOptions = composeOptions,
                roborazziOptions = provideRoborazziContext().options,
            )
        )
    }

    private companion object {
        const val PROP_RENDER_IMAGES = "shellcmp.preview.renderImages"
    }
}
