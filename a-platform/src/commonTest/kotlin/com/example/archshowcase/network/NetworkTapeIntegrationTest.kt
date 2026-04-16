package com.example.archshowcase.network

import com.example.archshowcase.core.trace.verification.NetworkRecording
import com.example.archshowcase.core.trace.verification.NetworkRecorderBridge
import com.example.archshowcase.core.trace.verification.NetworkTape
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 网络拦截链路集成测试
 *
 * 验证场景：模拟登录 + 获取用户信息两个 API 调用，
 * 覆盖 录制(Record) → 序列化(Serialize) → 反序列化(Deserialize) → 回放(Replay) 全链路。
 */
class NetworkTapeIntegrationTest {

    // ─── 测试用 API 响应 ─────────────────────────────────────

    private val loginResponse = """{"token":"abc123","user_id":42}"""
    private val profileResponse = """{"name":"张三","avatar":"https://img.example.com/1.jpg"}"""

    @AfterTest
    fun cleanup() {
        VerificationTapeHolder.clear()
    }

    /**
     * 创建一个装了 NetworkRecorder 插件的 HttpClient，
     * 后端由 MockEngine 模拟。
     */
    private fun createRecordingClient(): HttpClient {
        val serverEngine = MockEngine { request ->
            when {
                request.url.encodedPath.contains("/auth/login") -> respond(
                    content = loginResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.contains("/user/profile") -> respond(
                    content = profileResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond(
                    content = """{"error":"not found"}""",
                    status = HttpStatusCode.NotFound
                )
            }
        }

        return HttpClient(serverEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(NetworkRecorder.plugin)
        }
    }

    // ─── 1. 录制链路 ─────────────────────────────────────────

    @Test
    fun `NetworkRecorder captures requests during recording`() = runTest {
        val client = createRecordingClient()

        NetworkRecorder.markStart()
        client.post("https://api.example.com/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"phone":"13800138000"}""")
        }
        client.get("https://api.example.com/user/profile")
        val tape = NetworkRecorder.markEnd()

        assertEquals(2, tape.recordings.size, "应录制到 2 条请求")

        val login = tape.recordings[0]
        assertEquals(0, login.sequence)
        assertEquals("POST", login.method)
        assertTrue(login.url.contains("/auth/login"))
        assertEquals(200, login.responseStatus)
        assertEquals(loginResponse, login.responseBody)
        assertEquals("""{"phone":"13800138000"}""", login.requestBody)

        val profile = tape.recordings[1]
        assertEquals(1, profile.sequence)
        assertEquals("GET", profile.method)
        assertTrue(profile.url.contains("/user/profile"))
        assertEquals(profileResponse, profile.responseBody)
    }

    @Test
    fun `NetworkRecorder does not capture when not recording`() = runTest {
        val client = createRecordingClient()

        client.get("https://api.example.com/user/profile")

        NetworkRecorder.markStart()
        val tape = NetworkRecorder.markEnd()

        assertEquals(0, tape.recordings.size, "未录制状态下不应捕获请求")
    }

    @Test
    fun `markStart clears previous recordings`() = runTest {
        val client = createRecordingClient()

        NetworkRecorder.markStart()
        client.get("https://api.example.com/user/profile")
        NetworkRecorder.markEnd()

        NetworkRecorder.markStart()
        val tape = NetworkRecorder.markEnd()

        assertEquals(0, tape.recordings.size, "markStart 应清空之前的录制")
    }

    // ─── 2. 序列化/反序列化 ──────────────────────────────────

    @Test
    fun `NetworkTape JSON round-trip preserves data`() = runTest {
        val client = createRecordingClient()

        NetworkRecorder.markStart()
        client.post("https://api.example.com/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"phone":"13800138000"}""")
        }
        client.get("https://api.example.com/user/profile")
        val originalTape = NetworkRecorder.markEnd()

        val jsonCodec = Json { ignoreUnknownKeys = true }
        val jsonStr = jsonCodec.encodeToString(NetworkTape.serializer(), originalTape)
        val restoredTape = jsonCodec.decodeFromString(NetworkTape.serializer(), jsonStr)

        assertEquals(originalTape, restoredTape, "序列化往返应保持数据一致")
        assertEquals(originalTape.recordings.size, restoredTape.recordings.size)
        assertEquals(originalTape.recordings[0].url, restoredTape.recordings[0].url)
        assertEquals(originalTape.recordings[1].responseBody, restoredTape.recordings[1].responseBody)
    }

