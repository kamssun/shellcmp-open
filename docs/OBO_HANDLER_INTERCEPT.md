# OBO Handler 拦截

## 问题

三方 SDK 通过 `Handler.post()` 等方法直接往主线程 MessageQueue 灌消息，随着 SDK 数量增长，消息堆积导致 VSYNC / 触摸事件被延迟，造成卡顿。

## 方案

**编译期 ASM 字节码改写** — 在 Gradle Transform 阶段扫描三方 SDK 的 `.class` 文件，将 `Handler.post/send` 系列调用重写为 `OBOCompat` 静态方法，运行时统一收束到 OBO 调度器。同时注入调用方完整类名，供运行时诊断定位卡顿来源。

```
编译期：
  INVOKEVIRTUAL Handler.post(Runnable)
  → LDC "com.thirdparty.SomeClass"                    ← 编译期注入调用方类名
  → INVOKESTATIC OBOCompat.post(Handler, Runnable, String)

运行时：
  立即消息 → OBOScheduler.post(tag) { r.run() }
  延时消息 → handler.postDelayed({ OBOScheduler.post(tag) { r.run() } }, delay)
  定时消息 → handler.postAtTime({ OBOScheduler.post(tag) { r.run() } }, time)
  队首消息 → OBOScheduler.postFirst(tag) { r.run() }
```

核心原则：**定时交给系统 MessageQueue，执行交给 OBO。**

## 文件结构

| 文件 | 职责 |
|------|------|
| `build-plugin/src/.../OBOHandlerPlugin.kt` | 注册 ASM Transform + 独立报告 task |
| `build-plugin/src/.../OBOHandlerTransformFactory.kt` | ASM 工厂 + ClassVisitor + MethodVisitor + 类名注入 |
| `build-plugin/src/.../OBOHandlerConstants.kt` | 共享常量（SKIP_PREFIXES 黑名单 + TARGET_METHODS），Transform 与 Report task 共用 |
| `build-plugin/src/.../OBOInterceptReportTask.kt` | 独立 @CacheableTask，只读 ASM 扫描 runtime classpath JAR 生成拦截报告，与 Transform 缓存完全解耦 |
| `a-core/src/androidMain/.../OBOCompat.kt` | 运行时转发层，13 个 `@JvmStatic` 方法（均含 `tag: String` 参数） |
| `a-core/src/androidMain/.../OBODiagnostics.kt` | 运行时诊断：慢任务检测 + 累计耗时高频检测 + 队列深度检测 + 帧汇总补位 |

## 拦截的 Handler 方法（13 个）

| 类别 | 方法 |
|------|------|
| 立即 | `post`, `sendMessage`, `sendEmptyMessage` |
| 延时 | `postDelayed`(×2), `sendMessageDelayed`, `sendEmptyMessageDelayed` |
| 定时 | `postAtTime`(×2), `sendMessageAtTime`, `sendEmptyMessageAtTime` |
| 队首 | `postAtFrontOfQueue`, `sendMessageAtFrontOfQueue` |

## 黑名单（不拦截）

| 前缀 | 原因 |
|------|------|
| `com.example.archshowcase.` | 项目自身代码，用 Handler 有特殊原因 |
| `androidx.` | 框架基础设施（Compose 渲染管线、生命周期、动画） |
| `android.` | 系统框架 |
| `com.google.android.material.` | Material UI 组件 |
| `kotlinx.coroutines.` | 协程调度器（Dispatchers.Main），拦截会双重 OBO |
| `leakcanary.` / `curtains.` | Debug 工具 |
| `com.google.accompanist.` | Compose 周边库 |
| `com.arkivanov.` | Decompose / MVIKotlin 架构组件 |

新增三方 SDK 默认会被拦截。如需排除，在 `OBOHandlerConstants.SKIP_PREFIXES` 添加包前缀。

## 拦截报告

