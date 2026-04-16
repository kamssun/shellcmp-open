plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(libs.versions.jdk.get().toInt())
}

dependencies {
    implementation(project(":ksp-annotations"))
    implementation(libs.ksp.api)

    implementation(kotlin("stdlib"))

    testImplementation(kotlin("test"))
}
