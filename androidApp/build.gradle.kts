import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.baselineprofile)
    id("com.example.archshowcase.obo-handler")
}

roborazzi {
    outputDir = file("src/test/snapshots")
    @OptIn(ExperimentalRoborazziApi::class)
    generateComposePreviewRobolectricTests {
        enable = true
        packages = listOf("com.example.archshowcase")
        includePrivatePreviews = false
        testerQualifiedClassName = "com.example.archshowcase.test.StableComposePreviewTester"
        useScanOptionParametersInTester = true
        robolectricConfig = mapOf(
            "sdk" to "[34]",
            "qualifiers" to "\"w411dp-h891dp-xxhdpi\"",
        )
    }
}

android {
    namespace = "com.example.archshowcase"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.archshowcase"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val config = Properties()
        config.load(rootProject.file("config.properties").inputStream())
        for ((key, value) in config) {
            val k = key.toString()
            buildConfigField("String", k.replace('.', '_').uppercase(), "\"$value\"")
        }

        ndk { abiFilters += "arm64-v8a" }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            pickFirsts += "lib/**/libc++_shared.so"
        }
    }
    signingConfigs {
        getByName("debug") {
            //TODO  先用默认 debug keystore
        }
    }
    buildTypes {
        getByName("debug") {
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug") //TODO 开发测试先用 debug 签名
        }
        create("benchmark") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.jvmArgs("-Xmx4g")
                it.systemProperties["shellcmp.preview.renderImages"] =
                    providers.gradleProperty("shellcmp.preview.renderImages").orElse("false").get()
                it.systemProperties["robolectric.pixelCopyRenderMode"] = "hardware"
            }
        }
    }
}

dependencies {
    implementation(project(":a-shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.profileinstaller)
    implementation(libs.emoji2)
    debugImplementation(libs.compose.uiTooling)
    debugImplementation(libs.leakcanary)
    baselineProfile(project(":macrobenchmark"))

    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.composable.preview.scanner.android)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.rule)
    testImplementation(libs.roborazzi.preview.scanner)
    testImplementation(libs.robolectric)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.compose.ui.test.manifest)

    val enableBtrace = providers.gradleProperty("enable.btrace").orNull?.toBoolean() == true
    if (enableBtrace) {
        implementation(libs.btrace.rhea)
    } else {
        implementation(libs.btrace.rhea.noop)
    }
}
