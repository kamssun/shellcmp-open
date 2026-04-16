# ArchShowcase

KMP 项目 (Android + iOS + Desktop)，Compose Multiplatform 共享 UI。

## 架构心智模型

```
平台入口（androidApp / iosApp / desktopApp）
  └── a-shared（共享 UI + 业务，99% commonMain）
        ├── presentation/   Component + Content + Store（MVI）
        ├── di/             Koin 模块组装
        └── i18n/           StringProvider + tr()
  └── a-platform（SDK 桥接，expect/actual）
        ├── Auth / Pay / IM / RTC / Attribution / DeviceToken
        └── network/        Api / Repository / DTO / NetworkRecorder
  └── a-core（基础设施）
        ├── di/         DI 模块
        ├── scheduler/  OBO 调度器 + OBOCompat（Handler 拦截运行时）+ OBODiagnostics（来源级卡顿诊断）
        ├── perf/       性能监控（启动时间线 / 卡顿诊断 / 页面帧统计 / FrameMetrics）
        └── trace/      状态回溯 / 导出 / 泄漏审计 / 滚动恢复 / SSIM 截图验证
  └── build-plugin（Gradle 构建插件，ASM 字节码改写三方 SDK Handler 调用）
  └── macrobenchmark（Baseline Profile 生成 + 启动基准测试）
  └── tools/verify（VF AI 全自动截图回归：变更分析 → 路径推断 → adb 录制 → SSIM 验证）
```

数据流：Intent → Executor → Msg → Reducer → State（MVIKotlin）
导航：Decompose ChildStack，Route sealed interface，KSP 自动生成序列化

## 常用命令

```bash
./gradlew :a-shared:desktopTest                  # 快速单测（无需设备）
./gradlew koverVerify                            # 覆盖率 ≥80%
./gradlew :androidApp:assembleDebug              # 构建 Android
./gradlew :androidApp:verifyRoborazziDebug       # Preview 截图对比（JVM，无需设备）
tools/verify/verify-vf.sh                        # VF 端到端截图回归（真机，全量）
tools/verify/verify-vf.sh <name>                 # VF 端到端截图回归（真机，指定）
```

## 规范（渐进式加载）

`.claude/instincts/`（项目专属）为规范主目录。

### 通用规则 — 每次改动必读

| 文件 | 职责 |
|------|------|
| `forbidden.yaml` | 全局禁止事项 |
| `mvi.yaml` | MVI 状态管理（Reducer 纯函数） |
| `component.yaml` | Decompose Component 规范 |
| `commit-convention.yaml` | 提交消息格式 + pre-commit 验证流程 |
| `code-review.yaml` | **合规审查**：架构 / 性能 / UI / DI / 通用合规检查项 |
| `debugging.yaml` | **调试纪律**：日志先行 + logcat 分析方法，遇运行时问题必读 |

### 领域规则 — 碰到相关代码时加载

| 触发场景 | 文件 |
|----------|------|
| 写 UI / Compose | `ui-compose.yaml` + `i18n.yaml` |
| 改 App* 自定义组件 | `custom-components.yaml` |
| 改 DI / Koin | `koin-di.yaml` |
| 改路由 | `route.yaml` |
| 改列表/性能/Baseline Profile | `performance.yaml` |
| 写测试 | `testing.yaml` |
| 改状态回溯 | `time-travel.yaml` |
| 重构/代码风格 | `code-patterns.yaml` |
| 改网络拦截器 | `network-interceptor.yaml` |
| 分析 SDK | `sdk-source-analysis.yaml` |
| 创建/改 command/skill/agent | `command-skill-agent.yaml` |

### Instincts 编写规范

- 结构：`Action`（应该做）+ `Forbidden`（禁止做）
- 每个文件只关注一个领域
- 风格遵循文档维护原则（极简 + 渐进式披露）

## 深入文档 — 需要理解全貌时查阅

| 文档 | 何时读 |
|------|--------|
| `docs/codemaps/architecture.md` | 理解分层架构和技术栈 |
| `docs/codemaps/frontend.md` | 理解 Component/Content/Store 模式 |
| `docs/codemaps/data.md` | 理解数据层和网络层 |
| `docs/codemaps/backend.md` | 理解后端对接 |
| `docs/STATE_RESTORE.md` | 理解交互回溯系统 |
| `TESTING.md` | 完整测试命令手册（单测 / Kover / Roborazzi / VF / Maestro） |
| `docs/CROSS_PLATFORM_ANALYSIS.md` | 跨端方案选型论据 |
| `docs/PERF_MONITOR.md` | 运行时性能监控（PERF 日志体系 + Compose 帧渲染模型 + 排查流程） |
| `tools/btrace/README.md` | btrace 性能追踪 + trace 反混淆 + `/analyze-trace` AI 分析 |
| `macrobenchmark/README.md` | Baseline Profile 生成 + 启动基准测试 |
| `tools/verify/README.md` | VF 截图回归（录制 + 验证 + SSIM 引擎） |
| `docs/vf-recording-guide.md` | VF 录制 Agent 领域知识（Phase 1-3 规则） |
| `docs/OBO_HANDLER_INTERCEPT.md` | OBO Handler 拦截机制（ASM 改写原理 + 黑名单 + 报告查看） |

## 文档维护原则

所有文档（CLAUDE.md、instincts、docs/、MEMORY.md）遵循两条总纲：

### 极简

- 能用一句话说清的不用两句
- 禁止冗余描述、重复信息、装饰性文字
- instincts 禁止代码示例，仅用描述性语句
- 选对形式：单条规则用一句话，多项映射/查找用表格，索引优于全文

### 渐进式披露

信息按需分层，避免一次性灌入。

| 层 | 载体 | 加载时机 | 内容要求 |
|----|------|----------|----------|
| L0 | `CLAUDE.md` | 每次对话自动加载 | 架构心智模型 + 规范索引，不超过 200 行 |
| L1 | 通用 instincts | 每次写代码前 | 所有改动都必须遵守的规则 |
| L2 | 领域 instincts | 碰到相关代码时 | 特定领域的规则，按 trigger 字段匹配 |
| L3 | `docs/` | 需要理解全貌时 | 架构图谱、深度设计文档 |

**维护规则：**

- L0 只放索引和心智模型，细节下沉到 L1-L3
- L1/L2 每个文件聚焦单一领域，不交叉引用
- L3 文档可以详尽，但入口（L0）必须说明何时该读
- 新增规范先确定层级，再选择放置位置
- `MEMORY.md` 是草稿本，稳定知识应提炼到 yaml/docs 后从 MEMORY.md 移除
- 提炼标准：经多次会话验证、不再变化的模式或踩坑记录
