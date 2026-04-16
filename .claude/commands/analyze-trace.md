# Trace 性能分析

分析 btrace/Perfetto trace 文件，结合 PERF 日志，在主对话中持续诊断优化。

## 输入收集

1. **trace 文件** — 用户提供 `.pb` 路径，或扫描 `tools/btrace/pbs/` 取最新
2. **PERF 日志**（可选）— 用户粘贴的 `[PERF:*]` logcat 日志
3. **关注点**（可选）— 启动 / 页面卡顿 / 特定路由

## 环境准备

确认 perfetto Python 环境可用，不可用则安装：

```bash
# venv + perfetto
python3 -m venv /tmp/perfetto-env
source /tmp/perfetto-env/bin/activate && pip install perfetto pandas

# trace_processor bootstrapper 已在 tools/btrace/trace_processor
```

## 分析流程

用 Python `perfetto` 库加载 trace，通过 SQL 逐步提取数据。**不要一次跑完所有查询**，按用户关注点逐步深入。

### 查询策略（避免弯路）

1. **进程有多个 pid** — 进程可能被 fork 多次，始终加 `p.pid = <pid>` 过滤，从进程列表中选 pid 最大的那个（应用本身而非 fork）
2. **Top Slices 会被框架壳层淹没** — `ZygoteInit.main`、`Looper.loop`、`Handler.dispatchMessage` 等壳层 slice 的 dur 等于整个进程生命周期。第一轮直接看 doFrame 帧列表，不要查 Top Slices
3. **帧内展开：直接查自耗时 Top N，不要逐层钻取**
   - 逐层 depth 递增展开容易陷入框架壳层（depth 9→13→16→22→...→116），越钻越深反而丢失全局视角
   - 正确做法：**一次查帧内所有 slice 按 dur DESC 排序，取 Top 20**，过滤掉纯壳层（`Looper`/`Handler`/`doCallbacks`/`CallbackRecord`）
   - 关注 **自耗时**（self time）而非包含子调用的总耗时：壳层 slice 的 dur 等于其子调用之和，无分析价值
   - 自耗时查询：`s.dur - COALESCE((SELECT SUM(c.dur) FROM slice c WHERE c.parent_id = s.id), 0)` 或用 Perfetto 的 `self_dur` 列（如可用）
4. **时间戳对齐：trace 帧 ↔ PERF 日志**
   - trace 的 ts 是 monotonic clock（纳秒），PERF 日志是 wall clock — 不能直接比较绝对值
   - 用 **PERF:JANK 标记**（atrace instant event）在 trace 中找到帧号，再与 PERF 日志的帧号匹配
   - 用标记确认某帧渲染的是哪个路由，**不要从 Composable 名字推断路由**（`DemoRootContent` 是路由容器，不是特定页面）
5. **容器 Composable ≠ 页面内容** — `RootContent`、`DemoRootContent`、`Children` 等是路由容器/栈管理器，它们的耗时包含了当前活跃路由的渲染，但不能反推具体是哪个页面。必须结合 PERF 日志的 intent 或 atrace 标记确认
6. **第一轮概览合并查询** — 线程负载、卡顿帧列表、自定义标记三个查询放同一个 Python 脚本执行，减少 TraceProcessor 启动开销
7. **监控代码本身可能是瓶颈** — Choreographer 回调中的 PerfMonitor/JankTracker/LogReporter 也会出现在 trace 中。分析帧内 app 代码时，注意区分业务代码和监控代码的耗时
8. **120Hz 设备帧预算 8.3ms** — 卡顿阈值不能硬编码 16ms，应使用 PERF:STARTUP 中的 refreshRate 计算实际帧预算
9. **环境准备必须先行** — 先执行"环境准备"章节安装 perfetto Python 包，不要直接跳到查询。`trace_processor` CLI 的查询参数是 `-Q`（不是 `--query`），但优先用 Python `perfetto` 库而非 CLI
10. **同名线程有多个 utid** — 同进程内可能存在多个同名线程（如两个 `RenderThread`，utid 不同）。查 `thread_state` 时先确认 utid 来源：从目标 `thread_state` 行的 utid 反查，不要从 thread 表按名字查后假设只有一个
11. **btrace slice 时间戳坍缩** — btrace 方法级采样可能将嵌套 slice 坍缩为相同的 ts/end_ts（如 `dispatchDraw` 和 `measureAndLayout` 显示完全相同的 dur）。遇到这种情况，用 **thread_state 间隙法**替代：看主线程从 slice 报告结束到实际进入 Sleep 的时间差
12. **PERF draw ≈ TraversalRunnable**（主线程，不含 RenderThread）— btrace 会将 TraversalRunnable 内的 m&l 和 LayoutNode.draw 坍缩为相同 dur。修正方法：看主线程 thread_state 在 TraversalRunnable 报告结束后的 Running 延续时间。注意：`measureAndLayout` 可能出现在 ANIMATION callback（`Recomposer:animation` 内）或 TRAVERSAL callback 内，只有后者属于 draw 阶段

