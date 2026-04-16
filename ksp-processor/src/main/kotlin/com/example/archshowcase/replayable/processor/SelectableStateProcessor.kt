package com.example.archshowcase.replayable.processor

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.validate

/**
 * 处理 @SelectableState / @CustomState 注解，为每个标注的 State data class 生成：
 * - {StoreName}StateFields holder 类
 * - StateFlow<Store.State>.rememberFields() Composable 扩展
 */
class SelectableStateProcessor(
    codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private val generator = SelectableStateFieldsGenerator(codeGenerator, logger)
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

        val ctorParams = classDeclaration.primaryConstructor?.parameters.orEmpty()
        val ctorParamNames = ctorParams.mapNotNull { it.name?.asString() }.toSet()
        val excludedNames = collectExcludedFieldNames(classDeclaration)

        val fields = mutableListOf<StateFieldInfo>()

        for (param in ctorParams) {
            val name = param.name?.asString() ?: continue
            if (name in excludedNames || param.hasExcludeAnnotation()) continue
            fields.add(StateFieldInfo(name, param.type.resolve()))
        }

        classDeclaration.getDeclaredProperties()
            .filter { it.simpleName.asString() !in ctorParamNames && !it.isMutable }
            .filter { !it.hasExcludeAnnotation() }
            .forEach { fields.add(StateFieldInfo(it.simpleName.asString(), it.type.resolve())) }

        generator.generate(
            packageName = packageName,
            storeName = storeName,
            fields = fields,
            classDeclaration = classDeclaration
        )
    }

    companion object {
        private val TRIGGER_ANNOTATIONS = listOf(
            "com.example.archshowcase.compose.select.SelectableState",
            "com.example.archshowcase.compose.select.CustomState"
        )
    }
}

private const val EXCLUDE_ANN = "com.example.archshowcase.compose.select.ExcludeFromSelect"

private val EXCLUDED_INTERFACES = setOf(
    "com.example.archshowcase.core.trace.restore.ReplayableState",
    "com.example.archshowcase.core.trace.scroll.ScrollRestorableState"
)

private fun collectExcludedFieldNames(classDeclaration: KSClassDeclaration): Set<String> {
    return classDeclaration.superTypes
        .map { it.resolve().declaration }
        .filterIsInstance<KSClassDeclaration>()
        .filter { it.qualifiedName?.asString() in EXCLUDED_INTERFACES }
        .flatMap { it.getDeclaredProperties() }
        .map { it.simpleName.asString() }
        .toSet()
}

private fun KSValueParameter.hasExcludeAnnotation() =
    annotations.any { it.annotationType.resolve().declaration.qualifiedName?.asString() == EXCLUDE_ANN }

private fun KSPropertyDeclaration.hasExcludeAnnotation() =
    annotations.any { it.annotationType.resolve().declaration.qualifiedName?.asString() == EXCLUDE_ANN }

data class StateFieldInfo(val name: String, val type: KSType)

class SelectableStateProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        SelectableStateProcessor(environment.codeGenerator, environment.logger)
}
