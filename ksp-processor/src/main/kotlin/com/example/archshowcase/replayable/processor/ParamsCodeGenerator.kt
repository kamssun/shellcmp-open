package com.example.archshowcase.replayable.processor

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import java.io.BufferedWriter

/**
 * EventMapperProcessor / ExposureParamsProcessor 共享的参数提取 + 代码生成逻辑。
 * 职责：从 data class 提取基本类型属性 → 应用敏感字段规则 → 生成 mapOf() 代码。
 */
internal object ParamsCodeGenerator {

    data class PropertyEntry(
        val name: String,
        val typeName: String,
        val sensitivity: SensitivityLevel,
    )

    private val PRIMITIVE_TYPES = setOf(
        "kotlin.String", "kotlin.Int", "kotlin.Long", "kotlin.Float",
        "kotlin.Double", "kotlin.Boolean", "kotlin.Short", "kotlin.Byte",
    )

    /** 从 class 提取可追踪属性：基本类型 + 非 nullable + 非 EXCLUDE */
    fun extractProperties(classDecl: KSClassDeclaration): List<PropertyEntry> {
        return classDecl.getDeclaredProperties().mapNotNull { prop ->
            val propName = prop.simpleName.asString()
            val resolved = prop.type.resolve()
            if (resolved.isMarkedNullable) return@mapNotNull null

            val typeName = resolved.declaration.qualifiedName?.asString() ?: return@mapNotNull null
            if (!isPrimitiveOrEnum(typeName, resolved.declaration as? KSClassDeclaration)) {
                return@mapNotNull null
            }

            var sensitivity = sensitivityFor(propName)
            if (sensitivity == SensitivityLevel.EXCLUDE) return@mapNotNull null
            // LENGTH_ONLY (.length) 和 HASH (sha256Hex16) 只对 String 有效
            if (sensitivity in listOf(SensitivityLevel.LENGTH_ONLY, SensitivityLevel.HASH) && typeName != "kotlin.String") {
                sensitivity = SensitivityLevel.PLAIN
            }

            PropertyEntry(propName, typeName, sensitivity)
        }.toList()
    }

    /** 生成值表达式 */
    fun valueExpression(prop: PropertyEntry, accessor: String): String {
        val ref = if (accessor.isEmpty()) prop.name else "$accessor${prop.name}"
        return when (prop.sensitivity) {
            SensitivityLevel.LENGTH_ONLY -> "\"[len=\${$ref.length}]\""
            SensitivityLevel.HASH -> "sha256Hex16($ref)"
            SensitivityLevel.PLAIN -> when (prop.typeName) {
                "kotlin.String" -> ref
                else -> "$ref.toString()"
            }
            SensitivityLevel.EXCLUDE -> error("EXCLUDE should be filtered out")
        }
    }

    fun needsHashImport(properties: List<PropertyEntry>): Boolean =
        properties.any { it.sensitivity == SensitivityLevel.HASH }

    /** 写入 mapOf() 的 entry 行 */
    fun writeMapEntries(
        writer: BufferedWriter,
        properties: List<PropertyEntry>,
        accessor: String,
        indent: String,
    ) {
        for (prop in properties) {
            val key = toSnakeCase(prop.name)
            val value = valueExpression(prop, accessor)
            writer.appendLine("$indent\"$key\" to $value,")
        }
    }

    private fun isPrimitiveOrEnum(typeName: String, declaration: KSClassDeclaration?): Boolean {
        if (typeName in PRIMITIVE_TYPES) return true
        return declaration?.classKind == ClassKind.ENUM_CLASS
    }
}

// ── 共享工具 ──

internal val EXCLUDE_PATTERNS = listOf("url", "link", "uri", "token", "password", "secret", "credential", "apikey", "secretkey", "authkey", "privatekey", "accesskey", "otp", "pin", "captcha", "jwt", "bearer", "authheader", "auth", "code")
internal val LENGTH_PATTERNS = listOf("text", "content", "body", "message")
internal val HASH_PATTERNS = listOf("email", "mail", "phone", "mobile", "tel")

/** camelCase / snake_case 分词正则：authorId → [author, Id], HTMLParser → [HTML, Parser] */
private val WORD_BOUNDARY_REGEX = Regex("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|_")

private fun extractWords(fieldName: String): List<String> =
    fieldName.split(WORD_BOUNDARY_REGEX).map { it.lowercase() }

internal fun sensitivityFor(fieldName: String): SensitivityLevel {
    val words = extractWords(fieldName)
    val segments = buildSet {
        addAll(words)
        for (i in 0 until words.size - 1) {
            add(words[i] + words[i + 1])
        }
    }
    return when {
        EXCLUDE_PATTERNS.any { it in segments } -> SensitivityLevel.EXCLUDE
        LENGTH_PATTERNS.any { it in segments } -> SensitivityLevel.LENGTH_ONLY
        HASH_PATTERNS.any { it in segments } -> SensitivityLevel.HASH
        else -> SensitivityLevel.PLAIN
    }
}

internal enum class SensitivityLevel {
    PLAIN, LENGTH_ONLY, HASH, EXCLUDE
}

internal fun toSnakeCase(input: String): String {
    if (input.isEmpty()) return input
    return buildString {
        for ((i, c) in input.withIndex()) {
            if (c.isUpperCase()) {
                if (i > 0 && !input[i - 1].isUpperCase()) append('_')
                else if (i > 0 && i + 1 < input.length && !input[i + 1].isUpperCase()) append('_')
                append(c.lowercaseChar())
            } else {
                append(c)
            }
        }
    }
}