### 第一轮：概览（单次脚本执行）

1. 线程负载 Top 15（按总 slice 耗时排序）
2. 卡顿帧列表（Choreographer.doFrame > 16ms，按 dur DESC）
3. PERF 自定义 atrace 标记（`PERF:%` / `Startup%`）

向用户展示概览 + 初步判断（瓶颈线程、最严重帧、阶段分布），询问要深入哪个方向。

### 按需深入

用户追问时，执行对应查询：

- **某帧细节** → 帧内 slice 按 dur DESC Top 20（过滤壳层），重点看自耗时高的 slice
- **启动阶段** → 首帧 performTraversals 内的 attach/measure/layout/draw 拆解
- **某线程** → 该线程的 Top slices（按 name GROUP BY 聚合）
- **关联 PERF 日志** → 用 PERF:JANK 标记的帧号匹配 trace 中的 instant event

### 结合代码验证

trace 定位到热点后，**必须读源码确认根因再给结论**，禁止仅凭 trace 数据推测优化方案：

1. **读代码** — 根据 trace 中的类名/方法名找到对应源文件，理解实际实现
2. **区分框架开销与应用代码** — trace 中大量耗时可能来自 Compose/Android 框架内部（measureAndLayout、display list 录制），不能归因于应用代码
3. **验证假设** — 如果怀疑某个 Modifier/Composable 是瓶颈，先确认它的参数、缓存策略、是否已有优化（如固定尺寸、remember）
4. **给出分层结论**：
   - 哪些是应用代码可优化的（附具体文件和行号）
   - 哪些是框架固有开销（说明为什么不可避免，以及是否可通过架构调整规避）

### SQL 模板

