# ArchShowcase

> **🎨 [查看精美展示页](https://kamssun.github.io/shellcmp-open/)** — 获得更好的阅读体验

KMP 跨平台 App 外壳项目（Android / iOS / Desktop），共享 99% 代码。

## 🎥 视频演示

| 演示 | 说明 |
|------|------|
| [💻 Desktop 热重载](https://github.com/user-attachments/assets/ac1d9b98-64d5-4a18-8b31-c4d0310b704c) | 改代码秒级生效 |
| [⏪ MVI 状态回溯](https://github.com/user-attachments/assets/4f834f16-a44f-4448-baa0-48c0496d8768) | 时间旅行与开发书签 |
| [⏱️ OBO 调度器](https://github.com/user-attachments/assets/382f5490-8390-4a8d-aaf1-234ae785181c) | 解决消息积压，保持流畅 |
| [🤖 VF 自动化回归](https://github.com/user-attachments/assets/9cd654b1-b219-4878-8a59-df023b9c9c90) | 零手写断言的全链路验证 |
| [💬 极限并发模拟](https://github.com/user-attachments/assets/0b2052ac-f67d-478c-a751-1024f0d23ee7) | 微信级消息洪峰丝滑体验 |

> 以上均在 Pixel 4a Debug 环境录制，Release 包性能更佳

## 架构

```
 ┌──────────┐  ┌──────────┐  ┌──────────┐
 │ Android  │  │   iOS    │  │ Desktop  │   ← 平台入口（薄壳）
 └────┬─────┘  └────┬─────┘  └────┬─────┘
      │             │             │
 ┌────┴─────────────┴─────────────┴────┐
 │          a-shared (99% 共享)         │   ← Compose UI + MVI 业务
 │  Compose Multiplatform · Decompose  │
 │  MVIKotlin · i18n · Navigation      │
 ├─────────────────────────────────────┤
 │          a-platform (SDK 桥接)       │   ← expect/actual
 │  Auth · Pay · IM · RTC · 归因       │
 ├─────────────────────────────────────┤
 │          a-core (基础设施)           │   ← DI · 调度器 · 状态回溯
 └─────────────────────────────────────┘

 新增页面只改 Kotlin，三端同时获得
```

## 技术栈

Kotlin 2.3 · Compose Multiplatform · Decompose · MVIKotlin · Koin · Ktor · KSP

## 快速开始

```bash
./gradlew :androidApp:assembleDebug   # Android
./gradlew :desktopApp:run             # Desktop（支持热重载）
open iosApp/iosApp.xcworkspace        # iOS (Xcode)
```

## 核心能力

| 能力 | 说明 |
|------|------|
| MVI 状态回溯 | 交互回溯、开发书签、线上 Bug 一键复现 |
| VF 全自动验证 | AI 驱动的零手写断言回归测试 |
| OBO 调度器 | 消除消息积压，精准定位性能瓶颈 |
| 全链路性能诊断 | 启动时间线、卡顿诊断、btrace + AI 分析 |
| 全自动埋点 | 四类事件自动采集，三层脱敏 |
| KSP 代码生成 | 4 个注解消除重复代码 |
| 自定义 UI | 去 Material3，极少重组 |

👉 **[查看完整文档](https://kamssun.github.io/shellcmp-open/)** 了解每个能力的原理和实现

[English](README_en.md)
