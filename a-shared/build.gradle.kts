plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kover)
}

composeCompiler {
    stabilityConfigurationFiles.add(project.layout.projectDirectory.file("compose_stability_config.conf"))
    reportsDestination.set(project.layout.buildDirectory.dir("compose-reports"))
    metricsDestination.set(project.layout.buildDirectory.dir("compose-metrics"))
}

compose.resources {
    packageOfResClass = "com.example.archshowcase.resources"
}

kotlin {
    jvmToolchain(libs.versions.jdk.get().toInt())

    android {
        namespace = "com.example.archshowcase.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        withHostTest { }
        androidResources { enable = true }
    }

    jvm("desktop")

    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(project(":a-platform"))
            export(project(":a-core"))
        }
    }

    sourceSets {
        // Android/Desktop 共享 JVM 代码
        val jvmMain by creating { dependsOn(commonMain.get()) }

        // SDK 依赖由 :a-platform api 传递，此处仅声明 a-shared 自用的 Compose/UI 依赖
        commonMain.dependencies {
            api(project(":a-platform"))
            implementation(project(":ksp-annotations"))

            implementation(libs.compose.foundation)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.koin.compose)
            implementation(libs.filekit.dialogs.compose)
        }

        androidMain {
            dependsOn(jvmMain)
        }

        val iosMain by creating {
            dependsOn(commonMain.get())
        }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }

        val desktopMain by getting {
            dependsOn(jvmMain)
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.koin.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
    add("kspCommonMainMetadata", project(":ksp-processor"))
}

// KSP 生成代码注入 commonMain，所有编译任务依赖 KSP
afterEvaluate {
    kotlin.sourceSets.named("commonMain") {
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}
