# VF 录制领域知识

供 AI Agent 读取的 VF 操作指南。人工查阅请看 `tools/verify/README.md`。

## 设计原则

- **录制不改 APP 主链路** — 录制只观察不干预，APP 行为必须与真实使用一致
- **每个 Intent 至少被一个 VF 覆盖** — 一个模块多条路径 = 多个 VF
- **delay 覆盖真实耗时** — 网络重试、超时等不可跳过，delay 不够就加大

## VF 目录结构

```
<vf>/
├── manifest.json          # 场景元数据 + Intent 序列 + covers
├── start.tte              # TTE-A（初始状态，可为空）
├── end.tte                # TTE-B（期望最终状态）
├── start_baseline.png     # 录制起点截图
├── end_baseline.png       # 录制终点截图
├── network_tape.json      # （可选）网络录放数据
└── output/                # 验证输出（自动生成）
```

存放路径：`tools/verify/test-vfs/<功能域>/.../<路径描述>/manifest.json`
叶子目录（含 `manifest.json`）= 一个 VF，中间目录仅分组。

manifest.json 核心字段：`name`, `intents[]`(store/intentType/params/delayAfterMs), `screenshotCompare`(threshold/mask_regions), `networkTape`(enabled), `covers[]`

---

## Phase 1: 变更分析

### 判断运行模式

检查 `tools/verify/test-vfs/` 是否存在且有 VF 子目录。

- **首次运行**（目录不存在或为空）：跳过 git diff，扫描所有 `presentation/**/*Store.kt`、`*Content.kt`、`*Component.kt`，全部标记为 `uncovered_new`
- **增量运行**：走 git diff 流程

### 获取并过滤变更文件

```bash
git diff --name-only HEAD
git diff --cached --name-only
git ls-files --others --exclude-standard
```

合并去重后，按模式分类：

| 模式 | 分类 |
|------|------|
| `*Store.kt`, `*StoreFactory.kt`, `*Executor*` | 交互逻辑 |
| `*Component.kt` | 生命周期 |
| `*Content.kt` | UI 布局 |
| `*Route.kt` | 导航结构 |
| `*Theme*`, `*Color*`, `*.xml`, `i18n/*` | 纯视觉 |

无 UI 相关文件变更 → 提示 "无 UI 变更，无需更新 VF" 并结束。

### 匹配现有 VF

从文件路径提取功能模块名，扫描 `tools/verify/test-vfs/*/manifest.json` 的 `covers` 字段，分三组：

| 分组 | 条件 | 动作 |
|------|------|------|
| **matched_baseline_only** | covers 命中 + 只有纯视觉变更 | 只需重录截图 |
| **matched_full_rerecord** | covers 命中 + 交互逻辑变更 | AI 重新分析 |
| **uncovered_new** | 无任何 VF 覆盖 | AI 推断新路径 |

### 覆盖度检查

对 uncovered_new 和 matched_full_rerecord 的模块，列出 Store 的**所有 Intent 子类**，与现有 VF 的 intents 交叉比对。标记未被任何 VF 覆盖的 Intent。

---

## Phase 2: 规划交互路径

对每个需要录制的功能模块，读取 Store/Content/Component 源码，规划人工操作步骤。

### 读取源码

1. **Store.kt** — 提取 `sealed interface Intent` 的所有子类，了解有哪些操作
2. **Content.kt** — 找到 UI 中的按钮、列表、输入框等可交互元素
3. **Component.kt**（按需）— 确认页面入口和导航关系

### 枚举交互路径

每个功能模块枚举**所有典型交互路径**，每条路径 = 一个独立 VF：

1. **主流程**（必录）— 用户最常用的正向路径
2. **分支操作**（必录）— 每个独立功能至少被一条路径覆盖
3. **异常路径**（涉及网络时必录）— 网络错误、空状态、超时
4. **组合交互**（按需）— 多个操作联动的场景

### 输出操作计划

每个 VF 生成人工操作步骤描述（不是 Intent JSON），例如：
1. 从首页点击「图片列表」进入 ImageDemo
2. 等待图片加载完成
3. 向下滚动触发 LoadMore
4. 继续滚动查看更多图片

---

## Phase 3: 操作手机录制

通过 adb-mcp 操作手机，配合 TimeTravel 浮窗录制。

### 录制循环

每个 VF 执行：
1. 打开 app → 导航到目标页面
2. 点击浮窗「录制开始」
3. 按操作计划执行交互（tap/scroll/输入）
4. 点击浮窗「录制结束」
5. 从设备 pull VF 包到本地

### 设备端路径

VF 导出路径：`/sdcard/Android/data/com.example.archshowcase/files/vf/`

### 本地目标路径

```
tools/verify/test-vfs/<功能域>/<场景名>/
```

### 注意事项

- 需要设备连接（真机或模拟器），App 已安装 debug 版本
- 每次操作前用 capture_ui_dump 获取最新 UI 树，不要盲目点击坐标
- 滚动后等待内容加载再继续
- 涉及网络请求的 VF 如果不带 network_tape，回放时可能因网络差异导致 SSIM 不过
