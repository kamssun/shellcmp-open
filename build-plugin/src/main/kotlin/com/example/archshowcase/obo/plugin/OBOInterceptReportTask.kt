package com.example.archshowcase.obo.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.util.jar.JarFile

/**
 * 独立的 OBO 拦截报告生成 task。
 *
 * 用只读 ASM 扫描 runtime classpath 中的 JAR，检测 Handler 方法调用，
 * 输出拦截报告。与 ASM Transform 的缓存行为完全解耦。
 */
@CacheableTask
abstract class OBOInterceptReportTask : DefaultTask() {

    /** debug variant 的 runtime classpath（JAR 文件集合） */
    @get:Classpath
    abstract val runtimeJars: ConfigurableFileCollection

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val records = mutableListOf<String>()

        runtimeJars.files
            .filter { it.extension == "jar" && it.exists() }
            .forEach { jar ->
                JarFile(jar).use { jf ->
                    jf.entries().asSequence()
                        .filter { it.name.endsWith(".class") }
                        .forEach { entry ->
                            val className = entry.name
                                .removeSuffix(".class")
                                .replace('/', '.')
                            if (OBOHandlerConstants.SKIP_PREFIXES.none { className.startsWith(it) }) {
                                scanClass(jf.getInputStream(entry).readBytes(), className, records)
                            }
                        }
                }
            }

        val output = reportFile.get().asFile
        output.parentFile?.mkdirs()
        output.writeText(records.joinToString("\n", postfix = if (records.isNotEmpty()) "\n" else ""))
        logger.lifecycle("OBO intercept report: ${records.size} classes → ${output.absolutePath}")
    }

    private fun scanClass(bytes: ByteArray, className: String, records: MutableList<String>) {
        val intercepts = mutableListOf<Pair<String, String>>()
        val cr = ClassReader(bytes)
        cr.accept(object : ClassVisitor(Opcodes.ASM9) {
            override fun visitMethod(
                access: Int, name: String, descriptor: String,
                signature: String?, exceptions: Array<out String>?,
            ): MethodVisitor {
                return object : MethodVisitor(Opcodes.ASM9) {
                    override fun visitMethodInsn(
                        opcode: Int, owner: String, mName: String,
                        mDescriptor: String, isInterface: Boolean,
                    ) {
                        if (opcode == Opcodes.INVOKEVIRTUAL &&
                            OBOHandlerConstants.isHandlerCall(owner, mName)
                        ) {
                            intercepts.add(name to mName)
                        }
                    }
                }
            }
        }, ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)

        if (intercepts.isNotEmpty()) {
            val grouped = intercepts
                .groupBy({ it.first }, { it.second })
                .entries.joinToString { (caller, calls) ->
                    val summary = calls.groupingBy { it }.eachCount()
                        .entries.joinToString(", ") { (m, n) -> "Handler.$m ×$n" }
                    "$caller() → $summary"
                }
            records.add("$className | $grouped")
        }
    }
}
