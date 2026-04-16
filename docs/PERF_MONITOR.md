# 性能监控日志指南

## 日志总览

| 标签 | 级别 | 触发时机 |
|------|------|----------|
| `[PERF:STARTUP]` | I | 冷启动完成 |
| `[PERF:JANK]` | D/W/E | 检测到掉帧（SLIGHT/MODERATE/SEVERE+） |
| `[PERF:PAGE]` | I | 页面离开时汇总 |
| `[PERF:PROFILE]` | I | Baseline Profile 编译状态 |
| `[PERF:GC_PRESSURE]` | W/E | GC 压力超过阈值（WARN/SEVERE/CRITICAL） |

## STARTUP — 启动时间线

树形输出，父子关系由 `StartupTracer.traced()` 嵌套决定。

```
[PERF:STARTUP] COLD 763ms
  ├─ process_fork 468ms (content_provider=24ms)
  ├─ app_create 146ms
  │  ├─ di_init 35ms
  │  └─ sdk_init 105ms
  │     ├─ sdk_auth 88ms
  │     ├─ sdk_device_token 3ms
  │     └─ sdk_im 14ms
  ├─ system_activity_launch 18ms
  ├─ activity_create 65ms
  │  ├─ root_component 55ms
  │  └─ compose_setup 10ms
  └─ first_frame 27ms
```

| 阶段 | 含义 |
|------|------|
| `process_fork` | 进程启动 → attachBaseContext 结束（含 `content_provider` 耗时） |
| `content_provider` | AndroidX Startup Initializers 总耗时（metadata 附在 process_fork 上） |
| `app_create` | Application.onCreate 业务逻辑 |
| `di_init` | Koin DI 初始化 |
| `sdk_init` | 三方 SDK 初始化合计 |
| `system_activity_launch` | Application.onCreate 结束 → Activity.onCreate 开始 |
| `activity_create` | Activity.onCreate（含 root_component + compose_setup） |
| `first_frame` | 首帧渲染 |

### 新增启动阶段

```kotlin
// Application.kt 或任意启动路径
StartupTracer.traced("my_phase") {
    // 业务代码
}
```

## JANK — 卡顿诊断

按严重程度着色：SLIGHT(蓝) / MODERATE(黄) / SEVERE+FROZEN(红)。

```
[PERF:JANK] SEVERE 108ms (11 dropped) route=ImageDemo #912 (+19)
  ├─ Choreographer.doFrame 107ms
  ├─ gc: 2x blocking 15ms, 3x concurrent 45ms
  ├─ cpu: 100ms/108ms (92%)
  └─ intents: Push(route=ImageDemo), LoadInitial
[PERF:JANK] #912 Choreographer.doFrame拆解: delay 5ms | anim(recompose→apply) 30ms | layout 40ms | draw(m&l→canvas) 25ms | gpu 5ms (total 108ms)
```

> `#912` = 帧序号（对齐 btrace），`(+19)` = 距上次 JANK 的帧间隔（btrace 可对齐）。
> doFrame 分解由 FrameMetrics 异步输出（GPU 完成后到达），紧跟 JANK 行。

### 各行含义

**Message 行**（`Choreographer.doFrame`、`OBOScheduler` 等）

帧间所有 >3ms 的 Looper Message，名称对齐 btrace 方法名。

| 常见 Message | 来源 |
|---|---|
| `Choreographer.doFrame` | 帧渲染回调（Compose recompose → layout → draw） |
| `AndroidUiDispatcher` | Compose 协程调度器（LaunchedEffect、StateFlow 收集等） |
| `OBOScheduler <tag> Xms` | OBO 调度器任务（开启 `enableOBODiagnostics` 后，tag 为完整来源标识，如 `OBOScheduler com.thirdparty.im.sdk.IMClient 30ms`） |
| `OBO Xms [...]` | 帧内 OBO 汇总补位：仅当本帧无逐条 OBO 慢消息时注入，按耗时降序展示各来源（如 `OBO 18.5ms [com.thirdparty.im.sdk.IMClient 12.0ms, LoginStore 6.0ms]`） |

### OBODiagnostics 三级检测

开启 `enableOBODiagnostics` 后，OBODiagnostics 运行三项独立检测：

| 检测 | 日志标签 | 阈值 | 说明 |
|------|----------|------|------|
| 慢任务 | `OBO:SLOW` | 单任务 > 半帧 | 逐条展示完整来源 tag |
| 洪峰 | `OBO:FLOOD` | 1s 窗口内同来源累计 > 100ms | 5s 冷却，识别高频调用源 |
| 队列深度 | `OBO:SLOW` | pending > 512 (HIGH_WATER_MARK) | 队列堆积预警 |

### OBOLaunchedEffect