```python
from perfetto.trace_processor import TraceProcessor, TraceProcessorConfig

config = TraceProcessorConfig(bin_path="tools/btrace/trace_processor")
tp = TraceProcessor(file_path="<trace.pb>", config=config)

# === 概览查询（单次执行） ===

# 线程负载
tp.query("""
SELECT t.name, SUM(s.dur)/1e6 as total_ms, COUNT(*) as cnt
FROM slice s JOIN thread_track tt ON s.track_id = tt.id
JOIN thread t ON tt.utid = t.utid JOIN process p ON t.upid = p.upid
WHERE p.name = '<proc>' AND p.pid = <pid> AND s.dur > 0
GROUP BY t.name ORDER BY total_ms DESC LIMIT 15
""")

# 卡顿帧
tp.query("""
SELECT s.id, s.ts, s.dur/1e6 as dur_ms
FROM slice s JOIN thread_track tt ON s.track_id = tt.id
JOIN thread t ON tt.utid = t.utid JOIN process p ON t.upid = p.upid
WHERE p.name = '<proc>' AND p.pid = <pid> AND t.is_main_thread = 1
  AND s.name LIKE '%Choreographer%doFrame%' AND s.dur > 16000000
ORDER BY s.dur DESC
""")

# 自定义标记
tp.query("""
SELECT s.name, s.dur/1e6 as dur_ms, s.ts
FROM slice s JOIN thread_track tt ON s.track_id = tt.id
JOIN thread t ON tt.utid = t.utid JOIN process p ON t.upid = p.upid
WHERE p.name = '<proc>' AND p.pid = <pid>
  AND (s.name LIKE 'PERF:%' OR s.name LIKE 'Startup%')
ORDER BY s.ts LIMIT 30
""")

# === 帧细节查询：帧内 Top N（过滤壳层） ===

# 帧内所有 slice 按 dur 排序，过滤框架壳层
tp.query("""
SELECT s.name, s.dur/1e6 as dur_ms, s.depth, (s.ts - <frame_ts>)/1e6 as offset_ms
FROM slice s JOIN thread_track tt ON s.track_id = tt.id
JOIN thread t ON tt.utid = t.utid JOIN process p ON t.upid = p.upid
WHERE p.name = '<proc>' AND p.pid = <pid> AND t.is_main_thread = 1
  AND s.ts >= <frame_ts> AND s.ts + s.dur <= <frame_ts> + <frame_dur>
  AND s.dur > 5000000
  AND s.name NOT LIKE '%Looper%' AND s.name NOT LIKE '%Zygote%'
  AND s.name NOT LIKE '%Handler.dispatch%' AND s.name NOT LIKE '%Handler.handle%'
  AND s.name NOT LIKE '%CallbackRecord.run%' AND s.name NOT LIKE '%doCallbacks%'
  AND s.name NOT LIKE '%MethodAndArgsCaller%' AND s.name NOT LIKE '%Method.invoke%'
ORDER BY s.dur DESC LIMIT 25
""")

# 查帧内 app 代码（example.archshowcase 包名）
tp.query("""
SELECT s.name, s.dur/1e6 as dur_ms, s.depth, (s.ts - <frame_ts>)/1e6 as offset_ms
FROM slice s JOIN thread_track tt ON s.track_id = tt.id
JOIN thread t ON tt.utid = t.utid JOIN process p ON t.upid = p.upid
WHERE p.name = '<proc>' AND p.pid = <pid> AND t.is_main_thread = 1
  AND s.ts >= <frame_ts> AND s.ts + s.dur <= <frame_ts> + <frame_dur>
  AND s.name LIKE '%example.archshowcase%' AND s.dur > 1000000
ORDER BY s.dur DESC LIMIT 20
""")

# === 后台线程查询（GROUP BY 聚合） ===
tp.query("""
SELECT s.name, SUM(s.dur)/1e6 as total_ms, COUNT(*) as cnt
FROM slice s JOIN thread_track tt ON s.track_id = tt.id
JOIN thread t ON tt.utid = t.utid JOIN process p ON t.upid = p.upid
WHERE p.name = '<proc>' AND p.pid = <pid>
  AND t.name LIKE '<thread_pattern>%' AND s.dur > 0
GROUP BY s.name ORDER BY total_ms DESC LIMIT 15
""")
```

## 输出规范

- 每轮分析后给出**发现 + 建议**，不要只贴原始数据
- 卡顿级别：SLIGHT (<50ms) / MODERATE (50-100ms) / SEVERE (>100ms)
- 优化建议标注优先级 P0/P1/P2，并附源码位置
- 优化建议必须区分"应用代码可改"和"框架固有开销"
- 主动提示用户可以追问的方向（"要看启动细节？" "展开这个帧？"）

## 优化闭环

- 优化后必须同场景重新录制 PERF 日志 + trace，用前后数据对比验证效果
- 验证有效的优化手段，固化到对应 instinct 规则中（如 `performance.yaml`）；无效则 revert 改动
- 分析过程中的踩坑经验（查询策略、误判模式等），更新到本文件的"查询策略"章节
