# 架构概览

<!-- MANUAL: 本文件已精简，代码结构细节由 graphify 图谱提供。请勿自动补全目录树或接口签名。 -->

> 更新时间: 2026-04-13
> 代码结构细节请用 graphify MCP 查询（`query_graph` / `get_neighbors`）

## 技术栈

| 层级 | 技术 |
|------|------|
| UI | Compose Multiplatform |
| 导航 | Decompose 3.5.0 |
| 状态 | MVIKotlin 4.3.0 |
| DI | Koin 4.1.1 |
| 网络 | Ktor 3.4.1 |
| 图片 | Coil 3.4.0 |
| 持久化 | DataStore 1.2.0 |
| 数据库 | SQLDelight 2.3.2 |
| 代码生成 | KSP 2.3.6 |
| 国际化 | Compose Resources + StringProvider + tr() |
| 文件选择 | FileKit 0.13.0 (filekit-dialogs-compose) |
| 认证 SDK | Auth SDK (Google / Apple / Email login + token refresh) |
| 支付 SDK | Pay SDK (Google Pay / Custom Pay / Apple IAP) |
| IM SDK | IM SDK (chat room / DM / message) |
| RTC SDK | RTC SDK (Agora) |
| 风控 SDK | Risk SDK (device fingerprint + request signing) |
| 归因 SDK | Adjust 5.5.0 (Android) / ~5.0 (iOS pod) |
| 日志 | Kermit 2.1.0 + LogWriter 扩展 |
| 性能追踪 | btrace (RheaTrace) 3.0 |
| 启动优化 | Baseline Profile (benchmark 1.5.0-alpha03) |
| Handler 拦截 | build-plugin ASM Transform |
| 截图测试 | Roborazzi 1.59.0 |
| 覆盖率 | Kover 0.9.7 |

## 分层架构

```
┌─────────────────────────────────────┐
│  Android / iOS / Desktop 入口        │
├─────────────────────────────────────┤
│  MainView (KMP 入口)                 │
├─────────────────────────────────────┤
│  RootComponent (导航)                │
│  AuthObserverComponent (鉴权监听)     │
├─────────────────────────────────────┤
│  Feature Components                  │
│  (Login, Settings, Chat, Demo/*,  │
│   Live, Payment)                    │
├─────────────────────────────────────┤
│  Store (MVI 状态管理)                │
├─────────────────────────────────────┤
│  Service / Repository               │
├─────────────────────────────────────┤
│  API (Ktor) / DataStore / SQLDelight│
│  / SDK                              │
└─────────────────────────────────────┘
```

依赖方向：`a-shared → a-platform → a-core`（单向，编译器强制）

## MVI 数据流

```
Intent → Executor → Msg → Reducer → State
                              ↓
                           Label
```

## 模块职责

| 模块 | 职责 |
|------|------|
| a-core | 基础设施（trace, di, scheduler, perf, analytics, compose） |
| a-platform | SDK 桥接（auth, im, rtc, payment, network, attribution） |
| a-shared | 业务壳层（presentation, navigation, i18n, DI 组装） |
| build-plugin | Gradle ASM Transform（改写三方 SDK Handler 调用，收束到 OBO） |
| ksp-annotations | @Replayable, @CustomState, @MemoryTrackable, @RouteRegistry, @VfResolvable 等 |
| ksp-processor | KSP 处理器（EventMapper, ExportStrategy, SelectableState, ExposureParams 等） |
| macrobenchmark | Baseline Profile 生成 + 启动基准测试 |
| tools/verify | VF 截图回归（SSIM 引擎 + 录制/验证脚本） |

## 关键设计模式

### FeatureModule 按需加载
```kotlin
private val myFeature = featureModuleOf<MyStoreFactory> { singleOf(::MyStoreFactory) }
fun loadMyModule() = myFeature.load()
```

### StoreFactory 配置

| 模式 | StoreFactory | TimeTravelComponent |
|------|--------------|---------------------|
| enableRestore=true | TimeTravelStoreFactory | 创建（悬浮面板可见） |
| enableRestore=false | IntentTrackingStoreFactory + DefaultStoreFactory | null（零开销） |

### 平台桥接模式（iOS）

Bridge 接口在 `a-platform/src/iosMain/`，Swift 端通过 `BridgeRegistryKt.setXxxBridge()` 注册。

| Bridge | 说明 |
|--------|------|
| LoginBridge | 登录 SDK |
| ImBridge | IM SDK |
| RtcBridge | RTC 音视频 |
| UserBridge | 用户信息 |
| DeviceTokenBridge | 风控 SDK |
| PaymentBridge | 支付 SDK |
| AttributionBridge | 归因 SDK |

### expect/actual 服务模式

SDK 服务均为 expect/actual：Android 封装原生 SDK，iOS 委托 Bridge，Desktop 用 Mock。

### 状态恢复层次

- `RestorableState` — 基础可恢复
- `ReplayableState<R>` — 支持 History 回放（需 `history: AppendOnlyHistory<R>` 字段）
- `ScrollRestorableState` — 支持滚动位置恢复（需 `scrollPosition` 字段）

### KSP 代码生成

| 注解 | 生成产物 |
|------|----------|
| `@Replayable` | FactoryHelpers + ExportStrategy + GeneratedExportStrategies |
| `@CustomState` | StateFields（字段订阅）+ MemorySnapshot（内存快照）|
| `@VfResolvable` | IntentResolver（VF 回放）|
| `@RouteRegistry` | RouteSerializer（Route 序列化）|
| 无注解（自动扫描）| EventMapper（埋点映射）+ ExposureParams（曝光参数）|
