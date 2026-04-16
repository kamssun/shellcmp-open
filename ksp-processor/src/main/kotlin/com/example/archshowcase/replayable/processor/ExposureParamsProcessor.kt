package com.example.archshowcase.replayable.processor

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
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.symbol.Modifier

/**
 * 无注解全量扫描 com.example.archshowcase.* 下所有 data class，
 * 为每个生成 toExposureParams() 扩展 + 中央 ExposureParamsRegistry。
 *
 * 只提取基本类型属性（String/Number/Boolean/Enum），复用 ParamsCodeGenerator 敏感字段规则。
 */
class ExposureParamsProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    private val entries = mutableListOf<DataClassEntry>()
    private val processed = mutableSetOf<String>()
    private val originatingFiles = mutableListOf<KSFile>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getAllFiles()
            .filter { it.packageName.asString().startsWith("com.example.archshowcase.") }
            .forEach { file ->
                file.declarations
                    .filterIsInstance<KSClassDeclaration>()
                    .forEach { processClassAndNested(it, file) }
            }
        return emptyList()
    }

    override fun finish() {
        if (entries.isEmpty()) return
        generateRegistry()
    }

    private fun processClassAndNested(declaration: KSClassDeclaration, file: KSFile) {
        processDeclaration(declaration, file)
        declaration.declarations
            .filterIsInstance<KSClassDeclaration>()
            .forEach { processClassAndNested(it, file) }
    }

    private fun processDeclaration(declaration: KSClassDeclaration, file: KSFile) {
        if (declaration.classKind != ClassKind.CLASS) return
        if (Modifier.DATA !in declaration.modifiers) return
        // 跳过自身或任何外层声明为 private 的 data class，无法生成 internal 扩展
        if (!isAccessible(declaration)) return

        val qn = declaration.qualifiedName?.asString() ?: return
        if (qn in processed) return
        processed.add(qn)

        val properties = ParamsCodeGenerator.extractProperties(declaration)
        if (properties.isEmpty()) return

        val packageName = declaration.packageName.asString()
        val simpleName = declaration.simpleName.asString()
        val qualifiedName = qn

        val fileBaseName = qualifiedName.removePrefix("$packageName.").replace(".", "_")

        originatingFiles.add(file)
        generateParamsFunction(packageName, fileBaseName, qualifiedName, properties, file)
        entries.add(DataClassEntry(packageName, fileBaseName, qualifiedName))
    }

    private fun generateParamsFunction(
        packageName: String,
        fileBaseName: String,
        qualifiedName: String,
        properties: List<ParamsCodeGenerator.PropertyEntry>,
        originatingFile: KSFile,
    ) {
        val fileName = "${fileBaseName}ExposureParams"
        val funcName = "${fileBaseName}ExposureParams"
        val out = codeGenerator.createNewFile(
            Dependencies(true, originatingFile),
            packageName,
            fileName,
        )

        out.bufferedWriter().use { writer ->
            writer.appendLine("package $packageName")
            writer.appendLine()
            if (ParamsCodeGenerator.needsHashImport(properties)) {
                writer.appendLine("import com.example.archshowcase.core.util.sha256Hex16")
                writer.appendLine()
            }
            writer.appendLine("/** KSP 自动生成：${qualifiedName.substringAfterLast('.')} → 曝光参数提取 */")
            writer.appendLine("internal fun $funcName(item: $qualifiedName): Map<String, String> = mapOf(")
            ParamsCodeGenerator.writeMapEntries(writer, properties, "item.", "    ")
            writer.appendLine(")")
        }

        logger.info("Generated $fileName (${properties.size} properties)")
    }

    private fun generateRegistry() {
        val packageName = "com.example.archshowcase.presentation"
        val out = codeGenerator.createNewFile(
            Dependencies(true, *originatingFiles.toTypedArray()),
            packageName,
            "ExposureParamsRegistry",
        )

        // 为每个 params 函数生成唯一 import alias，避免同名冲突
        val imports = buildImports(entries)

        out.bufferedWriter().use { writer ->
            writer.appendLine("package $packageName")
            writer.appendLine()
            for (import in imports.values) {
                writer.appendLine("import ${import.fqn} as ${import.alias}")
            }
            writer.appendLine()
            writer.appendLine("/** KSP 自动生成：data class → 曝光参数提取分发器 */")
            writer.appendLine("object ExposureParamsRegistry {")
            writer.appendLine()
            writer.appendLine("    fun extract(item: Any): Map<String, String> = when (item) {")
            for (entry in entries) {
                val alias = imports[entry.qualifiedName]!!.alias
                writer.appendLine("        is ${entry.qualifiedName} -> $alias(item)")
            }
            writer.appendLine("        else -> emptyMap()")
            writer.appendLine("    }")
            writer.appendLine("}")
        }

        logger.info("Generated ExposureParamsRegistry with ${entries.size} data classes")
    }

    /** 为每个 data class 的 params 函数生成唯一 import alias */
    private fun buildImports(entries: List<DataClassEntry>): Map<String, ImportEntry> {
        val result = mutableMapOf<String, ImportEntry>()
        val usedAliases = mutableSetOf<String>()
        for (entry in entries) {
            val funcName = "${entry.funcBaseName}ExposureParams"
            val fqn = "${entry.packageName}.$funcName"
            var alias = funcName
            if (alias in usedAliases) {
                val prefix = entry.packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
                alias = "$prefix$funcName"
            }
            // 仍然碰撞时追加递增后缀
            var counter = 2
            val baseAlias = alias
            while (alias in usedAliases) {
                alias = "${baseAlias}_$counter"
                counter++
            }
            usedAliases.add(alias)
            result[entry.qualifiedName] = ImportEntry(fqn, alias)
        }
        return result
    }

    /** 递归检查自身及所有外层声明都不是 private */
    private fun isAccessible(declaration: KSClassDeclaration): Boolean {
        if (declaration.isPrivate() || Modifier.PRIVATE in declaration.modifiers) return false
        val parent = declaration.parentDeclaration as? KSClassDeclaration ?: return true
        return isAccessible(parent)
    }
}

internal data class ImportEntry(val fqn: String, val alias: String)

internal data class DataClassEntry(
    val packageName: String,
    val funcBaseName: String,
    val qualifiedName: String,
)

class ExposureParamsProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ExposureParamsProcessor(
            environment.codeGenerator,
            environment.logger,
        )
    }
}
