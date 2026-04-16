# 跨端方案对比：KMP+CMP vs 单平台先行+AI 复刻

> 基于 archshowcase 项目实际架构 + 2025-2026 行业数据的综合分析。

---

## 一、核心结论

KMP+CMP 是 Google、Apple、JetBrains 三大厂商正在收敛的方向，有 7 年生产验证；"AI 复刻"目前零成功案例，本质上是用不确定的 AI 能力替代确定的编译器保证。

---

## 二、五维对比

| 维度 | KMP 栈方案 (Compose+Decompose) | AI 复刻方案 (Swift -> Kotlin) |
|------|-------------------------------|------------------------------|
| 开发速度 | 前期慢 (搭架构) -> 后期极快 | 前期快 (原生开发) -> 后期慢 (重复搬砖) |
| 稳定性 | 极高 (逻辑共用，不存在同步 Bug) | 中 (AI 翻译可能遗漏逻辑细节) |
| 团队要求 | 需要精通 Kotlin 且对 iOS 有一定了解 | 需要两名或全栈开发者分别精通 Swift 和 Kotlin |
| 交互体验 | 95% 接近原生 (Compose iOS 已 Stable) | 100% 原生体验 |
| 适用场景 | 业务逻辑复杂、需要长期迭代的工具/社交类 | 交互极炫酷、依赖大量系统底层 API、且有充足人力的项目 |

---

## 三、archshowcase 项目实测数据

| 指标 | 数据 |
|------|------|
| commonMain 共享率 | **>90%** — UI、导航、状态管理、网络、DI 全部共享 |
| 共享代码行数 | ~8,200 行 commonMain / ~13,400 行总计 (61%) |
| a-shared 共享率 | **99%** — 4,458 行中仅 40 行为 iosMain |
| iOS Swift 代码量 | ~1,004 行（仅 SDK 桥接），ContentView.swift 仅 22 行 |
| expect/actual 数量 | 8 个服务（Auth、Pay、IM、RTC 等 SDK 桥接） |
| iOS 集成方式 | `MainViewController()` -> SwiftUI 薄壳，iOS 零 UI 代码 |
| 新增页面成本 | 只改 Kotlin，三端（Android/iOS/Desktop）同时获得 |

**关键事实：** 如果走 AI 复刻方案，需要从零重写 8,200+ 行共享业务逻辑到 Swift，再加上完全不同的 iOS SDK 集成、导航体系、状态管理。

---

## 四、详细对比

| 维度 | KMP+CMP | 单平台+AI 复刻 |
|------|---------|----------------|
| **代码源** | 单一源（1 份代码 -> N 端） | 双源（2 份独立代码库） |
| **新功能成本** | 1x（写一次） | 1.3x~2x（写一次 + AI 翻译 + 人工校验 + 测试） |
| **bug 修复** | 修一次，全端生效 | 修两次，或 AI 翻译后再验证 |
| **一致性保证** | 编译器保证 | 人工保证（或 AI 保证，但不确定） |
| **长期维护** | 线性增长 | 指数分叉风险 |
| **团队结构** | 统一 Kotlin 团队 | 需要 Android + iOS 两端能力 |
| **AI 的角色** | 辅助开发效率（写代码、补知识） | 承担核心翻译职责（风险集中） |
| **行业验证** | Google Docs、Cash App 等 7+ 年 | 零成功案例 |
| **技术趋势** | Google/Apple/JetBrains 三方收敛 | 逆趋势 |

---

## 五、KMP+CMP 方案优势

### 5.1 生态已成熟

- KMP 2023.11 Stable，CMP for iOS 2025.5 Stable
- Google Docs 已在 iOS 端生产运行 KMP，A/B 测试一年，性能对齐甚至优于 Objective-C（快操作 15-20% 提升）
- Cash App 自 2018 年跑 KMP 至今 7+ 年，处理金融核心逻辑

### 5.2 行业大规模验证

| 公司 | 场景 | 细节 |
|------|------|------|
| Google Workspace | Google Docs iOS 版 | A/B 测试一年，性能与 Obj-C 持平甚至更好，crash 率无差异 |
| Cash App | 金融核心功能 | 自 2018 年使用，美国排名第一的金融 app |
| McDonald's | 全 app | 月处理 650 万笔交易，超过 1 亿下载量 |
| Netflix | Prodicle 制片管理 | 第一家公开采用 KMP 的 FAANG |
| Duolingo | 逐步扩展到 KMP-first | 每个新模块都增强信心 |
| X/Twitter | 三平台共享 | KotlinConf 2025 演讲 |

### 5.3 中国公司采用

| 公司 | 产品 | 使用方式 |
|------|------|---------|
| 百度 | Wonder App | 数据层起步，逐步扩展到多个核心业务模块 |
| B站 | Bilibili 国内版 | IM 模块使用 KMP + CMP |
| 快手 | 快影 App（Top 3 视频编辑） | KMP 两年，共享视频编辑器核心业务逻辑 |
| 海尔 | IoT/智能家居 app | JetBrains 官方收录 |

### 5.4 市场份额与增速

| 框架 | 市场份额 | 增速 |
|------|---------|------|
| Flutter | ~46% | 稳定领先 |
| React Native | ~35-42% | 略降 |
| **KMP** | **~18-23%** | **18 个月从 7% -> 23%，增速最快** |

### 5.5 三方收敛趋势

- **Google** — 官方推荐 KMP，Jetpack Room/DataStore/ViewModel 已支持 KMP
- **JetBrains** — 全力投入 CMP for iOS Stable、Swift Export、KMP IDE
- **Apple** — WWDC 2025 发布 Swift for Android，定位完全一致：共享业务逻辑 + 原生 UI

Apple 自己做跨端，说明平台厂商也认可"共享逻辑层"这个方向。