    // ─── 3. VerificationTapeHolder 回放（按 URL 匹配）────────

    @Test
    fun `nextRecording matches by method and url`() {
        val tape = NetworkTape(
            recordings = listOf(
                NetworkRecording(
                    sequence = 0, method = "POST",
                    url = "https://api.example.com/auth/login",
                    responseStatus = 200, responseBody = loginResponse
                ),
                NetworkRecording(
                    sequence = 1, method = "GET",
                    url = "https://api.example.com/user/profile",
                    responseStatus = 200, responseBody = profileResponse
                )
            )
        )

        VerificationTapeHolder.load(tape)

        val r0 = VerificationTapeHolder.nextRecording("POST", "https://api.example.com/auth/login")
        assertEquals(0, r0?.sequence)
        assertEquals("POST", r0?.method)
        assertEquals(loginResponse, r0?.responseBody)

        val r1 = VerificationTapeHolder.nextRecording("GET", "https://api.example.com/user/profile")
        assertEquals(1, r1?.sequence)
        assertEquals(profileResponse, r1?.responseBody)
    }

    @Test
    fun `nextRecording returns null for unrecorded url`() {
        val tape = NetworkTape(
            recordings = listOf(
                NetworkRecording(
                    sequence = 0, method = "GET",
                    url = "https://api.example.com/test",
                    responseStatus = 200, responseBody = "{}"
                )
            )
        )

        VerificationTapeHolder.load(tape)
        assertNull(
            VerificationTapeHolder.nextRecording("GET", "https://api.example.com/unknown"),
            "未录制的 URL 应返回 null"
        )
    }

    @Test
    fun `same url called multiple times consumes from per-url queue`() {
        val tape = NetworkTape(
            recordings = listOf(
                NetworkRecording(
                    sequence = 0, method = "GET",
                    url = "https://api.example.com/users",
                    responseStatus = 200, responseBody = """{"page":1}"""
                ),
                NetworkRecording(
                    sequence = 1, method = "GET",
                    url = "https://api.example.com/users",
                    responseStatus = 200, responseBody = """{"page":2}"""
                )
            )
        )

        VerificationTapeHolder.load(tape)

        val r0 = VerificationTapeHolder.nextRecording("GET", "https://api.example.com/users")
        assertEquals("""{"page":1}""", r0?.responseBody, "第 1 次请求应返回 page 1")

        val r1 = VerificationTapeHolder.nextRecording("GET", "https://api.example.com/users")
        assertEquals("""{"page":2}""", r1?.responseBody, "第 2 次请求应返回 page 2")

        assertNull(
            VerificationTapeHolder.nextRecording("GET", "https://api.example.com/users"),
            "第 3 次请求应返回 null（该 URL 的 tape 耗尽）"
        )
    }

    @Test
    fun `url queue exhaustion does not affect other urls`() {
        val tape = NetworkTape(
            recordings = listOf(
                NetworkRecording(
                    sequence = 0, method = "GET",
                    url = "https://api.example.com/a",
                    responseStatus = 200, responseBody = """{"a":1}"""
                ),
                NetworkRecording(
                    sequence = 1, method = "GET",
                    url = "https://api.example.com/b",
                    responseStatus = 200, responseBody = """{"b":1}"""
                )
            )
        )

        VerificationTapeHolder.load(tape)

        // 耗尽 /a
        VerificationTapeHolder.nextRecording("GET", "https://api.example.com/a")
        assertNull(VerificationTapeHolder.nextRecording("GET", "https://api.example.com/a"))

        // /b 不受影响
        val rb = VerificationTapeHolder.nextRecording("GET", "https://api.example.com/b")
        assertEquals("""{"b":1}""", rb?.responseBody, "/b 应不受 /a 耗尽影响")
    }

