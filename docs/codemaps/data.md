# 数据模型

<!-- MANUAL: 本文件已精简，模型定义和接口签名由源码 + graphify 图谱提供。请勿自动补全。 -->

> 更新时间: 2026-04-13
> DTO/模型的完整字段定义请直接读源码，graphify MCP 可查询关联关系

## 序列化约定

- 网络 DTO: kotlinx.serialization (`@Serializable`)
- TimeTravel State: JvmSerializable
- History Record: kotlinx.serialization + JvmSerializable

## 关键模型设计决策

### Chat 窗口化模型
消息列表采用 anchor-driven windowed query（`WindowAnchor` + `MessageWindow`），替代全量加载。支持粘底滚动 + 定位跳转。

### ChatDao
SQLDelight DAO，手动通知 + 写入分片。平台 DatabaseDriverFactory：Android 用 AndroidSqliteDriver，Desktop 用 JdbcSqliteDriver (in-memory)，iOS 用 NativeSqliteDriver。

### CacheLock
expect/actual 缓存锁：Android 用 synchronized，Desktop/iOS 用 Mutex。

### 压力测试
4 个预设场景：messageStorm (200群×2msg/s)、chatRoomFlood (50msg/s单群)、backpressure (5000msg/s)、fullBlast (综合极限)。

### 回溯接口层次

```
RestorableState                    — hasValidData()
  └── ReplayableState<R>           — history: AppendOnlyHistory<R>, createInitialState()
  └── ScrollRestorableState        — scrollPosition: ScrollPosition
```

History Record 手写 `applyToState()` 和 `toIntent()` 实现回放。
