plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    compileOnly("com.android.tools.build:gradle:${libs.versions.agp.get()}")
}

gradlePlugin {
    plugins {
        register("oboHandler") {
            id = "com.example.archshowcase.obo-handler"
            implementationClass = "com.example.archshowcase.obo.plugin.OBOHandlerPlugin"
        }
    }
}
