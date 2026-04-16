# 前端结构

<!-- MANUAL: 本文件已精简，目录树和组件细节由 graphify 图谱提供。请勿自动补全。 -->

> 更新时间: 2026-04-13
> 目录树和具体文件请用 graphify MCP 查询

## Component 模式

每个 feature 包含：
- `XxxComponent.kt` — 接口 + 实现（持有 Store、处理导航）
- `XxxContent.kt` — Composable UI
- `XxxStore.kt` — MVI Store (可选)
- `XxxStoreFactory.kt` — Store 工厂 (可选)
- `XxxHistory.kt` — 回溯类型 (可选，与 Store 配对)
- `XxxModule.kt` — DI 模块 (可选)

## 导航

- **AppComponentContext**: 扩展 `ComponentContext`，携带 `navigator: Navigator`
- **NavigationStackManager**: 实现 `Navigator`，封装 Store↔StackNavigation 双向同步 + 防重入
- RootComponent / DemoRootComponent 均委托给 NavigationStackManager
- DemoRootComponent 持有独立的 DemoNavigationStore，与 Root 隔离

## 鉴权流程

```
App 启动 → AuthObserverComponent 监听 authState
  ├─ LoggedOut → 导航到 Login
  ├─ LoggedIn(token) → fetchProfile → 导航到 Home
  └─ ForcedLogout(reason) → 导航到 Login
```

`AppConfig.skipLogin = true` 时跳过登录（开发调试用）

## State 订阅

Store.State 标注 `@CustomState` 后 KSP 生成字段订阅代码。Content 中用 `val state = component.state.rememberFields()` 一行替代所有手写 `selectAsState`。

KSP 自动选择比较策略：原始类型用 `selectAsState`（equals），引用类型用 `selectRefAsState`（===）。`ReplayableState`/`ScrollRestorableState` 字段自动排除。

## Component 注册

```kotlin
// 普通 Store（带回溯）
private val store = registerRestorableStore(name = STORE_NAME, factory = { factory.create() })

// 滚动恢复 Store
private val (store, scrollRestoreEvent) = registerScrollRestorableStore(
    name = STORE_NAME, factory = { factory.create() },
    getItemCount = { store.state.items.size },
    isUserScrolling = scrollCoordinator::isUserScrolling
)

// 参数化页面（多实例）— 用 "BaseName:$param" 避免状态互相覆盖
```

## UI 规范

- 自定义设计系统: AppTheme + App* 组件（基于 compose-foundation，已移除 Material3）
- 主题: `AppTheme.colors` / `AppTheme.typography` / `AppTheme.shapes`
- 网络图片: AsyncImage (Coil)，本地图片: painterResource
- 导航: navigator.push/pop
- Effect: OBOLaunchedEffect
- 文本: tr() 国际化

## Feature 对比

| Feature | Store | History | ScrollRestore |
|---------|:-----:|:-------:|:-------------:|
| Login | Y | - | - |
| ConversationList | Y | Y | - |
| ChatRoom | Y | Y | - |
| MainTab | Y | Y | - |
| Settings | Y | Y | - |
| Navigation | Y | Y | - |
| ImageDemo | Y | Y | Y |
| OBODemo | Y | Y | Y |
| NetworkDemo | Y | Y | - |
| Live / Payment | - | - | - |
