package com.example.archshowcase.obo.plugin

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.File

/**
 * ASM Transform 参数
 */
interface OBOHandlerParams : InstrumentationParameters {
    @get:Input
    val reportFilePath: Property<String>
}

/**
 * ASM Transform 工厂
 *
 * 扫描所有依赖（含三方 SDK）的字节码，
 * 将 Handler.post/send 系列调用重写为 OBOCompat 静态方法。
 *
 * 拦截报告输出到 build/reports/obo-handler/{variant}-intercept.txt
 */
abstract class OBOHandlerTransformFactory :
    AsmClassVisitorFactory<OBOHandlerParams> {

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor,
    ): ClassVisitor = OBOHandlerClassVisitor(
        instrumentationContext.apiVersion.get(),
        nextClassVisitor,
        classContext.currentClassData.className,
        parameters.get().reportFilePath.get(),
    )

    override fun isInstrumentable(classData: ClassData): Boolean {
        val name = classData.className
        return SKIP_PREFIXES.none { name.startsWith(it) }
    }

    companion object {
        private val SKIP_PREFIXES = OBOHandlerConstants.SKIP_PREFIXES
    }
}

/**
 * 线程安全的报告文件写入
 */
private object ReportWriter {
    private val lock = Any()
    private val initialized = mutableSetOf<String>()

    fun write(filePath: String, line: String) {
        synchronized(lock) {
            val file = File(filePath)
            // 首次写入时清空旧报告
            if (initialized.add(filePath)) {
                file.parentFile?.mkdirs()
                file.writeText("")
            }
            file.appendText(line + "\n")
        }
    }
}

/**
 * ClassVisitor：为每个方法创建拦截 MethodVisitor，收集拦截记录并写入报告文件
 */
private class OBOHandlerClassVisitor(
    api: Int,
    classVisitor: ClassVisitor,
    private val className: String,
    private val reportFilePath: String,
) : ClassVisitor(api, classVisitor) {

    /** callerMethod → Handler.method 调用列表 */
    private val intercepts = mutableListOf<Pair<String, String>>()

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?,
    ): MethodVisitor {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        return OBOHandlerMethodVisitor(api, mv, name, className, intercepts)
    }

    override fun visitEnd() {
        super.visitEnd()
        if (intercepts.isEmpty()) return

        val grouped = intercepts
            .groupBy({ it.first }, { it.second })
            .entries.joinToString { (caller, calls) ->
                val summary = calls.groupingBy { it }.eachCount()
                    .entries.joinToString(", ") { (m, n) -> "Handler.$m ×$n" }
                "$caller() → $summary"
            }
        val line = "$className | $grouped"
        ReportWriter.write(reportFilePath, line)
    }
}

/**
 * MethodVisitor：拦截 Handler 调用指令并重写
 *
 * 将 INVOKEVIRTUAL android/os/Handler.xxx(args)
 * 改为 INVOKESTATIC com/example/archshowcase/core/scheduler/OBOCompat.xxx(Handler, args)
 */
private class OBOHandlerMethodVisitor(
    api: Int,
    methodVisitor: MethodVisitor,
    private val callerMethod: String,
    private val callerClass: String,
    private val intercepts: MutableList<Pair<String, String>>,
) : MethodVisitor(api, methodVisitor) {

    companion object {
        private const val HANDLER = "android/os/Handler"
        private const val COMPAT = "com/example/archshowcase/core/scheduler/OBOCompat"

        /** method name → 支持的描述符列表 */
        private val TARGETS: Map<String, List<String>> = mapOf(
            // ── Runnable 系列 ──
            "post" to listOf(
                "(Ljava/lang/Runnable;)Z",
            ),
            "postAtTime" to listOf(
                "(Ljava/lang/Runnable;J)Z",
                "(Ljava/lang/Runnable;Ljava/lang/Object;J)Z",
            ),
            "postDelayed" to listOf(
                "(Ljava/lang/Runnable;J)Z",
                "(Ljava/lang/Runnable;Ljava/lang/Object;J)Z",
            ),
            // ── Message 系列 ──
            "sendMessage" to listOf(
                "(Landroid/os/Message;)Z",
            ),
            "sendMessageDelayed" to listOf(
                "(Landroid/os/Message;J)Z",
            ),
            "sendMessageAtTime" to listOf(
                "(Landroid/os/Message;J)Z",
            ),
            "sendEmptyMessage" to listOf(
                "(I)Z",
            ),
            "sendEmptyMessageDelayed" to listOf(
                "(IJ)Z",
            ),
            "sendEmptyMessageAtTime" to listOf(
                "(IJ)Z",
            ),
            // ── AtFrontOfQueue 系列 ──
            "postAtFrontOfQueue" to listOf(
                "(Ljava/lang/Runnable;)Z",
            ),
            "sendMessageAtFrontOfQueue" to listOf(
                "(Landroid/os/Message;)Z",
            ),
        )
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean,
    ) {
        if (opcode == Opcodes.INVOKEVIRTUAL &&
            owner == HANDLER &&
            TARGETS[name]?.contains(descriptor) == true
        ) {
            intercepts.add(callerMethod to name)
            // push 调用方类名常量（编译期注入，运行时零解析开销）
            visitLdcInsn(callerClass)
            // (args)Z → (Landroid/os/Handler;args;Ljava/lang/String;)Z
            val args = descriptor.substring(1, descriptor.indexOf(')'))
            val newDescriptor = "(L$HANDLER;${args}Ljava/lang/String;)Z"
            super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                COMPAT,
                name,
                newDescriptor,
                false,
            )
        } else {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        }
    }
}
