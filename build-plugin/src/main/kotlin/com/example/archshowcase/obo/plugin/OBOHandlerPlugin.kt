package com.example.archshowcase.obo.plugin

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class OBOHandlerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        androidComponents.onVariants { variant ->
            // ASM 字节码改写：将三方 SDK 的 Handler 调用重写为 OBOCompat
            variant.instrumentation.transformClassesWith(
                OBOHandlerTransformFactory::class.java,
                InstrumentationScope.ALL,
            ) { params ->
                // Transform 仍保留副作用写报告（命令行 assembleDebug 时可用），
                // 但主要依赖下面的独立 task 生成报告
                val reportFile = project.layout.buildDirectory
                    .file("reports/obo-handler/${variant.name}-intercept.txt")
                params.reportFilePath.set(reportFile.map { it.asFile.absolutePath })
            }
            variant.instrumentation.setAsmFramesComputationMode(
                FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS,
            )

            // 独立的报告生成 task：只读 ASM 扫描 runtime classpath，
            // 有正确的 inputs/outputs 声明，不受 Gradle artifact transform cache 影响
            val capitalizedName = variant.name.replaceFirstChar { it.uppercase() }
            val reportTaskName = "generateOboInterceptReport$capitalizedName"
            val configName = "${variant.name}RuntimeClasspath"
            project.tasks.register(reportTaskName, OBOInterceptReportTask::class.java) {
                val config = project.configurations.named(configName)
                runtimeJars.from(config.map { c ->
                    c.incoming.artifactView {
                        attributes.attribute(
                            org.gradle.api.attributes.Attribute.of("artifactType", String::class.java),
                            "jar",
                        )
                    }.files
                })
                reportFile.set(
                    project.layout.buildDirectory.file("reports/obo-handler/${variant.name}-intercept.txt"),
                )
            }

            // assemble 时自动生成报告
            project.afterEvaluate {
                tasks.named("assemble$capitalizedName") {
                    dependsOn(reportTaskName)
                }
            }
        }
    }
}
