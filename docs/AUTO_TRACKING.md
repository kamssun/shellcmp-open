# 全自动埋点

## 架构

```
用户操作 → InteractionContext 标记 → Store.accept(Intent)
                                         ↓
                                   TrackingExecutor 拦截
                                         ↓
                              KSP EventMapper（类型匹配）
                                         ↓
                              RuntimeSanitizer 脱敏
                                         ↓
                              AnalyticsCollector 上报
```

四类事件全自动：Page（路由）、Intent（交互）、Exposure（曝光）、前后台。

## Intent 采集规则

**核心原则：所有用户交互入口必须经过 `appClickable` 系列，禁止裸用 Compose 原生交互 API。**

| 场景 | 使用 | 替代 |
|------|------|------|
| 点击 | `Modifier.appClickable(component, onClick)` | ~~`.clickable`~~ |
| 长按/双击 | `Modifier.appCombinedClickable(component, onClick, onLongClick)` | ~~`.combinedClickable`~~ |
| 键盘 IME | `appKeyboardActions(component, onSend, onDone, ...)` | ~~`KeyboardActions`~~ |
| 开关 | `AppSwitch`（内置追踪） | - |
| 筛选标签 | `AppFilterChip`（内置追踪） | - |
| 按钮 | `AppButton` / `AppTextButton` / `AppOutlinedButton` / `AppIconButton`（内置追踪） | - |

### appClickable 参数

```kotlin
Modifier.appClickable(
    component = "ChatInputBar",        // 组件标识，用于日志
    gestureType = GestureType.TAP,     // TAP / LONG_PRESS / SWITCH / CHIP / IME_ACTION
    enabled = true,
    interactionSource = interactionSource, // 可选，需要按压动画时传入
    role = Role.Switch,                // 可选，语义化角色
    onClick = { ... },
)
```

### 不需要追踪的交互

- 纯 UI 效果（滚动、拖拽、动画触发）不触发 Store Intent，不需要 `appClickable`
- 这类场景可以继续用原生 `.clickable`

## Exposure 曝光采集

**ExposureLazyColumn 自动追踪所有带 key + contentType 的 item，含 KSP 自动提取的业务参数，零手动代码。**

```kotlin
ExposureLazyColumn(
    listId = "image_list",
    state = listState,
) {
    items(
        items = images,
        key = { it.id },              // 作为 exposureKey
        contentType = { "image_card" } // 作为 componentType
    ) { image ->
        ImageCard(image)               // 无需加任何 Modifier
    }
}
```

### 自动追踪规则

| 条件 | 行为 |
|------|------|
| 有 key + 有 contentType | 自动注入 `trackExposure`，exposureKey = `{contentType}_{key}`，params = KSP 提取 |
| 缺 key 或缺 contentType | 跳过，不追踪 |

### 业务参数自动提取

ExposureLazyListScope 的成员 `items()` 保留 `List<T>` 引用，在自动追踪时调用 `ExposureParamsRegistry.extract(item)` 提取业务参数。

KSP `ExposureParamsProcessor` 无注解全量扫描 `com.example.archshowcase.*` 下所有 data class，生成 `toExposureParams()` 扩展（复用 ParamsCodeGenerator 脱敏规则）。

### 曝光触发条件

- 可见面积 ≥ 50%
- 停留时间 ≥ 500ms
- 同一页面生命周期内去重（同 key 只上报一次）

## KSP 自动生成

### EventMapper（Intent → TrackingEvent）

- 自动扫描所有 `Store` 接口的 `sealed interface Intent`
- 生成 `{StoreName}EventMapper.kt` + `EventMapperRegistry.kt`
- Registry 按 Intent 类型匹配（`when (intent)`），不依赖 storeName 字符串
- 命名：`CamelCase` → `snake_case`（如 `SendText` → `send_text`）

### 敏感字段脱敏（三层）

| 层 | 机制 | 示例 |
|----|------|------|
| KSP 编译期 | 字段名模式匹配 | `text` → `[len=N]`，`email` → `sha256 前 8 位`，`url` → 排除 |
| RuntimeSanitizer | 值正则检测 | `13812345678` → `138****5678`，`a@b.com` → `a***@b.com` |
| 长度截断 | >200 字符 | `aaaa...` → `aaaa...[truncated]` |

KSP 敏感字段规则：

| 策略 | 匹配字段名 |
|------|-----------|
| EXCLUDE（排除） | url, link, uri, token, password, secret, credential, apikey, secretkey, authkey, privatekey, accesskey, auth, code, otp, pin, captcha |
| LENGTH_ONLY（仅长度） | text, content, body, message |
| HASH（SHA256 前 8 位） | email, mail, phone, mobile, tel |

## Debug 验证

```bash
# logcat 过滤
adb logcat -s Analytics:D

# 输出格式
Intent | {route} | {storeName}.{intent_name} | {gestureType} | {params}
Page | {route} | {action} | from={from} | nav={nav} | {duration}ms
Exposure | {route} | {componentType}:{exposureKey} | list={listId} | {dwell}ms | {params}
```

Debug 包 `batchSize=1`，事件实时输出。
