package com.example.archshowcase.replayable.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.io.OutputStream

/** 收集的 Store resolver 信息 */
data class ResolverInfo(
    val packageName: String,
    val storeName: String,
    val functionName: String,
    val storeQualifiedName: String
)

class VfResolvableProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private val generator = IntentResolverGenerator(codeGenerator, logger)
    private val generatedResolvers = mutableListOf<ResolverInfo>()

    /** forType qualifiedName → fromString expression */
    private val paramAdapters = mutableMapOf<String, String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // 1. 收集 @VfParamAdapter
        collectParamAdapters(resolver)

        // 2. 处理 @VfResolvable
        val symbols = resolver.getSymbolsWithAnnotation("com.example.archshowcase.replayable.VfResolvable")
        val deferred = symbols.filter { !it.validate() }.toMutableList()

        symbols
            .filter { it is KSClassDeclaration && it.validate() }
            .forEach { processVfResolvable(it as KSClassDeclaration) }

        return deferred
    }

    private fun collectParamAdapters(resolver: Resolver) {
        resolver.getSymbolsWithAnnotation("com.example.archshowcase.replayable.VfParamAdapter")
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }
            .forEach { cls ->
                cls.annotations
                    .filter { it.shortName.asString() == "VfParamAdapter" }
                    .forEach { annotation ->
                        val forType = annotation.arguments
                            .find { it.name?.asString() == "forType" }
                            ?.value as? KSType ?: return@forEach
                        val fromString = annotation.arguments
                            .find { it.name?.asString() == "fromString" }
                            ?.value as? String ?: return@forEach

                        val typeName = forType.declaration.qualifiedName?.asString() ?: return@forEach
                        paramAdapters[typeName] = fromString
                    }
            }
    }

    private fun processVfResolvable(declaration: KSClassDeclaration) {
        // 验证：必须是 interface
        if (declaration.classKind != ClassKind.INTERFACE) {
            logger.error("@VfResolvable must be applied to an interface", declaration)
            return
        }

        // 查找内部 sealed interface Intent
        val intentDeclaration = declaration.declarations
            .filterIsInstance<KSClassDeclaration>()
            .find {
                it.simpleName.asString() == "Intent" &&
                    it.classKind == ClassKind.INTERFACE &&
                    Modifier.SEALED in it.modifiers
            }

        if (intentDeclaration == null) {
            logger.error(
                "@VfResolvable: interface '${declaration.simpleName.asString()}' must contain a 'sealed interface Intent'",
                declaration
            )
            return
        }

        // 解析注解参数
        val annotation = declaration.annotations
            .find { it.shortName.asString() == "VfResolvable" }
            ?: return

        val customStoreName = annotation.arguments
            .find { it.name?.asString() == "storeName" }
            ?.value as? String ?: ""

        val storeName = customStoreName.ifEmpty {
            declaration.simpleName.asString()
        }

        val packageName = declaration.packageName.asString()
        val storeQualifiedName = declaration.qualifiedName?.asString() ?: return
        val storeSimpleName = declaration.simpleName.asString()

        // 解析 Intent 子类
        val intentEntries = intentDeclaration.getSealedSubclasses().mapNotNull { sub ->
            parseIntentSubclass(sub, storeSimpleName)
        }.toList()

        if (intentEntries.isEmpty()) {
            logger.warn("@VfResolvable: '${storeSimpleName}' has no Intent subclasses, skipping")
            return
        }

        // 生成解析函数
        val functionName = "resolve${storeSimpleName}Intent"
        generator.generateResolver(
            packageName = packageName,
            storeSimpleName = storeSimpleName,
            storeQualifiedName = storeQualifiedName,
            functionName = functionName,
            intentEntries = intentEntries,
            originatingFile = declaration.containingFile!!
        )

        generatedResolvers.add(
            ResolverInfo(
                packageName = packageName,
                storeName = storeName,
                functionName = functionName,
                storeQualifiedName = storeQualifiedName
            )
        )
    }

    private fun parseIntentSubclass(
        sub: KSClassDeclaration,
        storeSimpleName: String
    ): IntentEntry? {
        val simpleName = sub.simpleName.asString()

        return when (sub.classKind) {
            ClassKind.OBJECT -> IntentEntry.ObjectIntent(simpleName)
            ClassKind.CLASS -> {
                val constructor = sub.primaryConstructor ?: run {
                    logger.error(
                        "@VfResolvable: Intent '$simpleName' in '$storeSimpleName' must have a primary constructor",
                        sub
                    )
                    return null
                }
                val params = constructor.parameters.mapNotNull { param ->
                    val paramName = param.name?.asString() ?: return@mapNotNull null
                    val resolvedType = param.type.resolve()
                    val typeInfo = resolveParamType(resolvedType, paramName, simpleName, storeSimpleName, param)
                        ?: return null // error already logged
                    typeInfo
                }
                IntentEntry.DataClassIntent(simpleName, params)
            }
            else -> {
                logger.error(
                    "@VfResolvable: Intent '$simpleName' in '$storeSimpleName' must be data object or data class",
                    sub
                )
                null
            }
        }
    }

    private fun resolveParamType(
        type: KSType,
        paramName: String,
        intentName: String,
        storeSimpleName: String,
        node: KSNode
    ): IntentParamInfo? {
        val declaration = type.declaration
        val qualifiedName = declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()

        // 内置基本类型
        val builtinExpr = when (qualifiedName) {
            "kotlin.String" -> "vfIntent.params[\"$paramName\"] ?: \"\""
            "kotlin.Int" -> "vfIntent.params[\"$paramName\"]?.toIntOrNull() ?: 0"
            "kotlin.Long" -> "vfIntent.params[\"$paramName\"]?.toLongOrNull() ?: 0L"
            "kotlin.Boolean" -> "vfIntent.params[\"$paramName\"]?.toBoolean() ?: false"
            "kotlin.Float" -> "vfIntent.params[\"$paramName\"]?.toFloatOrNull() ?: 0f"
            "kotlin.Double" -> "vfIntent.params[\"$paramName\"]?.toDoubleOrNull() ?: 0.0"
            else -> null
        }

        if (builtinExpr != null) {
            return IntentParamInfo(paramName, qualifiedName, builtinExpr)
        }

        // Enum 类型
        if (declaration is KSClassDeclaration && declaration.classKind == ClassKind.ENUM_CLASS) {
            val expr = "$qualifiedName.valueOf(vfIntent.params[\"$paramName\"] ?: \"\")"
            return IntentParamInfo(paramName, qualifiedName, expr, imports = listOf(qualifiedName))
        }

        // List<T> 类型
        if (qualifiedName == "kotlin.collections.List") {
            val typeArg = type.arguments.firstOrNull()?.type?.resolve() ?: run {
                logger.error(
                    "@VfResolvable: Cannot resolve List type argument for '$paramName' in '$intentName' of '$storeSimpleName'",
                    node
                )
                return null
            }
            val elementTypeName = typeArg.declaration.qualifiedName?.asString() ?: ""
            val elementAdapter = paramAdapters[elementTypeName]

            if (elementAdapter != null) {
                val expr = "vfIntent.params[\"$paramName\"]?.split(\",\")?.map { $elementAdapter(it.trim()) } ?: emptyList()"
                return IntentParamInfo(paramName, qualifiedName, expr)
            }

            // 检查元素是否为基本类型
            val elementExpr = when (elementTypeName) {
                "kotlin.String" -> "it.trim()"
                "kotlin.Int" -> "it.trim().toInt()"
                else -> null
            }
            if (elementExpr != null) {
                val expr = "vfIntent.params[\"$paramName\"]?.split(\",\")?.map { $elementExpr } ?: emptyList()"
                return IntentParamInfo(paramName, qualifiedName, expr)
            }

            logger.error(
                "@VfResolvable error in $storeSimpleName:\n" +
                    "  Intent \"$intentName\" parameter \"$paramName\" has type List<$elementTypeName>\n" +
                    "  which has no registered VfParamAdapter for element type.\n" +
                    "  Fix: Add @VfParamAdapter(forType = ${typeArg.declaration.simpleName.asString()}::class, fromString = \"...\")",
                node
            )
            return null
        }

        // 自定义适配器
        val adapter = paramAdapters[qualifiedName]
        if (adapter != null) {
            val expr = "$adapter(vfIntent.params[\"$paramName\"] ?: \"\")"
            return IntentParamInfo(paramName, qualifiedName, expr, imports = listOf(qualifiedName))
        }

        // 未知类型 → 编译时报错
        logger.error(
            "@VfResolvable error in $storeSimpleName:\n" +
                "  Intent \"$intentName\" parameter \"$paramName\" has type ${declaration.simpleName.asString()}\n" +
                "  which has no registered VfParamAdapter.\n" +
                "  Fix: Add @VfParamAdapter(forType = ${declaration.simpleName.asString()}::class, fromString = \"...\")\n" +
                "  Or: Remove @VfResolvable from $storeSimpleName if VF is not needed.",
            node
        )
        return null
    }

    override fun finish() {
        if (generatedResolvers.isNotEmpty()) {
            generateRegistry()
        }
    }

    private fun generateRegistry() {
        val targetPackage = "com.example.archshowcase.core.trace.verification"
        val fileName = "GeneratedIntentResolverRegistry"

        val file = codeGenerator.createNewFile(
            Dependencies(false),
            targetPackage,
            fileName
        )

        file.use { out ->
            out.appendText("// Auto-generated by VfResolvable KSP Processor\n")
            out.appendText("// DO NOT EDIT - This file is regenerated on every build\n")
            out.appendText("@file:Suppress(\"unused\")\n\n")
            out.appendText("package $targetPackage\n\n")

            // 导入所有 resolver 函数
            val imports = generatedResolvers.map { "${it.packageName}.${it.functionName}" }.sorted()
            imports.forEach { out.appendText("import $it\n") }
            out.appendText("\n")

            out.appendText("/**\n")
            out.appendText(" * 自动生成的 Intent 解析注册表\n")
            out.appendText(" *\n")
            out.appendText(" * 由 KSP @VfResolvable 处理器生成，替代手写 IntentResolverRegistry。\n")
            out.appendText(" */\n")
            out.appendText("object GeneratedIntentResolverRegistry {\n\n")

            out.appendText("    private val resolvers: Map<String, (VfIntent) -> Any?> = mapOf(\n")
            generatedResolvers.forEachIndexed { index, info ->
                val comma = if (index < generatedResolvers.size - 1) "," else ""
                out.appendText("        \"${info.storeName}\" to ::${info.functionName}$comma\n")
            }
            out.appendText("    )\n\n")

            out.appendText("    fun resolve(vfIntent: VfIntent): Any? =\n")
            out.appendText("        resolvers[vfIntent.store]?.invoke(vfIntent)\n\n")

            out.appendText("    fun supportedStores(): Set<String> = resolvers.keys\n")
            out.appendText("}\n")
        }

        logger.info("Generated IntentResolverRegistry with ${generatedResolvers.size} stores: ${generatedResolvers.map { it.storeName }}")
    }

    private fun OutputStream.appendText(str: String) {
        write(str.toByteArray())
    }
}

class VfResolvableProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return VfResolvableProcessor(
            environment.codeGenerator,
            environment.logger
        )
    }
}