---

## 六、"AI 复刻"方案的现实

### 6.1 零成功案例

截至 2026 年，没有任何公开的案例研究证明 AI 能端到端自动翻译一个完整 Android 应用到 iOS。所有 AI 翻译工具（Workik、CodeConvert.ai、Kotlift）仅适用于代码片段级别。

### 6.2 语言翻译 ≠ 应用翻译

Kotlin -> Swift 的语法转换只是冰山一角。真正的难点在于：

- **平台 API 差异** — Android Context vs iOS AppDelegate
- **UI 范式差异** — Jetpack Compose vs SwiftUI，状态管理模式完全不同
- **生命周期差异** — Activity/Fragment vs UIViewController
- **权限模型差异** — Android 运行时权限 vs iOS Info.plist 声明
- **SDK 集成** — 支付、推送、登录等第三方 SDK 在两端是完全不同的库

### 6.3 持续成本问题

即使首次 AI 翻译能达到 50-90% 的代码相似度（理想情况），后续问题更严重：

- 每次原端改动，复刻端也要改 -> 持续的双倍维护成本
- AI 翻译的代码质量不确定 -> "你不知道你不知道什么"
- 两端代码逐渐分叉 -> bug 修复不同步 -> 用户体验分裂

### 6.4 AI 信任度数据

- Stack Overflow 2025 Survey：只有 **29%** 认为 AI 能处理复杂任务（比 2024 年的 35% 下降）
- **46%** 的开发者对 AI 代码准确性不信任
- HackerNews 社区："你可以这样构建东西，短期也许能工作，但你不知道你不知道的东西"

### 6.5 最讽刺的事实

目前用 AI 做跨端最成功的案例，恰恰是用 Cursor + Claude **辅助 KMP 开发**，而不是替代 KMP。AI 和 KMP 是互补关系，不是替代关系。

---

## 七、对常见反对论点的回应

### "AI 进步很快，很快就能自动翻译整个 App"

AI 进步的方向是辅助开发者，不是替代架构决策。即使 AI 能翻译语法，平台 SDK 绑定、UI 交互范式、性能调优仍需人工。而且 AI 越强，用 AI + KMP 的效率提升也同步放大。

### "原生开发体验更好，性能更高"

Google Docs 用 KMP 在 iOS 上 A/B 测试一年，性能与 Objective-C 持平甚至更好。CMP 渲染的是原生 UI 组件，不是 WebView 或自绘引擎。

### "团队中有 iOS 开发者，不想学 Kotlin"

Apple 自己在 WWDC 2025 推 Swift for Android，说明平台厂商也认为"只会一门语言"不可持续。Kotlin 和 Swift 语法高度相似，学习曲线实测 2-4 周。

### "先做一端可以更快验证产品"

这是唯一有部分道理的论点。但 archshowcase 当前架构已证明 KMP 并不拖慢单端开发速度（新页面只改 Kotlin），且同时获得三端产出。一次开发三端部署反而更快。

---

## 八、KMP 已知痛点（诚实面对）

| 痛点 | 现状 | 缓解措施 |
|------|------|----------|
| iOS 构建慢 | 未优化可达 20 分钟 | 静态 framework + 只构建所需架构 + cache，可优化到 < 2 分钟 |
| iOS 调试体验 | 比 Android 复杂 | JetBrains 在持续改进，日志追踪 + 模拟器测试可覆盖 |
| Obj-C 桥接类型丢失 | Flow -> Any?，泛型丢失 | Swift Export（实验中）将彻底解决，SKIE 工具可临时缓解 |
| 学习曲线 | 需要了解 Gradle/KMP 工具链 | AI 工具可大幅降低入门门槛（Cursor + Claude 案例） |

---

## 九、数据来源

- [Android Developers Blog: KMP at Google I/O & KotlinConf 2025](https://android-developers.googleblog.com/2025/05/android-kotlin-multiplatform-google-io-kotlinconf-2025.html)
- [JetBrains: Industry Leaders at KotlinConf 2025](https://blog.jetbrains.com/kotlin/2025/12/industry-leaders-on-the-kotlinconf25-stage/)
- [JetBrains: CMP 1.8.0 Released — iOS Stable](https://blog.jetbrains.com/kotlin/2025/05/compose-multiplatform-1-8-0-released-compose-multiplatform-for-ios-is-stable-and-production-ready/)
- [JetBrains: KMP Roadmap August 2025](https://blog.jetbrains.com/kotlin/2025/08/kmp-roadmap-aug-2025/)
- [InfoQ: Apple Swift SDK for Android](https://www.infoq.com/news/2025/10/swift-sdk-android/)
- [KMPShip: Big Companies Using KMP 2025](https://www.kmpship.app/blog/big-companies-kotlin-multiplatform-2025)
- [Netguru: Top Apps Built with KMP](https://www.netguru.com/blog/top-apps-built-with-kotlin-multiplatform)
- [Kuaiying KMP Case Study](https://medium.com/@xiang.j9501/case-studies-kuaiying-kotlin-multiplatform-mobile-268e325f8610)
- [Java Code Geeks: KMP vs Flutter vs RN 2026](https://www.javacodegeeks.com/2026/02/kotlin-multiplatform-vs-flutter-vs-react-native-the-2026-cross-platform-reality.html)
- [Medium: Building Cross-Platform AI Chat App with Cursor + KMP](https://medium.com/@jacklandrin/building-a-cross-platform-ai-chat-app-with-cursor-kotlin-multiplatform-kmp-88d1f5d90e9b)
- [Baidu KMP Case Study](https://kotlinlang.org/lp/mobile/case-studies/baidu/)
- [Kotlin Official Case Studies](https://kotlinlang.org/case-studies/)
