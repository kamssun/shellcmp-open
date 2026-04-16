package com.example.archshowcase.core.trace.verification

import com.example.archshowcase.core.util.Log

/**
 * Intent 参数提取器
 *
 * 从运行时 Intent 对象提取 (intentType, params) 对。
 * 镜像 IntentResolverRegistry（params → Intent）的逆操作。
 *
 * 使用 toString() 解析而非反射，以兼容 KMP 全平台。
 * data class 的 toString() 格式为 "ClassName(field1=val1, field2=val2)"
 * data object 的 toString() 格式为 "ClassName"
 */
object IntentParamsExtractor {

    private const val TAG = "IntentParams"

    /**
     * 从运行时 Intent 对象提取类型和参数
     *
     * @param storeName Store 名称
     * @param intent 运行时 Intent 对象
     * @return (intentType, params) 对
     */
    fun extract(storeName: String, intent: Any): Pair<String, Map<String, String>> {
        val intentType = intent::class.simpleName ?: "Unknown"

        val params = try {
            parseToString(intent.toString(), intentType)
        } catch (e: Exception) {
            Log.w(TAG) { "Failed to extract params for $storeName/$intentType: ${e.message}" }
            emptyMap()
        }

        return intentType to params
    }

    /**
     * 从 data class 的 toString() 解析参数
     *
     * 输入示例：
     * - "Push(route=ImageDemo)" → {"route": "ImageDemo"}
     * - "UpdateScrollPosition(firstVisibleIndex=5, offset=100)" → {"firstVisibleIndex": "5", "offset": "100"}
     * - "LoadInitial" → {}（data object）
     */
    internal fun parseToString(str: String, intentType: String): Map<String, String> {
        // data object: "LoadInitial", "Pop" 等
        if (!str.contains("(")) {
            return emptyMap()
        }

        // 提取括号内的内容
        val start = str.indexOf('(')
        val end = str.lastIndexOf(')')
        if (start < 0 || end <= start) return emptyMap()

        val inner = str.substring(start + 1, end)
        if (inner.isBlank()) return emptyMap()

        return parseKeyValuePairs(inner)
    }

    /**
     * 解析 "key1=val1, key2=val2" 格式
     * 支持嵌套括号（如 routes=[Home, ImageDemo]）
     */
    private fun parseKeyValuePairs(input: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        var depth = 0
        var current = StringBuilder()
        val pairs = mutableListOf<String>()

        for (ch in input) {
            when {
                ch == '(' || ch == '[' -> { depth++; current.append(ch) }
                ch == ')' || ch == ']' -> { depth--; current.append(ch) }
                ch == ',' && depth == 0 -> { pairs.add(current.toString().trim()); current.clear() }
                else -> current.append(ch)
            }
        }
        if (current.isNotBlank()) pairs.add(current.toString().trim())

        for (pair in pairs) {
            val eq = pair.indexOf('=')
            if (eq > 0) {
                val key = pair.substring(0, eq).trim()
                val value = pair.substring(eq + 1).trim()
                result[key] = value
            }
        }

        return result
    }
}
