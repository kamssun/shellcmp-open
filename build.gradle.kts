plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidTest) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.kover)
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension>("kotlin") {
            compilerOptions {
                // KMP platform bridges intentionally use expect/actual classes; Kotlin 2.3 requires explicit opt-in to silence the Beta warning.
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }
}

apply(from = "gradle/kover.gradle")
apply(from = "gradle/verification.gradle")

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
