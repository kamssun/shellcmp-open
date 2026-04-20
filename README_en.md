# ArchShowcase

KMP (Kotlin Multiplatform) Cross-platform App Shell (Android / iOS / Desktop).

[中文文档](README.md)

## 🎥 Video Demos

- [💻 Desktop Hot Reload Demo](https://github.com/user-attachments/assets/ac1d9b98-64d5-4a18-8b31-c4d0310b704c) - Instant reloads for massive development efficiency boosts.
- [⏪ MVI State Restoring Demo](https://github.com/user-attachments/assets/4f834f16-a44f-4448-baa0-48c0496d8768) - Time travel debugging and dev bookmarks.
- [⏱️ OBO Dispatcher Demo](https://github.com/user-attachments/assets/382f5490-8390-4a8d-aaf1-234ae785181c) - Solves message backlog, keeping long lists extremely smooth.
- [🤖 Automated Regression Demo](https://github.com/user-attachments/assets/9cd654b1-b219-4878-8a59-df023b9c9c90) - Full-chain UI/logic validation with zero manual assertions.
- [💬 Extreme WeChat-like Message Concurrency Demo](https://github.com/user-attachments/assets/0b2052ac-f67d-478c-a751-1024f0d23ee7) - Breaking through rendering bottlenecks under high concurrent pressure to maintain a silky smooth experience.

> 💡 **Note: All videos above were recorded on a Pixel 4a in Debug mode. Performance and fluidity are even better in Release builds.**

## Architecture

```
 ┌──────────┐  ┌──────────┐  ┌──────────┐
 │ Android  │  │   iOS    │  │ Desktop  │   ← Platform entry (Thin shell)
 └────┬─────┘  └────┬─────┘  └────┬─────┘
      │             │             │
 ┌────┴─────────────┴─────────────┴────┐
 │          a-shared (99% shared)      │   ← Compose UI + MVI Business Logic
 │  Compose Multiplatform · Decompose  │
 │  MVIKotlin · i18n · Navigation      │
 ├─────────────────────────────────────┤
 │        a-platform (SDK Bridge)      │   ← expect/actual
 │  Auth · Pay · IM · RTC · Analytics  │
 ├─────────────────────────────────────┤
 │        a-core (Infrastructure)      │   ← DI · Dispatcher · State Restoring
 └─────────────────────────────────────┘

 Add a new page in Kotlin once, and get it on all three platforms simultaneously.
```

## Modules

| Module | Responsibility |
|--------|----------------|
| `a-core` | Infrastructure: DI, Dispatchers, State Restoring / Leak Auditing / SSIM Screenshot Validation |
| `a-platform` | Platform SDK Bridge: Auth, Pay, IM, RTC, Analytics, NetworkRecorder |
| `a-shared` | Shared UI and Business Logic (Compose Multiplatform): MVI + i18n |
| `ksp-annotations` / `ksp-processor` | `@Replayable` / `@CustomState` / `@MemoryTrackable` / `@RouteRegistry` / `@VfResolvable` code generation |
| `build-plugin` | Gradle Build Plugin: ASM bytecode rewriting to intercept third-party SDK Handler calls and route them to OBO Dispatcher |
| `androidApp` / `iosApp` / `desktopApp` | Platform entry points |
| `macrobenchmark` | Baseline Profile generation + Startup benchmarking (Android) |
| `tools/btrace` | btrace (RheaTrace) 3.0 performance tracing tool |
| `tools/verify` | VF Screenshot Regression: SSIM comparison engine + recording/verification scripts |

## Tech Stack

Kotlin 2.3 · Compose Multiplatform · Decompose · MVIKotlin · Koin · Ktor · KSP

## Build

```bash
./gradlew :androidApp:assembleDebug        # Android
./gradlew :desktopApp:run                  # Desktop
open iosApp/iosApp.xcworkspace             # iOS (Xcode)
```

## Why this architecture is worth it

**One person maintaining three platforms, and quality actually improves.** The capabilities below are not isolated technical highlights; they enhance each other to form a flywheel: MVI's determinism makes state restoring possible → State restoring makes fully automated VF validation possible → Higher VF coverage means fewer bugs → Performance monitoring guarantees no lag → Fully automated event tracking makes data collection zero-cost → KSP code generation lowers the barrier to entry → AI specification systems ensure even beginners can't write bad code.

### One Codebase, Three Platforms

Add a new page by changing only Kotlin, and get it simultaneously on Android / iOS / Desktop. Fix a bug once, and it's fixed everywhere. There is no "fixed on Android but forgotten on iOS".

**The real value of Desktop is development efficiency**: Desktop supports Hot Recomposition, where code changes take effect in seconds. You don't need to wait for compilation, installation, and app restarts like on Android. During daily development, you iterate on UI and logic quickly on Desktop, and only verify on real devices once confirmed. The development experience is close to frontend hot reloading.

Compared to industry norms (maintaining separate Android and iOS teams, duplicate codebases, writing every feature twice, fixing every bug twice, and tolerating inconsistent behavior between platforms), this architecture achieves true consistency with a single developer.

See [Cross-Platform Analysis](docs/CROSS_PLATFORM_ANALYSIS.md).

## Core Capabilities

### MVI State Restoring — The Foundation

A strictly unidirectional data flow architecture naturally produces a complete operation history — every user action is recorded, and state changes are fully traceable and replayable. This characteristic is the foundation of all subsequent advanced capabilities:

- **Interactive Time Travel** — Automatically records all operations, supporting time-travel replay to any moment, like fast-forwarding or rewinding a video.
- **State Export/Import** — Export the entire current UI state to a file, which can be restored across devices. **Production Bug Repro**: When a user encounters a bug, export the state file, send it to the developer, and the developer can import it with one click to see the user's exact UI at that time, eliminating the guesswork of "what did the user do".
- **Dev Bookmarks** — One-click save of the current interface. Upon restart, navigate directly to a deep page (e.g., third-level settings), saving you from repeatedly clicking through navigation and drastically accelerating debugging.
- **Leak Auditing** — Automatically detects whether state is correctly released after page destruction.
- **The Bedrock of Fully Automated Validation** — The VF validation system, recording system, and server data mocking are entirely built on the determinism of MVI. Without this foundation, none of the rest would be possible.

See [State Restore](docs/STATE_RESTORE.md).

### AI Fully Automated Validation (VF)

More than just UI screenshot comparison — **any bug that causes a UI change will be caught**. VF records the complete user operation path + server data snapshot, then replays it exactly and compares screenshots during validation. Thus, not only UI styling issues, but also logic bugs in the main path (data calculation errors, state transition anomalies, incorrect conditional branches) will be discovered as long as they reflect on the UI. When all critical paths are covered by recording, it's theoretically equivalent to a full regression test suite with zero manual assertions.

**An order of magnitude faster than traditional E2E tests**: Traditional solutions rely on "find view → click button → wait for animation → find next view", incurring lookup and waiting overheads at every step. VF is based on the MVI architecture, directly injecting operation instructions into the business layer for replay, without needing to simulate touches, traverse UI elements, or wait for animations. State restoration and instruction replay are purely memory operations, only rendering during the final screenshot. This enables validation speeds far beyond any UI automation solution.

Workflow:
1. **Change Analysis** — AI analyzes code changes, identifies affected test cases, and flags uncovered new features.
2. **Path Inference** — Reads business code, automatically enumerates all interaction paths, and generates operation plans.
3. **Auto-Recording** — AI controls real devices to simulate user operations and complete recordings without manual intervention.
4. **Screenshot Validation** — Proprietary image similarity engine compares screenshots + difference heatmaps, automatically judging unexpected changes.
5. **Server Data Mocking** — Automatically captures network requests/responses during recording, and replays them directly during validation, completely eliminating interference from backend data changes or network environments.

Recordings support two modes: **AI Auto-Recording** (one command) and **Manual Recording** (click start/end via in-app floating window). AI recording covers most scenarios; for complex interactions or when AI recording isn't ideal, a single manual recording is saved permanently, and subsequent validation is fully automated.

See [VF Validation](tools/verify/README.md) · [VF Recording Guide](docs/vf-recording-guide.md).

### OBO Dispatcher — Zero Lag + Traceable Performance Issues

A proprietary task dispatcher solving two core issues:

**Issue 1: Message Backlog Causing Lag.** Without OBO, a large number of asynchronous tasks are submitted to the main thread queue simultaneously, instantly filling it. While Android has a sync barrier mechanism (prioritizing rendering frames), barriers can only skip waiting tasks in the queue; **they cannot interrupt currently executing tasks or prevent the already-filled synchronous messages from being executed one by one**. Result: the rendering frame, despite its high priority, drops frames because of the backlog of tasks ahead of it.

OBO's approach: The queue **never holds more than one task**. The next task is submitted only after the current one finishes. When VSYNC arrives, the queue is practically empty, the barrier mechanism works normally, and rendering frames execute smoothly.

**Issue 2: Lag Exists But Hard to Isolate.** Without OBO, dozens of tasks execute crammed together. Performance monitoring can only see "this batch took 200ms total", unable to isolate the slow task. With OBO, each task is an independent message, allowing monitoring to pinpoint "Task X took 50ms" — **OBO is the prerequisite for precise performance monitoring.**

- **Effect** — Under identical workloads (30 list items × 10 tasks × 3ms), native concurrency freezes completely, while OBO executes them sequentially, maintaining smooth scrolling.
- **Extreme Concurrency Simulation (WeChat-level Message Flood)** — Built an extremely high-pressure IM message concurrency scenario internally (e.g., 1000 active chat groups simultaneously, with ultra-high frequency message spam). Combining the OBO dispatcher, chunked Flush backpressure mechanisms, and precise rendering updates, it keeps long lists and chat windows silky smooth under insane concurrency, thoroughly breaking through rendering bottlenecks.
- **Built-in Comparison UI** — A stress-test page features a real-time framerate indicator + adjustable parameters. Toggle between OBO / Native modes with one click to visibly see the difference.
- **Unified Across Platforms** — Adapted for Android / iOS / Desktop.
- **3rd-Party SDK Handler Interception** — OBO only governs your own code. 3rd-party SDKs (IM, payments, analytics) bypass OBO by directly calling `Handler.post/send` on the main thread, causing backlogs. Solution: ASM bytecode rewriting at compile-time intercepts these calls and routes them to OBO automatically, without SDK cooperation. System frameworks (`androidx.*`, `kotlinx.coroutines.*`) are excluded. Reports available at `build/reports/obo-handler/`.
- **Source-Level Lag Diagnostics** — ASM rewriting injects the caller's full class name. At runtime, OBO tracks each task's origin and execution time. Three detections: single tasks > half a frame (slow tasks), sources taking >100ms within 1s (high-frequency spam), and queue depths >512 (backlog warning). JANK logs display the full origin for OBO slow tasks (e.g., `OBOScheduler com.thirdparty.im.sdk.IMClient 51ms`).

**Trade-off**: OBO lengthens the *total completion time* of a batch of tasks (because frame callbacks are inserted between tasks), but the *single task speed* remains the same. Users won't notice if "30 background tasks finish in 2 seconds vs 1 second", but they *will* notice if "scrolling lags for 1 second". Prioritizing fluidity over absolute speed is the right trade-off.

Note: OBO prevents message backlogs, but if a single task is inherently heavy (e.g., 50ms), it will still lag. OBO's diagnostic capabilities (`enableOBODiagnostics`) accurately pinpoint the specific SDK or business code causing it.
See [OBO Handler Interception](docs/OBO_HANDLER_INTERCEPT.md).

### Closed-Loop Performance Diagnostics

A proprietary runtime performance monitoring system, independent of third parties, covering all three platforms. Because OBO ensures each task is an independent message, monitoring can accurately isolate the task slowing down the main thread:

| Capability | Description |
|------------|-------------|
| Startup Timeline | Tree view displaying the duration of each startup step (process creation → DI → SDK init → first frame render), making bottlenecks obvious at a glance. |
| Jank Diagnostics | Automatically detects dropped frames while collecting: GC pauses, **CPU usage** (instantly revealing if the code is heavy or preempted by other tasks), and the user's action at the moment of lag. |
| GC Pressure Detection | 5s sliding window monitoring blocking GC. Outputs PSS memory breakdown + collection sizes of each Store (auto-generated by KSP `@MemoryTrackable`) when thresholds are exceeded, quickly isolating memory bloat sources. |
| Main Thread Slow Task Monitor | Since OBO isolates tasks, the monitor captures individual slow tasks (e.g., network callbacks, DB operations, business logic) and reports them, pinpointing "this exact task was 50ms slow" instead of a bundled duration. |
| Frame Breakdown + Trace Alignment | Every frame is broken down into 6 stages (queue → animation → layout → draw → sync → GPU), and aligned precisely with btrace flame graphs via frame numbers. Logs tell you "which stage is slow", traces tell you "which function is slow". |
| Page Frame Stats | Automatically aggregates on page exit: average framerate, dropped frames, jank occurrences. |
| btrace + AI Analysis | ByteDance's btrace captures function-level durations. Debug builds are instantly readable; Release builds are automatically de-obfuscated — restoring not just btrace instrumentations but also system samples, compiler inlines, coroutine lambdas, and deeply optimized functions. AI then analyzes the trace data with source code to provide optimization suggestions. |
| Startup Benchmarking | Automatically generates pre-compiled rules + startup benchmarks, quantifying optimization results. |

See [Performance Monitoring Guide](docs/PERF_MONITOR.md) · [btrace Guide](tools/btrace/README.md).

### Fully Automated Tracking — Zero-Code Data Collection

Four types of events (Page, Interaction, Exposure, Foreground/Background) are collected automatically. Developers don't need to write tracking code when implementing business logic:

| Capability | Approach |
|------------|----------|
| Interaction Tracking | All clicks/long-presses/toggles go through the `appClickable` Modifier series, automatically capturing user actions and linking them to Store Intents. |
| Exposure Tracking | `ExposureLazyColumn` replaces `LazyColumn`, automatically tracking exposure events for items visible ≥50% and lingering ≥500ms, with same-page deduplication. |
| KSP Auto-Mapping | `EventMapperProcessor` scans all `sealed interface Intent` in Stores without annotations, automatically mapping Intents to event names. New Intents are reported with zero config. |
| Business Param Extraction | `ExposureParamsProcessor` scans data classes, generating `toExposureParams()` extensions. List exposures automatically carry business fields. |
| 3-Layer Masking | Compile-time field matching (excluding url/token/password, hashing email/phone) → Runtime regex detection (phone/email masking) → Length truncation (>200 chars). |

Effect: Adding a new list page with tracking = use `ExposureLazyColumn` + `appClickable`. Zero tracking code written; event names, business parameters, and masking are fully automated.

See [Automated Tracking](docs/AUTO_TRACKING.md).

### Custom UI Components + Minimal Recomposition

Removed Google's official Material3 component library, building a lightweight component system to drastically reduce unnecessary recompositions:

| Strategy | Approach |
|----------|----------|
| Drop Material3 | Buttons, text, cards, nav bars, etc. are all proprietary, removing the hidden overheads of official components. |
| List Scroll Optimization | Visual effects like rounded corners are implemented as lightweight as possible to avoid triggering UI recalculations during scrolling. |
| Smart Skipping | Compiler automatically judges "don't refresh if data hasn't changed", paired with stability declarations so more components enjoy skipping optimizations. |
| Deferred Reading | High-frequency changing values (animations, scrolling) are read at the very last moment, skipping UI recalculations and updating visuals directly. |
| Calculation Caching | Results of sorting, filtering, formatting, etc., are automatically cached. Unchanged data means no recalculation. |

**How to Verify**: Use Android Studio's Running Devices + Layout Inspector to highlight recomposing components in real-time. When scrolling lists or switching pages, you'll visibly see that only components with genuinely changed data are refreshed, while the rest are completely skipped.

### KSP Code Generation — Lowering the Barrier

Four annotations eliminate vast amounts of boilerplate code. New features only require annotations, no manual glue code:

| Annotation | Purpose |
|------------|---------|
| `@Replayable` | Auto-generates state export code; zero manual integration for the Time Travel system. |
| `@RouteRegistry` | Just one line for a new page, and routing registration code is auto-generated. |
| `@VfResolvable` | One annotation on a new business module automatically hooks it into the Validation system. |
| `@CustomState` | Compound annotation (`@SelectableState` + `@MemoryTrackable`), generating field subscriptions + memory snapshots simultaneously. |

Effect: Adding a new page with Time Travel + Auto Validation = Write business code + add two annotations. Everything else is generated.

### AI Specification System — Preventing Bad Code

34 strict prohibitions + layered rule systems, enforced by AI in real-time during coding — not after-the-fact reviews, but preventing mistakes during creation:

- **Prohibition Auto-Blocking** — Forbidden code patterns (e.g., main thread network requests, hardcoded keys, memory leak patterns) are blocked instantly during writing.
- **On-Demand Domain Rules** — Automatically loads UI rules when editing UI, network rules when editing network code. Developers don't need to memorize the entire rulebook.
- **Pre-Commit Automated Checks** — Testing, linting, compliance reviews, and documentation checks must pass before committing. Bypassing is impossible.

Meaning: When the team expands, junior developers' code quality won't drag the project down. Standards aren't enforced by human memory, but guaranteed by the system.

**OpenSpec — AI-Driven Complex Feature Development**

Cross-module large requirements go through the OpenSpec structured flow: AI generates proposals → Tech Design → Task Breakdown (can be manually reviewed/edited), then implemented step-by-step according to spec. Design intent doesn't get lost during coding. Already successfully applied to 7 complex features (Design System, Perf Monitor, Dev Bookmarks, iOS Restoring, VF Recording, etc.).

### Multi-Tier Testing System

Beyond VF Auto-Validation, traditional testing is equally complete, forming a multi-layered defense from fast/granular to slow/broad:

| Tier | Capability | Speed |
|------|------------|-------|
| Unit Tests | Independent tests for all three modules (core / platform / shared), runs on Desktop without devices. | Seconds |
| Coverage Gates | Kover aggregated coverage **≥80%** is a hard requirement. Builds fail below the threshold; "release first, test later" is not permitted. | Seconds |
| Preview Screenshot Diffing | Roborazzi automatically scans all `@Preview`, running on JVM without devices. **Crucially: Previews boot a complete Koin DI environment** (DI + Navigation + i18n + Theming), rendering components in a near-real runtime state, not with static mock data. | Tens of Seconds |
| VF Auto-Validation | Operational path-level regression, covering logic + UI, with server data mocking. | Minutes |
| E2E Testing | Maestro supports Android + iOS real device end-to-end testing. | Minutes |

Unit tests ensure function correctness, coverage gates eliminate blind spots, Preview diffing ensures component visuals, VF guarantees complete paths, and E2E ensures real-device/environment functionality. Five overlapping layers, each automated, with zero reliance on manual regression.

See [Testing Guide](TESTING.md).

## Performance Profiling

```bash
./gradlew :androidApp:generateBaselineProfile     # Generate Baseline Profile (Requires physical device API 28+)
./gradlew :macrobenchmark:connectedBenchmarkAndroidTest  # Start benchmarks
```

## Documentation

- [Testing Guide](TESTING.md)
- [State Restore](docs/STATE_RESTORE.md)
- [Automated Tracking](docs/AUTO_TRACKING.md)
- [Performance Monitoring](docs/PERF_MONITOR.md)
- [OBO Handler Interception](docs/OBO_HANDLER_INTERCEPT.md)
- [Architecture Maps](docs/codemaps/)
- [Cross-Platform Analysis](docs/CROSS_PLATFORM_ANALYSIS.md) — KMP+CMP vs Native+AI
