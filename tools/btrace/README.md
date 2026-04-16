# btrace (RheaTrace) 3.0 使用指南

字节跳动开源的方法级性能追踪工具，基于 Perfetto，用于卡顿定位和启动分析。

## 前置条件

- 电脑已安装 adb、Java、Python3
- 手机通过 USB 连接，`adb devices` 可识别
- 注释 debugImplementation(libs.leakcanary)
- 注释 leakcanary.LeakCanary.config相关代码

## 1. 构建 btrace 版本

修改 `gradle.properties`：

```properties
enable.btrace=true
```

## 2. 抓取 trace

### 常用参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `-a <包名>` | 应用包名（必填） | — |
| `-t <秒>` | 采集时长 | 5 |
| `-o <路径>` | 输出文件路径 | — |
| `-m <路径>` | ProGuard mapping 文件（release 包需要） | — |
| `-s <序列号>` | 指定 adb 设备（多设备时） | — |
| `-r sched` | 自动重启应用，抓启动 trace | — |

### 常见场景

> mapping 文件存在时自动加 `-m` 反混淆（release 包需要）。mapping.txt 必须与安装的 APK 来自同一次构建，CI 产物需从对应 artifact 中获取。

抓 10 秒通用 trace：

> btrace SDK 的 trace server 仅在 app 启动时激活。直接对已运行的 app 抓通用 trace 会报 `wait for trace ready timeout`。需先激活再抓：

```bash
# 1. 激活 btrace 并重启 app（点一次即可）
adb shell setprop debug.rhea3.startWhenAppLaunch 1 && \
adb shell setprop debug.rhea3.waitTraceTimeout 20 && \
adb shell am force-stop com.example.archshowcase && \
adb shell am start -n com.example.archshowcase/.MainActivity -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
# 2.在 app 中执行要抓的操作，然后点击下方代码块抓 trace：
```

```bash
# 3. 抓 trace（不带 -r，不会再重启）
MAPPING=../../androidApp/build/outputs/mapping/release/mapping.txt
OUT="pbs/trace_$(date +%Y%m%d_%H%M%S).pb"
java -jar ./rhea-trace-shell.jar -a com.example.archshowcase -t 10 \
  ${MAPPING:+$([ -f "$MAPPING" ] && echo "-m $MAPPING")} \
  -o "$OUT"
[ -f "$MAPPING" ] && python3 deobfuscate-trace.py "$OUT" "$MAPPING"
```

抓冷启动：

```bash
MAPPING=../../androidApp/build/outputs/mapping/release/mapping.txt
OUT="pbs/startup_$(date +%Y%m%d_%H%M%S).pb"
java -jar ./rhea-trace-shell.jar -a com.example.archshowcase -t 10 \
  ${MAPPING:+$([ -f "$MAPPING" ] && echo "-m $MAPPING")} \
  -o "$OUT" -r sched
[ -f "$MAPPING" ] && python3 deobfuscate-trace.py "$OUT" "$MAPPING"
```


## 反混淆说明

rhea 的 `-m` 参数只反混淆 **btrace 插桩采集的方法**（`sampling-mapping.bin` 中的条目）。ART 运行时采样的方法（Perfetto CPU profiler / sched 模式）仍保留混淆名。

`deobfuscate-trace.py` 补充处理这部分：解析 mapping.txt 构建类名映射，直接替换 `.pb` 中 `track_event.name` 的混淆类名。同时解析方法级内联链，将合成类/协程 lambda 映射回源码位置。输出 `*_deobf.pb` 可拖入 ui.perfetto.dev 查看。

标注格式：`源码上下文 > 原始方法() [源文件:行号] | 完整运行时类名`

示例：
- `StressItem > blockingWork() [OBODemoContent.kt:257] | ...invokeSuspend(...)` — 同包内联
- `JDK8PlatformImplementations$getSystemClock$1 > now() [...:64] | ...ByteString$ArraysByteArrayCopier.g()` — R8 跨包 class merging

**局限**：R8 合成类（如 `ExternalSyntheticApiModelOutline`/`ExternalSyntheticLambda`）无原始映射，无法反混淆。

首次运行自动创建 venv（`/tmp/perfetto-env`）并安装 `perfetto`，无需手动配置。

## 3. 分析 trace

### AI 分析（推荐）

```
/analyze-trace
```

AI agent 自动解析 `.pb` 文件，结合 PERF 日志给出性能诊断报告。

依赖：`trace_processor`（本目录已包含，首次运行自动下载平台二进制）+ Python `perfetto` 包。

### 火焰图（可视化）

打开 https://ui.perfetto.dev ，拖入 `.pb` 文件。

## 4. 用完关闭

改回 `gradle.properties`：

```properties
enable.btrace=false
```

重新构建，APK 回到 noop 零开销状态。

## PERF 日志与 Trace 的对照分析

详见 [PERF_MONITOR.md — PERF 日志 ↔ Perfetto Trace 映射](../../docs/PERF_MONITOR.md#perf-日志--perfetto-trace-映射)。

## 工具版本

- `rhea-trace-shell.jar` — v3.0.0（本目录已包含）
- 更新：https://repo1.maven.org/maven2/com/bytedance/btrace/rhea-trace-processor/
- 项目主页：https://github.com/bytedance/btrace
