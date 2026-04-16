package com.example.archshowcase.replayable.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate

/** Route 子类模型 */
sealed interface RouteEntry {
    val simpleName: String
    val qualifiedName: String

    /** data object — 无参数路由 */
    data class ObjectRoute(
        override val simpleName: String,
        override val qualifiedName: String
    ) : RouteEntry

    /** data class — 带参数路由（所有参数必须为 String） */
    data class ParameterizedRoute(
        override val simpleName: String,
        override val qualifiedName: String,
        val params: List<String>
    ) : RouteEntry
}

class RouteRegistryProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private val generator = RouteSerializationGenerator(codeGenerator, logger)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("com.example.archshowcase.replayable.RouteRegistry")
        val deferred = symbols.filter { !it.validate() }.toMutableList()

        symbols
            .filter { it is KSClassDeclaration && it.validate() }
            .forEach { processRouteRegistry(it as KSClassDeclaration) }

        return deferred
    }

    private fun processRouteRegistry(declaration: KSClassDeclaration) {
        if (declaration.classKind != ClassKind.INTERFACE ||
            Modifier.SEALED !in declaration.modifiers
        ) {
            logger.error("@RouteRegistry must be applied to a sealed interface", declaration)
            return
        }

        val annotation = declaration.annotations
            .find { it.shortName.asString() == "RouteRegistry" }
            ?: return

        val entries = declaration.getSealedSubclasses().mapNotNull { sub ->
            parseSubclass(sub)
        }.toList()

        if (entries.isEmpty()) {
            logger.error("@RouteRegistry: sealed interface has no subclasses", declaration)
            return
        }

        val fallbackParam = annotation.arguments
            .find { it.name?.asString() == "fallback" }
            ?.value as? String ?: ""

        val fallback = fallbackParam.ifEmpty {
            entries.filterIsInstance<RouteEntry.ObjectRoute>().firstOrNull()?.simpleName
                ?: entries.first().simpleName
        }

        generator.generate(
            packageName = declaration.packageName.asString(),
            sealedName = declaration.simpleName.asString(),
            sealedQualifiedName = declaration.qualifiedName!!.asString(),
            entries = entries,
            fallback = fallback,
            originatingFile = declaration.containingFile!!
        )
    }

    private fun parseSubclass(sub: KSClassDeclaration): RouteEntry? {
        val simpleName = sub.simpleName.asString()
        val qualifiedName = sub.qualifiedName?.asString() ?: return null

        return when (sub.classKind) {
            ClassKind.OBJECT -> RouteEntry.ObjectRoute(simpleName, qualifiedName)
            ClassKind.CLASS -> {
                val constructor = sub.primaryConstructor ?: run {
                    logger.error("@RouteRegistry: data class '$simpleName' must have a primary constructor", sub)
                    return null
                }
                val params = constructor.parameters.map { param ->
                    val typeName = param.type.resolve().declaration.qualifiedName?.asString()
                    if (typeName != "kotlin.String") {
                        logger.error(
                            "@RouteRegistry: data class '$simpleName' parameter '${param.name?.asString()}' must be String, got $typeName",
                            param
                        )
                        return null
                    }
                    param.name?.asString() ?: return null
                }
                RouteEntry.ParameterizedRoute(simpleName, qualifiedName, params)
            }
            else -> {
                logger.error("@RouteRegistry: subclass '$simpleName' must be data object or data class", sub)
                null
            }
        }
    }
}

class RouteRegistryProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return RouteRegistryProcessor(
            environment.codeGenerator,
            environment.logger
        )
    }
}
