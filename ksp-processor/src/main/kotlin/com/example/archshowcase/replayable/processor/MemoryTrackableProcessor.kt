package com.example.archshowcase.replayable.processor

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate
import java.io.OutputStream

/**
 * 处理 @MemoryTrackable / @CustomState 注解，为每个标注的 State data class 生成：
 * - {Store}MemorySnapshot.kt — memorySnapshot() 扩展（递归扫描嵌套集合）
 * - MemorySnapshotRegistry — 汇总 when 分发器（用于 GC 压力检测）
 */
class MemoryTrackableProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private val registryEntries = mutableListOf<StoreEntry>()
    private val processed = mutableSetOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val deferred = mutableListOf<KSAnnotated>()

        for (ann in TRIGGER_ANNOTATIONS) {
            val symbols = resolver.getSymbolsWithAnnotation(ann)
            deferred.addAll(symbols.filter { !it.validate() })
            symbols
                .filter { it is KSClassDeclaration && it.validate() }
                .forEach { processState(it as KSClassDeclaration) }
        }

        return deferred
    }

    override fun finish() {
        if (registryEntries.isEmpty()) return
        generateRegistry()
    }

    private fun processState(classDeclaration: KSClassDeclaration) {
        val qn = classDeclaration.qualifiedName?.asString() ?: return
        if (qn in processed) return
        processed.add(qn)

        val packageName = classDeclaration.packageName.asString()

        val parentDeclaration = classDeclaration.parentDeclaration as? KSClassDeclaration
        if (parentDeclaration == null) {
            logger.error("State must be nested inside a Store interface", classDeclaration)
            return
        }
        val storeName = parentDeclaration.simpleName.asString()

        val entries = collectSnapshotEntries(classDeclaration)
        if (entries.isEmpty()) return

        generateSnapshot(packageName, storeName, entries, classDeclaration)
        registryEntries.add(StoreEntry(packageName, storeName))
    }

    // ── 递归扫描 ────────────────────────────────────

    /**
     * 递归扫描 State 及其嵌套对象的所有 Collection/Map 字段，
     * 生成 (displayPath, codeExpr) 对。
     *
     * 例：State.conversations: List<Conversation>，Conversation.memberAvatars: List<String>
     * → ("conversations", "conversations.size")
     * → ("conversations[].memberAvatars", "conversations.sumOf { v0 -> v0.memberAvatars.size }")
     *
     * 递归终止于：非项目类型（com.example.archshowcase.*） / 已访问（防环） / 非 class
     */
    private fun collectSnapshotEntries(
        classDeclaration: KSClassDeclaration
    ): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        val visited = mutableSetOf<String>()
        scanClassFields(classDeclaration, "", { it }, "", visited, 0, results)
        return results
    }

    /**
     * @param wrapExpr 将内部 Int 表达式包装到外部上下文（处理 sumOf / ?.let 嵌套）
     * @param selfRef 当前上下文变量名（""=顶层 State, "v0"=sumOf 内部）
     */
    private fun scanClassFields(
        classDecl: KSClassDeclaration,
        displayPrefix: String,
        wrapExpr: (String) -> String,
        selfRef: String,
        visited: MutableSet<String>,
        depth: Int,
        results: MutableList<Pair<String, String>>
    ) {
        val params = classDecl.primaryConstructor?.parameters.orEmpty()
        for (param in params) {
            val name = param.name?.asString() ?: continue
            val type = param.type.resolve()
            val fieldAccess = if (selfRef.isEmpty()) name else "$selfRef.$name"
            scanField(name, type, fieldAccess, displayPrefix, wrapExpr, visited, depth, results)
        }

        val paramNames = params.mapNotNull { it.name?.asString() }.toSet()
        classDecl.getDeclaredProperties()
            .filter { it.simpleName.asString() !in paramNames && !it.isMutable }
            .forEach { prop ->
                val name = prop.simpleName.asString()
                val type = prop.type.resolve()
                val fieldAccess = if (selfRef.isEmpty()) name else "$selfRef.$name"
                scanField(name, type, fieldAccess, displayPrefix, wrapExpr, visited, depth, results)
            }
    }

    private fun scanField(
        name: String,
        type: KSType,
        fieldAccess: String,
        displayPrefix: String,
        wrapExpr: (String) -> String,
        visited: MutableSet<String>,
        depth: Int,
        results: MutableList<Pair<String, String>>
    ) {
        if (isSizeable(type)) {
            // 本字段是集合 — 报告 .size
            results.add(displayPrefix + name to wrapExpr("$fieldAccess.size"))

            // 递归扫描元素类型内部的集合
            val elementType = getCollectionElementType(type) ?: return
            val elementDecl = elementType.declaration as? KSClassDeclaration ?: return
            if (!shouldScan(elementDecl, visited)) return

            val qn = elementDecl.qualifiedName!!.asString()
            visited.add(qn)

            val varName = "v$depth"
            val newWrap: (String) -> String = { inner ->
                wrapExpr("$fieldAccess.sumOf { $varName -> $inner }")
            }
            scanClassFields(
                elementDecl, displayPrefix + name + "[].",
                newWrap, varName, visited, depth + 1, results
            )

            visited.remove(qn)
        } else {
            // 非集合 — 检查是否为含嵌套集合的 class
            val classDecl = type.declaration as? KSClassDeclaration ?: return
            if (!shouldScan(classDecl, visited)) return

            val qn = classDecl.qualifiedName!!.asString()
            visited.add(qn)

            val nullable = type.isMarkedNullable
            val newSelfRef: String
            val newWrap: (String) -> String

            if (nullable) {
                val letVar = "n$depth"
                newSelfRef = letVar
                newWrap = { inner ->
                    wrapExpr("($fieldAccess?.let { $letVar -> $inner } ?: 0)")
                }
            } else {
                newSelfRef = fieldAccess
                newWrap = wrapExpr
            }

            scanClassFields(
                classDecl, displayPrefix + name + ".",
                newWrap, newSelfRef, visited, depth + 1, results
            )

            visited.remove(qn)
        }
    }

    // ── 文件生成 ────────────────────────────────────

    private fun generateSnapshot(
        packageName: String,
        storeName: String,
        entries: List<Pair<String, String>>,
        classDeclaration: KSClassDeclaration
    ) {
        val fileName = "${storeName}MemorySnapshot"
        val file = codeGenerator.createNewFile(
            Dependencies(true, classDeclaration.containingFile!!),
            packageName,
            fileName
        )

        file.use { out ->
            out.appendText("// Auto-generated by MemoryTrackable KSP Processor\n")
            out.appendText("// DO NOT EDIT - This file is regenerated on every build\n")
            out.appendText("@file:Suppress(\"unused\")\n\n")
            out.appendText("package $packageName\n\n")
            out.appendText("/**\n")
            out.appendText(" * 返回 $storeName.State 中所有集合字段的 size（递归扫描嵌套对象）。\n")
            out.appendText(" * 用于 GC 压力检测时快速定位内存大户。\n")
            out.appendText(" */\n")
            out.appendText("fun $storeName.State.memorySnapshot(): Map<String, Int> = mapOf(\n")
            for ((i, entry) in entries.withIndex()) {
                val comma = if (i < entries.lastIndex) "," else ""
                out.appendText("    \"${entry.first}\" to ${entry.second}$comma\n")
            }
            out.appendText(")\n")
        }

        logger.info("Generated $fileName with ${entries.size} entries")
    }

    private fun generateRegistry() {
        val packageName = "com.example.archshowcase.presentation"
        val file = codeGenerator.createNewFile(
            Dependencies.ALL_FILES,
            packageName,
            "MemorySnapshotRegistry"
        )

        file.use { out ->
            out.appendText("// Auto-generated by MemoryTrackable KSP Processor\n")
            out.appendText("// DO NOT EDIT - This file is regenerated on every build\n")
            out.appendText("@file:Suppress(\"unused\")\n\n")
            out.appendText("package $packageName\n\n")

            for (entry in registryEntries.sortedBy { it.storeName }) {
                out.appendText("import ${entry.packageName}.${entry.storeName}\n")
                out.appendText("import ${entry.packageName}.memorySnapshot\n")
            }

            out.appendText("\n")
            out.appendText("/**\n")
            out.appendText(" * KSP 生成的内存快照分发器。\n")
            out.appendText(" *\n")
            out.appendText(" * 根据 Store.state 的运行时类型调用对应的 memorySnapshot() 扩展。\n")
            out.appendText(" */\n")
            out.appendText("object MemorySnapshotRegistry {\n\n")
            out.appendText("    fun snapshot(state: Any): Map<String, Int>? = when (state) {\n")
            for (entry in registryEntries.sortedBy { it.storeName }) {
                out.appendText("        is ${entry.storeName}.State -> state.memorySnapshot()\n")
            }
            out.appendText("        else -> null\n")
            out.appendText("    }\n")
            out.appendText("}\n")
        }

        logger.info("Generated MemorySnapshotRegistry with ${registryEntries.size} entries")
    }

    // ── 工具方法 ────────────────────────────────────

    private fun isSizeable(type: KSType): Boolean {
        val qn = type.declaration.qualifiedName?.asString() ?: return false
        if (qn in SIZEABLE_TYPES) return true
        val classDecl = type.declaration as? KSClassDeclaration ?: return false
        return classDecl.superTypes.any { superRef ->
            val superQn = superRef.resolve().declaration.qualifiedName?.asString()
            superQn in SIZEABLE_TYPES
        }
    }

    private fun getCollectionElementType(type: KSType): KSType? {
        val args = type.arguments
        if (args.isEmpty()) return null
        return args.firstOrNull()?.type?.resolve()
    }

    /** 只递归项目包名内的 class，遇到三方/标准库类型自然停止 */
    private fun shouldScan(decl: KSClassDeclaration, visited: Set<String>): Boolean {
        val qn = decl.qualifiedName?.asString() ?: return false
        if (qn in visited) return false
        if (!qn.startsWith(PROJECT_PACKAGE_PREFIX)) return false
        if (decl.classKind != ClassKind.CLASS) return false
        return true
    }

    private data class StoreEntry(val packageName: String, val storeName: String)

    private fun OutputStream.appendText(str: String) {
        write(str.toByteArray())
    }

    companion object {
        private const val PROJECT_PACKAGE_PREFIX = "com.example.archshowcase."

        private val TRIGGER_ANNOTATIONS = listOf(
            "com.example.archshowcase.compose.select.MemoryTrackable",
            "com.example.archshowcase.compose.select.CustomState"
        )

        private val SIZEABLE_TYPES = setOf(
            "kotlin.collections.List",
            "kotlin.collections.MutableList",
            "kotlin.collections.Set",
            "kotlin.collections.MutableSet",
            "kotlin.collections.Map",
            "kotlin.collections.MutableMap",
            "kotlin.collections.Collection",
            "kotlin.collections.MutableCollection",
            "java.util.List",
            "java.util.Map",
            "java.util.Set",
            "java.util.Collection",
            "kotlin.collections.AbstractList",
        )
    }
}

class MemoryTrackableProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        MemoryTrackableProcessor(environment.codeGenerator, environment.logger)
}
