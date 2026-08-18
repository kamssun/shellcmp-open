# Macrobenchmark — Baseline Profile & 启动基准测试

## 前置条件

- `enable.btrace=false`（btrace 插桩会污染采集结果）
- 手机 USB 连接，`adb devices` 可识别
- **手机充电状态**，避免跑 benchmark 途中关机

## 1. 生成 Baseline / Startup Profile

采集启动路径的方法级 profile。Baseline Profile 由 ART 在安装时预编译，Startup Profile 由 R8 在构建时优化 DEX 布局。

```bash
# 推荐：收集 profile 到项目中（只跑已连接真机）
# 任务启动后在设备上输入账号和验证码，登录成功后自动继续采集。
./gradlew :androidApp:generateReleaseBaselineProfile

# 可选：如果要让脚本先填账号/验证码，可传入参数。
./gradlew :androidApp:generateReleaseBaselineProfile \
  -Pbenchmark.email=<account> \
  -Pbenchmark.verificationCode=<code>
```

产物位置：`androidApp/src/release/generated/baselineProfiles/`

- `baseline-prof.txt`：运行时 AOT 预编译规则
- `startup-prof.txt`：构建时 DEX layout 规则

### 关键踩坑

| 坑 | 说明 |
|----|------|
| **btrace 必须关** | `enable.btrace=true` 时构建的 APK 有插桩开销，profile 采集不准 |
| **发版入口用 androidApp task** | 推荐 `:androidApp:generateReleaseBaselineProfile`，会收集并复制到 app source set |
| **Generator 只跑真机** | 真实登录需要验证码，`baselineProfile` 只启用 connected device，不跑 managed device |
| **先真实登录再采集** | `BaselineProfileGenerator` 会先等真实登录拿 SDK token，再杀进程采集已登录冷启路径 |
| **profile 不会自动更新** | 代码改动后需手动重跑 Generator，否则新增的热点路径不会被覆盖 |
| **Startup Profile 必须显式开启** | `BaselineProfileRule.collect(..., includeInStartupProfile = true)` 才会生成 `startup-prof.txt` |

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

## 3. Release 性能门禁

```bash
# 快速静态配置检查
./gradlew verifyReleasePerformanceConfig

# 发版前完整检查：assembleRelease + bundleRelease + R8 metadata
./gradlew verifyReleasePerformance
```

完整门禁会检查：

- release R8 full mode / shrink / obfuscation / optimization / repackage / optimized resource shrinking
- APK 是否携带 `.dm` Baseline Profile
- AAB 的 `BUNDLE-METADATA/com.android.tools/r8.json` 是否至少有一个 DEX 为 `"startup": true`

如果只看到 `"startup": false`，说明 Startup Profile 没有真正参与 DEX layout，需要重新生成并提交 `startup-prof.txt`。

## 4. 扩大 Profile 覆盖面

当前 `BaselineProfileGenerator` 先准备真实登录态，再杀进程覆盖登录后的冷启动路径（`startActivityAndWait`）。可补充关键用户路径：

```kotlin
rule.collect(
    packageName = "com.example.archshowcase",
    includeInStartupProfile = true,
) {
    RealLoginPreparer.prepare(this)
    killProcess()
    startActivityAndWait()
    RealLoginPreparer.requireMainScreen()
}
```

启动链路保持 `includeInStartupProfile = true`；首页滚动、常用子页面、返回等非启动链路建议另建测试且不放入 Startup Profile，避免 primary dex 过大。
