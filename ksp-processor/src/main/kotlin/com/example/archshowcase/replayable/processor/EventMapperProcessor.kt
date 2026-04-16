package com.example.archshowcase.replayable.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.Modifier

/**
 * 无注解自动扫描所有 com.example.archshowcase.* 包下的 Store 接口，
 * 为每个 Store 的 Intent sealed class 生成事件映射代码。
 */
class EventMapperProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private val storeEntries = mutableListOf<StoreMapperEntry>()
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
        if (storeEntries.isEmpty()) return
        generateRegistry()
    }

    private fun processClassAndNested(declaration: KSClassDeclaration, file: KSFile) {
        processDeclaration(declaration, file)
        declaration.declarations
            .filterIsInstance<KSClassDeclaration>()
            .forEach { processClassAndNested(it, file) }
    }

    private fun processDeclaration(declaration: KSClassDeclaration, file: KSFile) {
        if (declaration.classKind != ClassKind.INTERFACE) return
        if (!isStoreInterface(declaration)) return

        val qn = declaration.qualifiedName?.asString() ?: return
        if (qn in processed) return
        processed.add(qn)

        val intentClass = findIntentSealedClass(declaration) ?: return
        val storeName = declaration.simpleName.asString()
        val packageName = declaration.packageName.asString()
        val intentVariants = collectIntentVariants(intentClass, storeName)

        if (intentVariants.isEmpty()) return

        originatingFiles.add(file)
        generateMapper(packageName, storeName, intentClass, intentVariants, file)
        storeEntries.add(StoreMapperEntry(packageName, storeName))
    }

    private fun isStoreInterface(declaration: KSClassDeclaration): Boolean {
        return declaration.superTypes.any { ref ->
            val resolved = ref.resolve()
            val declQn = resolved.declaration.qualifiedName?.asString()
            declQn == "com.arkivanov.mvikotlin.core.store.Store"
        }
    }

    private fun findIntentSealedClass(storeDecl: KSClassDeclaration): KSClassDeclaration? {
        return storeDecl.declarations
            .filterIsInstance<KSClassDeclaration>()
            .firstOrNull { inner ->
                inner.simpleName.asString() == "Intent" &&
                    (Modifier.SEALED in inner.modifiers)
            }
    }

    // ── Intent 变体收集（复用 ParamsCodeGenerator） ──

    private fun collectIntentVariants(
        intentClass: KSClassDeclaration,
        storeName: String
    ): List<IntentVariant> {
        return intentClass.getSealedSubclasses().mapNotNull { sub ->
            val name = sub.simpleName.asString()
            val params = when (sub.classKind) {
                ClassKind.OBJECT -> emptyList()
                ClassKind.CLASS -> ParamsCodeGenerator.extractProperties(sub)
                else -> return@mapNotNull null
            }
            IntentVariant(name, sub.classKind == ClassKind.OBJECT, params)
        }.toList()
    }

    // ── 代码生成 ──

    private fun generateMapper(
        packageName: String,
        storeName: String,
        intentClass: KSClassDeclaration,
        variants: List<IntentVariant>,
        originatingFile: KSFile
    ) {
        val intentQn = intentClass.qualifiedName?.asString() ?: run {
            logger.warn("Skipping $storeName: Intent class has no qualifiedName")
            return
        }
        val storePrefix = toSnakeCase(storeName.removeSuffix("Store"))
        val fileName = "${storeName}EventMapper"

        val out = codeGenerator.createNewFile(
            Dependencies(true, originatingFile),
            packageName,
            fileName
        )

        out.bufferedWriter().use { writer ->
            writer.appendLine("package $packageName")
            writer.appendLine()
            writer.appendLine("import com.example.archshowcase.core.analytics.TrackingEvent")
            val needsHash = variants.any { v -> ParamsCodeGenerator.needsHashImport(v.params) }
            if (needsHash) {
                writer.appendLine("import com.example.archshowcase.core.util.sha256Hex16")
            }
            writer.appendLine()
            writer.appendLine("/** KSP 自动生成：$storeName Intent → TrackingEvent 映射 */")
            writer.appendLine("internal fun $intentQn.toTrackingEvent(): TrackingEvent {")
            writer.appendLine("    return when (this) {")

            for (variant in variants) {
                val eventName = "$storePrefix.${toSnakeCase(variant.name)}"
                val variantQn = "$intentQn.${variant.name}"
                if (variant.isObject || variant.params.isEmpty()) {
                    writer.appendLine("        is $variantQn -> TrackingEvent(")
                    writer.appendLine("            name = \"$eventName\",")
                    writer.appendLine("            params = emptyMap(),")
                    writer.appendLine("        )")
                } else {
                    writer.appendLine("        is $variantQn -> TrackingEvent(")
                    writer.appendLine("            name = \"$eventName\",")
                    writer.appendLine("            params = mapOf(")
                    ParamsCodeGenerator.writeMapEntries(writer, variant.params, "", "                ")
                    writer.appendLine("            ),")
                    writer.appendLine("        )")
                }
            }

            writer.appendLine("    }")
            writer.appendLine("}")
        }

        logger.info("Generated $fileName for $storeName (${variants.size} intents)")
    }

    private fun generateRegistry() {
        val packageName = "com.example.archshowcase.presentation"
        val out = codeGenerator.createNewFile(
            Dependencies(true, *originatingFiles.toTypedArray()),
            packageName,
            "EventMapperRegistry"
        )

        out.bufferedWriter().use { writer ->
            writer.appendLine("package $packageName")
            writer.appendLine()
            writer.appendLine("import com.example.archshowcase.core.analytics.TrackingEvent")
            for (entry in storeEntries) {
                writer.appendLine("import ${entry.packageName}.${entry.storeName}")
                writer.appendLine("import ${entry.packageName}.toTrackingEvent")
            }
            writer.appendLine()
            writer.appendLine("/** KSP 自动生成：所有 Store 的 Intent→TrackingEvent 分发器 */")
            writer.appendLine("object EventMapperRegistry {")
            writer.appendLine()
            writer.appendLine("    fun map(storeName: String, intent: Any): TrackingEvent? {")
            writer.appendLine("        return when (intent) {")
            for (entry in storeEntries) {
                writer.appendLine("            is ${entry.storeName}.Intent -> intent.toTrackingEvent()")
            }
            writer.appendLine("            else -> null")
            writer.appendLine("        }")
            writer.appendLine("    }")
            writer.appendLine("}")
        }

        logger.info("Generated EventMapperRegistry with ${storeEntries.size} stores")
    }

}

// ── 内部模型 ──

internal data class StoreMapperEntry(
    val packageName: String,
    val storeName: String,
)

internal data class IntentVariant(
    val name: String,
    val isObject: Boolean,
    val params: List<ParamsCodeGenerator.PropertyEntry>,
)

class EventMapperProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return EventMapperProcessor(
            environment.codeGenerator,
            environment.logger
        )
    }
}
