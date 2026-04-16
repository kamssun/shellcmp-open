plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kover)
}

kotlin {
    jvmToolchain(libs.versions.jdk.get().toInt())

    android {
        namespace = "com.example.archshowcase.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    jvm("desktop")

    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "Core"
            isStatic = true
        }
    }

    sourceSets {
        // Android/Desktop 共享 JVM 代码
        val jvmMain by creating { dependsOn(commonMain.get()) }

        commonMain.dependencies {
            // api: 传递给 shared 及上层模块
            api(compose.runtime)
            api(compose.ui)
            api(libs.koin.core)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.json)
            api(libs.kotlinx.datetime)
            api(libs.decompose)
            api(libs.decompose.compose)
            api(libs.mvikotlin)
            api(libs.mvikotlin.main)
            api(libs.mvikotlin.logging)
            api(libs.mvikotlin.coroutines)
            api(libs.mvikotlin.timetravel)
            api(libs.essenty.lifecycle)
            api(libs.essenty.lifecycle.coroutines)
            api(libs.coil.compose)
            api(libs.coil.network.ktor)
            api(libs.datastore.preferences)

            // implementation: 仅 core 内部使用
            implementation(libs.kermit)
        }

        androidMain {
            dependsOn(jvmMain)
            dependencies {
                api(libs.koin.android)
                api(libs.kotlinx.coroutines.android)
                api(libs.coil.gif)
                implementation(libs.androidx.lifecycle.process)
                implementation(libs.moko.permissions)
                implementation(libs.moko.permissions.compose)
                implementation(libs.moko.permissions.storage)
            }
        }

        val iosMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.moko.permissions)
                implementation(libs.moko.permissions.compose)
                implementation(libs.moko.permissions.storage)
            }
        }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }

        val desktopMain by getting {
            dependsOn(jvmMain)
            dependencies {
                api(libs.kotlinx.coroutines.swing)
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}