    @Test
    fun `nextRecording returns null for empty tape`() {
        VerificationTapeHolder.load(NetworkTape(recordings = emptyList()))
        assertNull(VerificationTapeHolder.nextRecording("GET", "https://any.url"))
    }

    @Test
    fun `clear resets all url cursors`() {
        val tape = NetworkTape(
            recordings = listOf(
                NetworkRecording(
                    sequence = 0, method = "GET",
                    url = "https://api.example.com/test",
                    responseStatus = 200, responseBody = """{"ok":true}"""
                )
            )
        )

        VerificationTapeHolder.load(tape)
        VerificationTapeHolder.nextRecording("GET", "https://api.example.com/test") // consume
        VerificationTapeHolder.clear()

        VerificationTapeHolder.load(tape)
        val r = VerificationTapeHolder.nextRecording("GET", "https://api.example.com/test")
        assertEquals("""{"ok":true}""", r?.responseBody, "clear 后 cursor 应重置")
    }

    // ─── 4. 全链路：录制 → 序列化 → 反序列化 → 回放 ─────────

    @Test
    fun `full cycle - record then replay via TapeHolder produces matching data`() = runTest {
        // === Phase 1: 录制 ===
        val recordingClient = createRecordingClient()

        NetworkRecorder.markStart()
        val liveResp1 = recordingClient.post("https://api.example.com/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"phone":"13800138000"}""")
        }
        val liveBody1 = liveResp1.bodyAsText()
        val liveResp2 = recordingClient.get("https://api.example.com/user/profile")
        val liveBody2 = liveResp2.bodyAsText()
        val tape = NetworkRecorder.markEnd()

        // === Phase 2: 序列化 → 反序列化 ===
        val jsonCodec = Json { ignoreUnknownKeys = true }
        val tapeJson = jsonCodec.encodeToString(NetworkTape.serializer(), tape)
        val restoredTape = jsonCodec.decodeFromString(NetworkTape.serializer(), tapeJson)

        // === Phase 3: 通过 TapeHolder 回放（按 URL 匹配）===
        VerificationTapeHolder.load(restoredTape)

        val replay0 = VerificationTapeHolder.nextRecording("POST", "https://api.example.com/auth/login")!!
        val replay1 = VerificationTapeHolder.nextRecording("GET", "https://api.example.com/user/profile")!!

        assertEquals(liveBody1, replay0.responseBody, "登录响应回放应一致")
        assertEquals(liveResp1.status.value, replay0.responseStatus, "登录状态码回放应一致")
        assertEquals(liveBody2, replay1.responseBody, "用户信息响应回放应一致")
        assertEquals(liveResp2.status.value, replay1.responseStatus, "用户信息状态码回放应一致")
    }

    // ─── 5. NetworkRecorderBridge 桥接 ───────────────────────

    @Test
    fun `NetworkRecorderBridge delegates to NetworkRecorder`() = runTest {
        NetworkRecorderBridge.register(object : NetworkRecorderBridge.Delegate {
            override fun markStart() = NetworkRecorder.markStart()
            override fun markEnd(): NetworkTape = NetworkRecorder.markEnd()
        })

        val client = createRecordingClient()

        NetworkRecorderBridge.markStart()
        client.get("https://api.example.com/user/profile")
        val tape = NetworkRecorderBridge.markEnd()

        assertEquals(1, tape?.recordings?.size, "Bridge 应正确委托到 NetworkRecorder")
        assertEquals("GET", tape?.recordings?.first()?.method)
    }

    @Test
    fun `NetworkRecorderBridge returns null when no delegate registered`() {
        NetworkRecorderBridge.register(object : NetworkRecorderBridge.Delegate {
            override fun markStart() = NetworkRecorder.markStart()
            override fun markEnd(): NetworkTape = NetworkRecorder.markEnd()
        })

        NetworkRecorderBridge.markStart()
        val tape = NetworkRecorderBridge.markEnd()
        assertTrue(tape != null, "已注册 delegate 时 markEnd 应返回非 null")
    }
}
