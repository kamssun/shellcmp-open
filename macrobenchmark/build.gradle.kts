plugins {
    alias(libs.plugins.androidTest)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.example.archshowcase.macrobenchmark"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["benchmarkEmail"] =
            providers.gradleProperty("benchmark.email").getOrElse("")
        testInstrumentationRunnerArguments["benchmarkVerificationCode"] =
            providers.gradleProperty("benchmark.verificationCode").getOrElse("")
        testInstrumentationRunnerArguments["benchmarkLoginTimeoutSeconds"] =
            providers.gradleProperty("benchmark.loginTimeoutSeconds").getOrElse("300")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    targetProjectPath = ":androidApp"

    testOptions.managedDevices.allDevices {
        create("pixel6Api31", com.android.build.api.dsl.ManagedVirtualDevice::class.java) {
            device = "Pixel 6"
            apiLevel = 31
            systemImageSource = "aosp"
        }
    }
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.benchmark.macro.junit4)
    implementation(libs.uiautomator)
    implementation(libs.androidx.test.monitor)
}
