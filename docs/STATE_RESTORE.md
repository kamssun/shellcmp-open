# 交互回溯

支持记录用户操作，导出/导入后按时间线重放。

## 快速开始

### 1. 定义 HistoryType（历史类型）

```kotlin
import kotlinx.serialization.Serializable
import com.arkivanov.mvikotlin.core.utils.JvmSerializable

@Serializable
sealed interface MyHistoryType : JvmSerializable {
    @Serializable
    data class Toggle(val enabled: Boolean) : MyHistoryType
}
```

### 2. 定义 HistoryRecord（历史记录）

```kotlin
import com.example.archshowcase.core.trace.restore.appendHistory
import com.example.archshowcase.replayable.Replayable

@Replayable  // storeName 为空时从父类推导
@Serializable
data class MyHistoryRecord(
    val type: MyHistoryType,
    val timestamp: Long
) : JvmSerializable {

    fun applyToState(prevState: State): State = when (type) {
        is MyHistoryType.Toggle -> prevState.copy(
            isEnabled = type.enabled,
            history = prevState.appendHistory(this)
        )
    }

    fun toIntent(): Any = when (type) {
        is MyHistoryType.Toggle -> Intent.Toggle(type.enabled)
    }
}
```

> `applyToState()` 和 `toIntent()` 是手写成员函数，KSP 自动生成 FactoryHelpers 和 ExportStrategy
>
> `@Replayable` 支持 `@Repeatable`：同一 Record 可标注多个注解，为不同 Store 生成独立代码：
> ```kotlin
> @Replayable(stateClass = NavigationStore.State::class)
> @Replayable(stateClass = NavigationStore.State::class, storeName = "DemoNavigationStore")
> data class NavHistoryRecord(...)
> ```

### 3. State 添加 history

```kotlin
import com.example.archshowcase.core.trace.restore.AppendOnlyHistory
import com.example.archshowcase.core.trace.restore.ReplayableState

data class State(
    val isEnabled: Boolean = false,
    override val history: AppendOnlyHistory<MyHistoryRecord> = AppendOnlyHistory()
) : ReplayableState<MyHistoryRecord> {
    override fun hasValidData() = history.isNotEmpty()
    override fun createInitialState() = State()
}
```

### 4. 注册 Store

```kotlin
// Component 中
private val store = registerRestorableStore(
    name = "MyStore",
    factory = { storeFactory.create() }
)
```

**参数化页面**（同一 Store 类型、不同参数创建多实例）必须用实例化 store name：

```kotlin
// 每个实例独占 RestoreRegistry 槽位，避免状态互相覆盖
private val store = registerRestorableStore(
    name = "$BASE_STORE_NAME:$paramId",
    factory = { storeFactory.create(paramId) }
)
```

导出框架通过 `StoreExportRegistry.get()` 前缀匹配自动关联基础策略，无需额外注册。

完成！Store 的状态和历史会自动保存/恢复。

---

## 进阶

### 滚动位置恢复

**Component**：
```kotlin
private val scrollCoordinator: ScrollUpdateCoordinator by inject()

private val (store, scrollRestoreEvent) = registerScrollRestorableStore(
    name = "MyStore",
    factory = { storeFactory.create() },
    getItemCount = { store.state.items.size },
    isUserScrolling = scrollCoordinator::isUserScrolling
)

override fun updateScrollPosition(index: Int, offset: Int) {
    scrollCoordinator.runWithUserScroll {
        store.accept(Intent.UpdateScroll(index, offset))
    }
}
```

**Content**：
```kotlin
val listState = rememberLazyListState()

ScrollRestoreEffect(listState, component.scrollRestoreEvent)

OBOLaunchedEffect(listState) {
    snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
        .debounce(100)
        .collect { (index, offset) ->
            component.updateScrollPosition(index, offset)
        }
}

LazyColumn(state = listState) { ... }
```

> `ScrollUpdateCoordinator` 标记用户滚动，避免自动恢复和用户操作冲突

### 导出/导入

