package com.example.archshowcase.network

import com.example.archshowcase.core.trace.verification.NetworkRecording
import com.example.archshowcase.core.trace.verification.NetworkTape
import kotlin.concurrent.Volatile

/**
 * 验证模式下的 NetworkTape 持有器
 *
 * 按 "METHOD url" 分组匹配，每组独立 cursor。
 * 同一 URL 多次请求 → 组内按录制顺序逐条消费。
 *
 * 由 VerificationReceiver.handleInit() 在读取 VF 时填充，
 * NetworkModule 在创建 HttpClient 时检查。
 */
object VerificationTapeHolder {
    @Volatile
    var tape: NetworkTape? = null
        private set

    @Volatile
    private var urlQueues: Map<String, List<NetworkRecording>> = emptyMap()

    @Volatile
    private var urlCursors: MutableMap<String, Int> = mutableMapOf()

    fun load(newTape: NetworkTape) {
        tape = newTape
        urlQueues = newTape.recordings.groupBy { "${it.method} ${it.url}" }
        urlCursors = mutableMapOf()
    }

    fun nextRecording(method: String, url: String): NetworkRecording? {
        val key = "$method $url"
        val queue = urlQueues[key] ?: return null
        val cursor = urlCursors.getOrPut(key) { 0 }
        val recording = queue.getOrNull(cursor) ?: return null
        urlCursors[key] = cursor + 1
        return recording
    }

    fun clear() {
        tape = null
        urlQueues = emptyMap()
        urlCursors = mutableMapOf()
    }
}
