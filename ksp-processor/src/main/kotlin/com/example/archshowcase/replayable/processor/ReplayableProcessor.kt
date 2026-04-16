package com.example.archshowcase.replayable.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.io.OutputStream

/**
 * ExportStrategy 信息
 */
data class StrategyInfo(
    val packageName: String,
    val strategyName: String,
    val fullyQualifiedName: String
)

class ReplayableProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    private val isDebug = options["replayable.debug"] == "true"
    private val storeFactoryGenerator = StoreFactoryGenerator(codeGenerator, logger)
    private val exportStrategyGenerator = ExportStrategyGenerator(codeGenerator, logger)

    // 收集所有生成的 ExportStrategy 信息
    private val generatedStrategies = mutableListOf<StrategyInfo>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // 只处理 @Replayable 注解（统一入口）
        val symbols = resolver.getSymbolsWithAnnotation("com.example.archshowcase.replayable.Replayable")
        val ret = symbols.filter { !it.validate() }.toMutableList()

        symbols
            .filter { it is KSClassDeclaration && it.validate() }
            .forEach { it.accept(ReplayableVisitor(), Unit) }

        return ret
    }

    inner class ReplayableVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            // 遍历所有 @Replayable 注解（支持 @Repeatable）
            val annotations = classDeclaration.annotations
                .filter { it.shortName.asString() == "Replayable" }
                .toList()

            if (annotations.isEmpty()) return

            for (annotation in annotations) {
                processAnnotation(classDeclaration, annotation)
            }
        }

        private fun processAnnotation(classDeclaration: KSClassDeclaration, annotation: KSAnnotation) {
            // === 自动推断 stateClass ===
            val explicitStateClass = annotation.arguments
                .find { it.name?.asString() == "stateClass" }
                ?.value as? KSType

            val stateClass = if (explicitStateClass != null && explicitStateClass.declaration.simpleName.asString() != "Nothing") {
                explicitStateClass
            } else {
                // 自动推断：查找外部类的 State 内部类
                val parentClass = classDeclaration.parentDeclaration as? KSClassDeclaration
                val stateDeclaration = parentClass?.declarations
                    ?.filterIsInstance<KSClassDeclaration>()
                    ?.find { it.simpleName.asString() == "State" }

                if (stateDeclaration != null) {
                    stateDeclaration.asStarProjectedType()
                } else {
                    logger.error("@Replayable: Cannot auto-infer stateClass. Record '${classDeclaration.simpleName.asString()}' must be inside a Store interface with a State inner class, or specify stateClass explicitly.", classDeclaration)
                    return
                }
            }

            val packageName = classDeclaration.packageName.asString()
            val recordClassName = classDeclaration.qualifiedName?.asString()?.removePrefix("$packageName.")
                ?: classDeclaration.simpleName.asString()
            val stateClassName = stateClass.declaration.simpleName.asString()
            val stateFullName = stateClass.declaration.qualifiedName?.asString() ?: stateClassName

            val generateStoreHelpers = annotation.arguments
                .find { it.name?.asString() == "generateStoreHelpers" }
                ?.value as? Boolean ?: true
            val generateExportStrategy = annotation.arguments
                .find { it.name?.asString() == "generateExportStrategy" }
                ?.value as? Boolean ?: true

            // === storeName：非空时直接使用，空时从父类推导 ===
            val customStoreName = annotation.arguments
                .find { it.name?.asString() == "storeName" }
                ?.value as? String ?: ""

            // 从 Record 推断 Store 信息（用于获取 storeClass 的 containingFile）
            val parentClass = classDeclaration.parentDeclaration as? KSClassDeclaration
            val storeClass = parentClass ?: stateClass.declaration.parentDeclaration as? KSClassDeclaration

            val storeName = if (customStoreName.isNotEmpty()) {
                customStoreName
            } else {
                storeClass?.simpleName?.asString() ?: run {
                    logger.error("@Replayable: Cannot derive storeName. Specify storeName explicitly.", classDeclaration)
                    return
                }
            }

            // 用于 Dependencies 的声明（优先用 storeClass，fallback 到 record 自身）
            val dependencyDeclaration = storeClass ?: classDeclaration

            if (generateStoreHelpers) {
                val storeFullName = storeClass?.qualifiedName?.asString() ?: storeName
                val recordFullName = classDeclaration.qualifiedName?.asString() ?: recordClassName

                storeFactoryGenerator.generate(
                    packageName = packageName,
                    storeName = storeName,
                    storeClassName = stateFullName,
                    stateClassName = stateClassName,
                    recordClassName = recordFullName,
                    classDeclaration = dependencyDeclaration
                )

                if (generateExportStrategy) {
                    val storeNameConstant = toSnakeCase(storeName) + "_NAME"
                    exportStrategyGenerator.generate(
                        packageName = packageName,
                        storeName = storeName,
                        storeFullName = storeFullName,
                        stateFullName = stateFullName,
                        recordFullName = recordFullName,
                        storeNameConstant = storeNameConstant,
                        classDeclaration = dependencyDeclaration
                    )

                    val strategyName = "${storeName}ExportStrategy"
                    generatedStrategies.add(
                        StrategyInfo(
                            packageName = packageName,
                            strategyName = strategyName,
                            fullyQualifiedName = "$packageName.$strategyName"
                        )
                    )
                }
            }
        }

        private fun toSnakeCase(str: String): String {
            return str.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()
        }
    }

    private fun OutputStream.appendText(str: String) {
        this.write(str.toByteArray())
    }

    override fun finish() {
        // 生成注册文件（如果有策略需要注册）
        if (generatedStrategies.isNotEmpty()) {
            generateRegistrationFile()
        }
    }

    private fun generateRegistrationFile() {
        val targetPackage = "com.example.archshowcase.core.trace.export"
        val fileName = "GeneratedExportStrategies"

        val file = codeGenerator.createNewFile(
            Dependencies(false), // aggregating=false，因为依赖所有 @Replayable 文件
            targetPackage,
            fileName
        )

        file.use { out ->
            out.appendText("// Auto-generated by Replayable KSP Processor\n")
            out.appendText("// DO NOT EDIT - This file is regenerated on every build\n")
            out.appendText("@file:Suppress(\"unused\")\n\n")
            out.appendText("package $targetPackage\n\n")

            // 导入所有策略
            generatedStrategies.forEach { strategy ->
                out.appendText("import ${strategy.fullyQualifiedName}\n")
            }
            out.appendText("\n")

            // 生成注册函数
            out.appendText("""
                |/**
                | * 自动注册所有生成的 ExportStrategy
                | *
                | * 此函数由 KSP 自动生成，包含所有标注了 @Replayable 的 Store 的 ExportStrategy。
                | * 在编译时生成，无性能损耗。
                | */
                |fun registerGeneratedExportStrategies() {
            """.trimMargin())

            // 为每个策略生成注册调用
            generatedStrategies.forEach { strategy ->
                out.appendText("\n    StoreExportRegistry.register(${strategy.strategyName})")
            }

            out.appendText("\n}\n")
        }

        logger.info("Generated registration file with ${generatedStrategies.size} strategies: ${generatedStrategies.map { it.strategyName }}")
    }
}

class ReplayableProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ReplayableProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options
        )
    }
}
