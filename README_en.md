# ArchShowcase

> **🎨 [View Beautiful Showcase Page](https://kamssun.github.io/shellcmp-open/index_en.html)** — Get a better reading experience

KMP Cross-Platform App Shell (Android / iOS / Desktop), sharing 99% code.

## 🎥 Video Demos

| Demo | Description |
|------|-------------|
| [💻 Desktop Hot Reload](https://github.com/user-attachments/assets/ac1d9b98-64d5-4a18-8b31-c4d0310b704c) | Code changes take effect in seconds |
| [⏪ MVI State Restore](https://github.com/user-attachments/assets/4f834f16-a44f-4448-baa0-48c0496d8768) | Time travel and dev bookmarks |
| [⏱️ OBO Dispatcher](https://github.com/user-attachments/assets/382f5490-8390-4a8d-aaf1-234ae785181c) | Eliminate message backlog, stay smooth |
| [🤖 VF Automated Regression](https://github.com/user-attachments/assets/9cd654b1-b219-4878-8a59-df023b9c9c90) | Zero assertion full-chain verification |
| [💬 Extreme Concurrency](https://github.com/user-attachments/assets/0b2052ac-f67d-478c-a751-1024f0d23ee7) | WeChat-level message flood, silky smooth |

> All recorded on Pixel 4a Debug, Release builds perform even better

## Architecture

```
 ┌──────────┐  ┌──────────┐  ┌──────────┐
 │ Android  │  │   iOS    │  │ Desktop  │   ← Platform entry (thin shell)
 └────┬─────┘  └────┬─────┘  └────┬─────┘
      │             │             │
 ┌────┴─────────────┴─────────────┴────┐
 │          a-shared (99% shared)       │   ← Compose UI + MVI business logic
 │  Compose Multiplatform · Decompose  │
 │  MVIKotlin · i18n · Navigation      │
 ├─────────────────────────────────────┤
 │          a-platform (SDK bridge)     │   ← expect/actual
 │  Auth · Pay · IM · RTC · Attribution│
 ├─────────────────────────────────────┤
 │          a-core (infrastructure)     │   ← DI · Dispatcher · State restore
 └─────────────────────────────────────┘

 New pages: just Kotlin, all three platforms get it
```

## Tech Stack

Kotlin 2.3 · Compose Multiplatform · Decompose · MVIKotlin · Koin · Ktor · KSP

## Quick Start

```bash
./gradlew :androidApp:assembleDebug   # Android
./gradlew :desktopApp:run             # Desktop (hot reload)
open iosApp/iosApp.xcworkspace        # iOS (Xcode)
```

## Core Capabilities

| Capability | Description |
|------------|-------------|
| MVI State Restore | Interactive replay, dev bookmarks, production bug reproduction |
| VF Fully Automated Validation | AI-driven zero-assertion regression testing |
| OBO Dispatcher | Eliminate message backlog, pinpoint performance bottlenecks |
| Full-Chain Performance Diagnosis | Startup timeline, jank diagnostics, btrace + AI analysis |
| Fully Automated Tracking | Four event types auto-collected, three-layer desensitization |
| KSP Code Generation | Four annotations eliminate duplicate code |
| Custom UI | No Material3, minimal recomposition |

👉 **[View Full Documentation](https://kamssun.github.io/shellcmp-open/index_en.html)** for detailed explanations of each capability

[中文文档](README.md)