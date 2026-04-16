# 聊天压力测试

## Desktop 单测

零修改 ChatDao 源码，通过公共 API 外部施压。

### ChatDaoStressTest（正确性 + 性能）

```bash
./gradlew :a-shared:desktopTest --tests "com.example.archshowcase.chat.ChatDaoStressTest"
```

| 场景 | 验证内容 |
|------|---------|
| 写入吞吐 | insertMessage 500 条 > 500 msg/s |
| 批量插入 | insertMessages 1000 条单事务 |
| 排序正确性 | 100 会话快速更新后 DESC 排序 |
| 未读计数 | 10 会话 × 20 条，总未读一致 |
| clearUnread 重累加 | 清零后再灌入，计数准确 |
| 万级会话性能 | 10K 会话插入 < 5s |
| 消息摘要一致 | 51 条快速插入后 preview 正确 |

### ChatDaoWindowTest（窗口查询）

```bash
./gradlew :a-shared:desktopTest --tests "com.example.archshowcase.chat.ChatDaoWindowTest"
```

验证 `MessageWindow` + `WindowAnchor` 驱动的窗口化查询：

| 场景 | 验证内容 |
|------|---------|
| Latest 模式 | 查询最新 windowSize 条，hasMoreBefore/hasMoreAfter 边界 |
| At 模式 | 以 (timestamp, id) 为中心前后各 halfWindow 条 |
| Anchor 切换 | Latest ↔ At 切换后 Flow 重新发射 |
| 消息插入响应 | 新消息插入后窗口自动更新 |
| newMessageCount | newestSeenTs/newestSeenId 准确计数新消息 |
| 连续滑动 | 窗口连续移动时相邻窗口重叠验证 |

## 真机压测（端到端 UI + 异步 flush 链路）

通过 `MockChatRepository.STRESS_CONFIG` 在真机上注入消息，验证异步分片 flush、背压机制、UI 帧率。

### 使用方式

1. 改 `a-platform/.../chat/repository/MockChatRepository.kt` companion object：

```kotlin
private val STRESS_CONFIG: StressTestConfig? = StressTestConfig.messageStorm()
```

2. 关闭 `AppConfig.enableTimeTravelLogging`（State→String 序列化开销大，压测时导致严重卡顿）
3. Build & run 到真机，进入聊天 tab，3s 后自动开始
4. 看日志：`adb logcat -s StressTest MockChatRepo`
5. 压测结束后恢复 `STRESS_CONFIG = null` 和 `enableTimeTravelLogging`

### 预设场景

| 预设 | 参数 | 目标吞吐 |
|------|------|---------|
| `messageStorm()` | 500 群 200 活跃 2msg/s/群 30s | ~400 msg/s |
| `chatRoomFlood()` | 100 群 + g1 房间 50msg/s 20s | ~100 msg/s |
| `backpressure()` | 50 群各 100msg/s 10s | ~5000 msg/s |
| `fullBlast()` | 1000 群 300 活跃 + ChatRoom 60s | ~610 msg/s |

### 日志输出示例

```
StressTest ──── 6s Window ────────────────────────────
StressTest   ENQUEUE    [2000] avg=218μs P50=40μs P95=867μs max=16.0ms  total=2000
StressTest ═══════════ FINAL SUMMARY ═══════════════════════
StressTest   Duration:      31s
StressTest   Total enqueued: 12000
StressTest   Throughput:    385 msg/s
```

### 关键文件

| 文件 | 作用 |
|------|------|
| `chat/stress/StressTestConfig.kt` | 场景配置 + 4 个预设 |
| `chat/stress/StressTestLogger.kt` | 日志收集（ring buffer + P50/P95/P99） |
| `chat/mock/MockDataGenerator.kt` | `generateStressGroups()` 动态群生成 |
| `chat/repository/MockChatRepository.kt` | `startStressTest()` seed + 注入 + 日志 |
| `chat/local/CacheLock.kt` | expect/actual 缓存锁（线程安全 cache 更新） |
| `chat/model/MessageWindow.kt` | 消息窗口快照（anchor-driven windowed query） |
| `chat/model/WindowAnchor.kt` | 窗口锚点（Latest/At sealed interface） |