`OBOLaunchedEffect` 使用 Compose dispatcher（而非 OBOScheduler.dispatcher），因为 `scrollToItem` 等帧耦合 API 依赖 `MonotonicFrameClock` 触发布局刷新。如果被 OBO 编排进队列，会导致帧信号与实际执行脱节、视觉不更新。

### Coil IO 并发限制

`ImageLoader` 配置 `fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(8))`，限制并发图片解码线程数，避免 IO 线程与主线程争抢 CPU 导致掉帧。

**FrameMetrics doFrame 分解**（异步输出，紧跟 JANK 行）

主线程检测到卡顿时标记 vsyncNs，FrameMetrics 在 GPU 完成后异步到达 bg thread，按 vsyncNs 精确匹配并输出。

| 字段 | FrameMetrics 常量 | 含义 |
|------|---|------|
| `delay` | UNKNOWN_DELAY | 帧排队等待（上一帧未完成/主线程忙） |
| `input` | INPUT_HANDLING | 触摸事件处理 |
| `anim(recompose→apply)` | ANIMATION | **performRecompose**（执行 @Composable 计算 UI 树差异）+ **applyChanges**（提交 diff 到 SlotTable/LayoutNode 树） |
| `layout` | LAYOUT_MEASURE | View measure + layout（**Compose 中通常 ≈0ms**，Compose 的 m&l 在 draw 阶段内完成） |
| `draw(m&l→canvas)` | DRAW | **measureAndLayout**（Compose 测量+布局）+ **LayoutNode.draw**（Canvas 录制） |
| `sync` | SYNC | 主线程 nativeSync（display list 提交到 RenderThread，主线程阻塞等待） |
| `gpu` | GPU (API 31+) | RenderThread DrawFrame |

**GC 行**

```
gc: 2x blocking 15ms, 3x concurrent 45ms
```

| 类型 | 含义 | 对卡顿的影响 |
|------|------|-------------|
| `blocking` | 阻塞式 GC，暂停所有线程 | **高**：时间直接计入卡顿 |
| `concurrent` | 并发 GC，后台线程执行 | **低**：争抢 CPU 但不暂停主线程 |

来源：`Debug.getRuntimeStat("art.gc.*")`

## GC_PRESSURE — GC 压力检测（Android）

5s 滑动窗口监控 blocking GC。分值 = (blockingGcTimeMs/sec) × (1 - freePercent/100)。开关：`AppConfig.enableGcPressureDetection`（默认 true），10s 冷却防刷屏。

```
[PERF:GC_PRESSURE] [GC_PRESSURE] SEVERE  pressure=72.3 | 5 GC 180ms / 5s | heap 128/256MB (50% free)
  PSS: Java 48MB  Native 32MB  Code 12MB  Stack 2MB  Graphics 8MB  System 6MB
  ChatRoomStore
  ├─ messageWindow.messages: 1200
  │  └─ body: 1200
  └─ history: 850
  ConversationListStore
  └─ conversations: 500
```

| 级别 | 阈值 | 含义 |
|------|------|------|
| WARN | ≥20 | 轻度压力 |
| SEVERE | ≥60 | 影响帧率 |
| CRITICAL | ≥150 | 可能触发 LMK |

报告含 PSS 分解（`Debug.MemoryInfo`）和 Store 集合大小快照（KSP `@MemoryTrackable` 生成，递归统计 Collection/Map `.size`）。

数据链路：`FrameDiagnosticsCollector` 每帧喂 GC 增量 → `GcPressureDetector` 计算分值 → `RootComponent.collectStoreMemorySnapshots()` 遍历 RestoreRegistry 提供快照。

**CPU 行**

```
cpu: 100ms/108ms (92%)
```

`主线程 CPU 时间 / 墙钟时间（利用率）`

| CPU% | 诊断 |
|------|------|
| >80% | 代码本身重（渲染复杂、计算密集） |
| <80% | 主线程被抢占（锁等待 / IO 阻塞 / GC 停顿 / CPU 竞争） |

来源：`SystemClock.currentThreadTimeMillis()` vs `elapsedRealtime()`

**intents 行**

最近的用户操作（MVI Intent），帮助还原卡顿发生时的操作上下文。

## PAGE — 页面帧统计

```
[PERF:PAGE] route=Home stay=4830ms fps=95.1 frames=232 dropped=55 jank={slight:1}
```

| 字段 | 含义 |
|------|------|
| `stay` | 页面停留时长 |
| `fps` | 平均帧率 |
| `frames` | 总帧数 |
| `dropped` | 总掉帧数 |
| `jank` | 各级别卡顿次数 |

## PERF 日志 ↔ Perfetto Trace 映射

结合 btrace 抓取的 Perfetto trace，可将 PERF 日志的聚合数据映射到方法级 slice。

### Compose 帧渲染模型