独立 Gradle task（`OBOInterceptReportTask`），只读扫描 runtime classpath JAR，与 ASM Transform 缓存解耦。支持 `@CacheableTask` 增量缓存。

```
androidApp/build/reports/obo-handler/{variant}-intercept.txt
```

格式：`类名 | 方法() → Handler.xxx ×次数`

## 来源标识（tag）

每个通过 OBO 调度的任务都携带来源标识：

| 来源 | tag 值 | 注入方式 |
|------|--------|---------|
| ASM 拦截的三方 SDK | 完整类名（如 `com.thirdparty.im.sdk.IMClient`） | 编译期 LDC 常量 |
| `oboLaunch` 协程 | 显式参数（如 `"LoginStore"`） | CoroutineName |
| `OBOLaunchedEffect` | debugTag 或默认 `"effect"` | CoroutineName |
| 直接 `OBOScheduler.post` | 显式参数（如 `"Application"`） | 直接传参 |

## 运行时诊断

`AppConfig.enableOBODiagnostics = true` 开启后，OBODiagnostics 追踪每个任务的执行耗时和来源。

### [OBO:SLOW] 慢任务

单个任务执行超过半帧（阈值跟设备刷新率挂钩）：

```
[OBO:SLOW] com.thirdparty.im.sdk.IMClient 12.3ms (threshold=8.3ms)
```

| 刷新率 | 阈值 |
|--------|------|
| 60Hz | 8.3ms |
| 90Hz | 5.6ms |
| 120Hz | 4.2ms |

### [OBO:SLOW] 队列深度

pending 任务数超过 HIGH_WATER_MARK (512) 时告警，表明入队速度持续大于消费速度：

```
[OBO:SLOW] queue depth 637 > 512 (HIGH_WATER_MARK)
```

### [OBO:FLOOD] 累计耗时高频

1s 窗口内某来源累计执行超过 100ms（帧预算的 10%），冷却期 5s：

```
[OBO:FLOOD] com.thirdparty.im.push.f 185.0ms/1000ms (threshold=100.0ms, count=92, cooldown=5s)
```

### JANK 日志 OBO 来源

开启诊断后，OBO 任务来源通过两种互斥方式出现在 `[PERF:JANK]` 日志中：

**1. 逐条慢消息（>3ms 的 OBO 任务独立成行）：**

```
[PERF:JANK] MODERATE 58ms (6 dropped) route=Main #3158
  ├─ OBOScheduler LoginStore 3ms
  ├─ OBOScheduler com.thirdparty.push.f 3ms
  ├─ OBOScheduler com.thirdparty.im.sdk.IMClient 51ms
  └─ cpu: 53ms/58ms (91%)
```

**2. 帧汇总补位（本帧无逐条 OBO 慢消息时，按耗时降序注入）：**

```
[PERF:JANK] MODERATE 50ms (5 dropped) route=Main #3897
  ├─ SomeHandler 50ms
  ├─ OBO 4.0ms [com.thirdparty.im.sdk.IMClient 2.0ms, com.thirdparty.im.report.t 2.0ms]
  └─ cpu: 55ms/55ms (100%)
```

补位场景：多来源短任务（每个 <3ms）被系统压力挤入同一卡顿帧，单独不可见但累计显著。

### 开关与开销

| 状态 | 额外开销 |
|------|---------|
| 关闭（默认） | processNext 多一次 boolean 读取 |
| 开启 | 每任务 2× nanoTime + HashMap 操作 |

## 全局开关

| 开关 | 作用 |
|------|------|
| `AppConfig.useOBOScheduler = false` | 关闭 OBO 调度，OBOCompat 透传原始 Handler 调用 |
| `AppConfig.enableOBODiagnostics = true` | 开启来源级诊断日志 |

## 安全保障

- `com.example.archshowcase.core.scheduler.*` 包内所有类（含 OBOCompat）被排除改写，避免递归
- 非主线程 Handler 直接透传
- 延时/定时消息的时序语义完整保留
