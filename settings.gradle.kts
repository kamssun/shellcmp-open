pluginManagement {
    includeBuild("build-plugin")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// JetBrains Runtime (JBR) 用于 Compose Hot Reload
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        // Private Maven repositories removed for open-source release.
        // To restore, add your Maven URL and credentials here:
        // maven {
        //     url = uri("https://your-maven-repo.example.com/repository/public/")
        // }
        // maven {
        //     url = uri("https://your-maven-repo.example.com/repository/release/")
        //     credentials {
        //         username = providers.gradleProperty("maven.username").orNull ?: ""
        //         password = providers.gradleProperty("maven.password").orNull ?: ""
        //     }
        // }
    }
}

rootProject.name = "ArchShowcase"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":a-core")
include(":a-platform")
include(":a-shared")
include(":androidApp")
include(":desktopApp")
include(":ksp-annotations")
include(":ksp-processor")
include(":macrobenchmark")
include(":tools:verify:screenshot-compare")