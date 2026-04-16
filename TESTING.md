# 测试命令

## 单元测试

| 命令 | 说明 |
|------|------|
| `./gradlew :a-core:allTests` | Core 模块测试 |
| `./gradlew :a-platform:allTests` | Platform 模块测试 |
| `./gradlew :a-shared:allTests` | Shared 模块测试 |
| `./gradlew :a-core:desktopTest` | Core Desktop 测试 |
| `./gradlew :a-shared:desktopTest` | Shared Desktop 测试 |
| `./gradlew :a-shared:desktopTest --tests "*.ChatDaoStressTest"` | Chat 压力测试 |
| `./gradlew :a-shared:desktopTest --tests "*.ChatDaoWindowTest"` | Chat 窗口查询测试 |

## 覆盖率

| 命令 | 说明 |
|------|------|
| `./gradlew koverHtmlReport` | 全模块聚合覆盖率报告 |
| `./gradlew koverLog` | 全模块覆盖率摘要 (控制台) |
| `./gradlew koverVerify` | 全模块覆盖率验证 (≥80%，LINE，JaCoCo 引擎) |

## 视觉回归（Roborazzi + Preview Scanner）

通过 `generateComposePreviewRobolectricTests` 自动扫描 `com.example.archshowcase` 包下的 `@Preview` 函数生成截图测试，无需手写测试类。新增 Preview 即自动纳入覆盖。`private` Preview 会被排除。

| 命令 | 说明 |
|------|------|
| `./gradlew :androidApp:recordRoborazziDebug` | 录制基准截图 |
| `./gradlew :androidApp:verifyRoborazziDebug` | 验证截图差异 |

## UI 测试

| 命令 | 说明 |
|------|------|
| `./gradlew :androidApp:connectedAndroidTest` | Android Instrumented 测试 (需设备) |

## VF 截图回归

录制 → ADB 回放 → SSIM 截图对比。

| 命令 | 说明 |
|------|------|
| `./gradlew verifyVf` | 全量 VF 验证 |
| `./gradlew verifyVf -Pvf.dir=<path>` | 指定目录验证 |
| `./gradlew cleanVerification` | 清除验证产物（`clean` 时自动触发）|
| `./gradlew :tools:verify:screenshot-compare:jar` | 构建截图对比工具 |
| `/update-vf` | AI 操作手机 + TimeTravel 浮窗录制 |

录制方式：
- **手动**：App 内 TT 浮窗 → 「录制开始」→ 操作 → 「录制结束」→ 保存 zip
- **AI 自动**：`/update-vf` 通过 adb-mcp 操作手机模拟人工交互

输出：`<vf_dir>/output/`（verify_a/b.png, result_ssim_start/end.json, diff_heatmap_start/end.png, report.md）

详见 `tools/verify/README.md`

## 聊天压力测试

详见 `docs/stress-test/chat.md`。Desktop 单测（ChatDao 正确性）+ 真机 4 场景预设（异步 flush + UI 帧率）。

## E2E 测试 (Maestro)

iOS:
```bash
maestro test .maestro/flows/ -e APP_ID=com.example.archshowcase.ArchShowcase
```

Android:
```bash
maestro test .maestro/flows/ -e APP_ID=com.example.archshowcase
```

> 多设备同时连接时需指定 `--udid`，或只保留一个设备
