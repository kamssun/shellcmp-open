# Macrobenchmark — Baseline Profile & 启动基准测试

## 前置条件

- `enable.btrace=false`（btrace 插桩会污染采集结果）
- 手机 USB 连接，`adb devices` 可识别
- **手机充电状态**，避免跑 benchmark 途中关机

## 1. 生成 Baseline Profile

采集应用热点路径的方法级 profile，安装时 ART 预编译这些方法（AOT），减少运行时 JIT。

```bash
# 在真机上跑 Generator（~2min）
./gradlew :macrobenchmark:connectedNonMinifiedReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.archshowcase.macrobenchmark.BaselineProfileGenerator

# 收集 profile 到项目中（会同时跑 managed device，~2min）
./gradlew :macrobenchmark:collectNonMinifiedReleaseBaselineProfile
```

产物位置：`androidApp/src/release/generated/baselineProfiles/baseline-prof.txt`

### 关键踩坑

| 坑 | 说明 |
|----|------|
| **btrace 必须关** | `enable.btrace=true` 时构建的 APK 有插桩开销，profile 采集不准 |
| **task 在 macrobenchmark 模块** | `collect*` task 在 `:macrobenchmark` 而非 `:androidApp` |
| **Generator 只跑真机** | `connectedNonMinifiedReleaseAndroidTest`，不是 `pixel6Api33DebugAndroidTest`（不存在） |
| **profile 不会自动更新** | 代码改动后需手动重跑 Generator，否则新增的热点路径不会被覆盖 |

### 何时需要重新生成

- 新增/改动首页 Composable 或导航路由
- 升级 Compose / Kotlin 版本
- DI 模块结构变化
- 发版前（建议纳入 CI）

## 2. 启动基准测试

对比有无 Baseline Profile 的冷启动时间，量化优化效果。

```bash
# 跑全部测试（NoCompilation + BaselineProfile，各 5 次冷启动，~3min）
./gradlew :macrobenchmark:connectedBenchmarkReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.archshowcase.macrobenchmark.StartupBenchmark
```

单独跑某个：

```bash
# 仅 BaselineProfile
./gradlew :macrobenchmark:connectedBenchmarkReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.archshowcase.macrobenchmark.StartupBenchmark#startupBaselineProfile

# 仅 NoCompilation
./gradlew :macrobenchmark:connectedBenchmarkReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.archshowcase.macrobenchmark.StartupBenchmark#startupNoCompilation
```

### 结果查看

- JSON 数据：`macrobenchmark/build/outputs/connected_android_test_additional_output/benchmarkRelease/connected/<device>/*.json`
- Perfetto traces：同目录下 `*.perfetto-trace`，可拖入 ui.perfetto.dev
- 关键指标：`timeToInitialDisplayMs`（TTID，系统级冷启动耗时）

### 指标说明

| 指标 | 含义 | 对应 |
|------|------|------|
| TTID（macrobenchmark） | `am start` → 首帧渲染完成 | 包含 zygote fork、进程调度 |
| PERF:STARTUP（应用内） | `Application.onCreate` → first_frame | 不含进程创建 |

两者差值 ≈ process_fork + 系统调度开销（通常 100-200ms）。

## 3. 扩大 Profile 覆盖面

当前 `BaselineProfileGenerator` 只覆盖启动路径（`startActivityAndWait`）。可补充关键用户路径：

```kotlin
rule.collect(packageName = "com.example.archshowcase") {
    pressHome()
    startActivityAndWait()
    // TODO: 补充以下交互
    // - 首页列表滚动
    // - 进入常用子页面
    // - 返回操作
}
```

覆盖越多 → 更多方法被 AOT 预编译 → 首次交互更流畅。
