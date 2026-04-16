plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kover)
    alias(libs.plugins.sqldelight)
}

kotlin {
    jvmToolchain(libs.versions.jdk.get().toInt())

    android {
        namespace = "com.example.archshowcase.platform"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    jvm("desktop")

    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "Platform"
            isStatic = true
        }
    }

    sourceSets {
        // Android/Desktop 共享 JVM 代码
        val jvmMain by creating { dependsOn(commonMain.get()) }

        commonMain.dependencies {
            api(project(":a-core"))

            implementation(libs.compose.runtime)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.encoding)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.mock)
        }

        androidMain {
            dependsOn(jvmMain)
            dependencies {
                implementation(libs.sqldelight.android)
                implementation(libs.ktor.client.okhttp)
            }
        }

        val iosMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.sqldelight.native)
                implementation(libs.ktor.client.darwin)
            }
        }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }

        val desktopMain by getting {
            dependsOn(jvmMain)
            dependencies {
                implementation(libs.sqldelight.jvm)
                implementation(libs.ktor.client.okhttp)
            }
        }
    }
}

sqldelight {
    databases {
        create("ChatDatabase") {
            packageName.set("com.example.archshowcase.chat.db")
        }
    }
}