TimeTravel 悬浮窗提供：
- 导出 `.tte` 文件（包含所有 Store 状态和历史）
- 导入后自动重放所有操作
- 按时间线回溯任意时刻
- 回放时顶部显示当前 Intent 信息（`Store · Intent`）
- 面板支持拖动，自动约束在屏幕边界内

**导出策略**：

@Replayable 注解的 HistoryRecord 会由 KSP 自动生成 ExportStrategy 并注册，无需手写。

生成文件位于 `a-shared/build/generated/ksp/metadata/commonMain/`：
- `{Store}ExportStrategy.kt` — SimpleExportStrategy 实现
- `GeneratedExportStrategies.kt` — 自动注册所有策略

注册链路：
```
AppModule: StoreExportRegistry.setExternalRegistrar(::registerGeneratedExportStrategies)
    ↓
TimeTravelExporter.initStrategies(): StoreExportRegistry.runExternalRegistrar()
    ↓
GeneratedExportStrategies: register(ImageDemo/OBODemo/Settings/Navigation/DemoNavigation...ExportStrategy)
```

导出器位于 `a-core/src/jvmMain/.../export/TimeTravelExporter.jvm.kt`。

### 开发书签

开发深层链路时，一键保存当前界面状态，重启后自动恢复到该界面。

TimeTravel 悬浮窗中点击：
- **保存书签**（蓝色）— 导出当前 TTE 到 `Downloads/dev_bookmark.tte`
- **清除书签**（红色）— 删除文件，恢复默认启动

启动恢复流程：
```
App 启动 → 检测 dev_bookmark.tte
  → 存在且有效 → TteStateExtractor 提取状态 → RestoreRegistry 恢复 → 直达目标界面
  → 存在但失败 → 删文件 → Toast "书签已失效" → 正常启动
  → 不存在 → 正常启动
```

文件恢复后保留（不自动删除），支持多次重启反复恢复。VF 验证优先于书签恢复。

代码位于 `a-core/trace/bookmark/`（接口 + 恢复器）和 `androidApp/src/debug/bookmark/`（Android 实现）。

**手动策略**（仅用于非 @Replayable Store）：

```kotlin
object MyExportStrategy : SimpleExportStrategy<State, Record>(
    storeName = "MyStore",
    getHistory = { it.history },
    getTimestamp = { it.timestamp }
) {
    override fun createInitialState() = State()

    override fun processRecord(
        record: Record,
        prevState: State
    ): Triple<State, Any, Any?> {
        val newState = record.applyToState(prevState)
        val intent = record.toIntent()
        return Triple(newState, intent, null)
    }
}
```

---

## 工作原理

```
用户操作 → Store 更新 State → 添加 Record 到 history
                                        ↓
                                  RestoreRegistry 自动保存
                                        ↓
                                  重启/导入时恢复
                                        ↓
                            按 timestamp 排序回放 Record
```

**配置开关**：

整条链路由 `AppConfig.enableRestore` 控制，默认跟随 `isDebug()`。
关闭时 `RestoreRegistry.register()` 和 `InitialStateResolver.resolve()` 均为 no-op，零开销。
State→String 日志由 `AppConfig.enableTimeTravelLogging` 独立控制（默认 false），压测时关闭避免序列化开销。
`appendHistory()` 使用 `AppendOnlyHistory` 共享底座实现 O(1) 追加（替代 `history + record` 的 O(N) 拷贝），`enableRestore=false` 时返回空 `AppendOnlyHistory()`，Release 下 history 不增长。

**核心机制**：
- `registerRestorableStore()` 自动注册/注销
- `applyToState()` 和 `toIntent()` 为 HistoryRecord 上的手写成员函数
- `RestoreRegistry` 管理状态缓存
- `ExportStrategy` 负责序列化/反序列化

---

## 模块

- `ksp-annotations` - 注解定义 (`@Replayable`，支持 `@Repeatable` + `storeName`)
- `ksp-processor` - KSP 代码生成器（每个注解实例独立生成一套文件）
- `a-core/trace/` - 回溯基础设施
