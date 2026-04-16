# VF 回归验证

运行 VF 截图回归验证，对比当前 APP 行为与录制基线，SSIM 不通过时 AI 辅助诊断。

## 用户输入

向用户确认：

1. **范围** — 全部（默认，扫描 `tools/verify/test-vfs/`）/ 指定 VF 目录 / 指定功能模块名
2. **设备**（可选）— 多设备时指定 serial

收集完成后，使用 Agent tool 启动 `vf-verifier` agent（`.claude/agents/vf-verifier.md`），将用户选择传入 prompt。
