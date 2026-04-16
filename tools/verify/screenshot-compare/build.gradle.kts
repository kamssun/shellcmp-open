plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    kotlin("jvm")
    alias(libs.plugins.kotlinSerialization)
    application
}

application {
    mainClass.set("com.example.archshowcase.verify.screenshot.MainKt")
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.example.archshowcase.verify.screenshot.MainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
