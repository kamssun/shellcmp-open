# VF 截图回归

## 前置依赖

- `adb` (Android SDK Platform-Tools)
- `jq` (JSON 处理)
- `java` (JDK 21+, 用于 SSIM 截图对比)
- `bc` (数学计算, macOS/Linux 自带)

## 构建

```bash
./gradlew :tools:verify:screenshot-compare:jar
```

## 录制

两种方式：

### 方式一：手动录制（App 内 TT 浮窗）

App 内 TimeTravel 浮窗 → 「录制开始」→ 操作 → 「录制结束」→ 保存 zip。

### 方式二：AI 自动录制（/update-vf）

AI 通过 adb-mcp 操作手机模拟人工交互，配合 TimeTravel 浮窗录制。详见 `.claude/agents/vf-recorder.md`。

### 方式三：adb 广播录制

无法通过浮窗录制按钮时（如回溯模式下按钮不可见），通过 adb broadcast 触发录制：

```bash
# 开始录制
adb shell am broadcast -a com.example.archshowcase.VF_RECORD_START -n com.example.archshowcase/com.example.archshowcase.verification.RecordReceiver
```

```bash
# 结束录制（建议加上 --es verification_text "描述"）
adb shell am broadcast -a com.example.archshowcase.VF_RECORD_END -n com.example.archshowcase/com.example.archshowcase.verification.RecordReceiver --es verification_text "描述"
```

产物保存到 `/sdcard/Download/vf/vf_<timestamp>/`

## 验证

```bash
# 单个 VF（目录含 manifest.json）
./tools/verify/run_verification.sh tools/verify/test-vfs/image_demo_scroll

# 批量（目录含多个 VF 子目录）
./tools/verify/run_verification.sh tools/verify/test-vfs/

# 指定设备
./tools/verify/run_verification.sh tools/verify/test-vfs/ emulator-5554
```

判断规则：
- `<dir>/manifest.json` 存在 → 单个验证
- 不存在 → 扫描子目录，逐个验证后汇总

## VF 目录结构

按功能域自由嵌套，深度不限。叶子目录（含 `manifest.json`）即为一个 VF，中间目录仅用于分组。验证脚本递归查找所有 `manifest.json`（排除 `output/`）。

单个 VF 目录内容：
```
<vf>/
├── manifest.json       # 场景描述 + Intent 序列 + covers + 验证配置
├── start.tte           # TTE-A (初始状态，首页录制时可能为空)
├── end.tte             # TTE-B (期望最终状态)
├── start_baseline.png  # 录制时的开始截图
├── end_baseline.png    # 录制时的结束截图
├── network_tape.json   # (可选) 网络请求录制
└── output/             # 验证产出（自动生成）
```

manifest.json 关键字段：
- `intents` — Intent 序列（store + intent_type + params + delay_after_ms）
- `covers` — 该 VF 覆盖的 Store/Component 列表（用于 /update-vf 关联 git diff）
- `screenshot_compare` — SSIM 配置（`ssim_threshold` 阈值默认 0.95，`mask_regions` 忽略区域）。对比行为由 `baseline.png` 文件是否存在决定
- `network_tape` — 是否启用网络录制回放

## 验证流程

| 步骤 | 动作 |
|------|------|
| 0 | start.tte 为空时 force-stop App 回到 Home |
| 1 | Push VF 到设备 |
| 2 | `VERIFY_INIT` → 恢复初始状态 → Activity recreate |
| 3 | 截图 A'（初始状态）|
| 4 | 逐个 `VERIFY_DISPATCH` Intent |
| 5 | 截图 B'（最终状态）|
| 6 | SSIM 截图对比（start_baseline vs A'，end_baseline vs B'）|
| 7 | 生成统一报告 |

## 输出

### 单个用例 → `<vf_dir>/output/`

| 文件 | 内容 |
|------|------|
| `verify_a.png` | 初始状态截图 |
| `verify_b.png` | 最终状态截图 |
| `result_ssim_start.json` | Start SSIM 对比结果 |
| `result_ssim_end.json` | End SSIM 对比结果 |
| `diff_heatmap_start.png` | Start 差异热力图 |
| `diff_heatmap_end.png` | End 差异热力图 |
| `report.json` | 统一报告 (JSON) |
| `report.md` | 统一报告 (Markdown) |

### 批量 → `<dir>/batch_output/`

| 文件 | 内容 |
|------|------|
| `batch_report.json` | 汇总（total/passed/failed + 各用例报告）|
| `batch_report.md` | Markdown 汇总表格 |

## 退出码

| 脚本 | 码 | 含义 |
|------|-----|------|
| run_verification.sh | 0 | 全部 PASS |
| | 1 | 有 FAIL |
| | 2 | 无用例 / 参数错误 |

## 设计原则

### 录制不改 APP 主链路

录制只**观察**，不**干预**。录制期间 APP 行为必须与用户真实使用完全一致（网络重试、超时、错误处理等不可跳过或缩短）。

验证模式可以偏离主链路（禁用重试、使用 tape 回放），因为验证的目标是确定性复现，不是模拟真实请求。

## KSP 自动生成

Store 加 `@VfResolvable` 注解后，KSP 自动生成 Intent 解析代码（`GeneratedIntentResolverRegistry`），无需手写映射。新增 Store 只需加一行注解。

## Gradle 集成

```bash
./gradlew runVerification -Pavf.dir=tools/verify/test-vfs/image_demo_scroll
```