Compose 在 Choreographer 回调中的执行模型与传统 View 不同：

1. **ANIMATION callback**：`runRecomposeAndApplyChanges`（recompose + applyChanges），对应 `anim(recompose→apply)`
2. **TRAVERSAL callback**：`performDraw` 内 Compose 先 `measureAndLayout`（测量+布局），再 `LayoutNode.draw`（绘制），对应 `draw(m&l→canvas)`
3. **`layout` 在 Compose 中通常为 0** — Compose 不走 View 系统的 `onMeasure`/`onLayout`
4. **`measureAndLayout` 有两个出现位置**：ANIMATION callback 内（属于 anim）和 TRAVERSAL callback 内（属于 draw），分析 trace 时必须通过父链区分

### 示例：帧拆解日志 vs Trace

```
日志: delay 21ms | anim(recompose→apply) 69ms | draw(m&l→canvas) 18ms | gpu 1ms

Trace 对应:
doFrame 88ms
├─ CallbackRecord.run (ANIMATION)
│  └─ runRecomposeAndApplyChanges 69ms
│     ├─ performRecompose 55ms          ← 重组：执行 @Composable 计算 UI 树差异
│     └─ applyChanges 11ms             ← 提交 diff 到 SlotTable/LayoutNode 树
└─ TraversalRunnable (TRAVERSAL) ≈ draw 18ms
   └─ performDraw
      ├─ measureAndLayout 13ms          ← Compose 测量+布局
      └─ LayoutNode.draw ~5ms*          ← Canvas 录制
                                          *btrace 坍缩为 0ms，修正方法见下
+ RenderThread Running 1.4ms            ≈ gpu 1ms
```

### btrace 中的 draw 耗时推算

btrace 会将 `TraversalRunnable` 内的嵌套 slice 坍缩为相同 dur（`measureAndLayout` = `TraversalRunnable`），导致 LayoutNode.draw 显示 0ms。

**PERF draw ≈ TraversalRunnable**（已用 10 帧验证，9/10 误差 <3ms）。

btrace 坍缩时的修正方法——在 Perfetto 中对比两条轨道：

```
slice 轨道:    ┌── TraversalRunnable 12.6ms ──┐
thread_state:  ┌────────── Running ───────────────────┐
                                              ↑       ↑
                                         slice 结束  Running 结束
                                              └ 4.7ms ┘ ← 被吞掉的 LayoutNode.draw + nativeSync
```

slice 右边缘到 thread_state Running 右边缘的差，就是 btrace 吞掉的耗时。修正后 12.6 + 4.7 = 17.3ms ≈ PERF draw 18ms。

## 排查流程

```
1. 看 JANK 日志确定卡顿类型
   ├─ Choreographer.doFrame 慢 → 渲染问题 → 看 FrameMetrics 分解
   │  ├─ anim(recompose→apply) 大 → recompose 重 → 检查重组范围 / applyChanges 节点变更多
   │  ├─ layout 大 → View 系统布局复杂（Compose 中通常为 0）
   │  ├─ draw(m&l→canvas) 大 → measureAndLayout 或 Canvas 录制重 → 简化布局 / 减少绘制
   │  └─ delay 大 → 流水线阻塞 → 上一帧太慢导致排队
   ├─ 其他 Message 慢 → 主线程有耗时任务 → 移到后台线程
   ├─ gc blocking → GC 停顿 → 减少对象分配
   ├─ cpu% 低 → 主线程被阻塞 → 用 btrace 抓堆栈定位锁/IO
   └─ 无 msg + cpu% 低 → 线程调度延迟 / GC 并发期
2. 用 btrace 抓方法级耗时，定位具体函数
   Perfetto 中 Ctrl+F 搜 "PERF:JANK" 直接跳到卡顿帧（atrace 标记）
3. 修复后对比 JANK 日志验证
```

## 架构

```
Looper.Printer ──→ MessageMonitor ──→ slowMessages (帧间慢 Message)
                       ↓                   ↓
                   FrameDiagnosticsCollector  快照 vsyncNs（帧边界 swap 时）
                       ↓
                   GC delta + CPU/墙钟比 ──→ GcPressureDetector (5s 滑动窗口)

Choreographer.FrameCallback ──→ FrameMonitor ──→ JankTracker ──→ ContextCollector
                  ↓                  ↓                              ↓
          写入 vsyncNs          PageTracker            markPendingJank(vsyncNs)
                                     ↓                     ↓                ↓
                                reportPageMetrics   atrace 标记      reportJank → Logcat
                                                 "PERF:JANK #N"

Window.OnFrameMetricsAvailableListener ──→ FrameMetricsCollector (bg thread)
                                              ↓
                                  vsyncNs 匹配 pending → 异步输出 doFrame 分解
```
