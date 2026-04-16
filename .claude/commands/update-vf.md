# VF 自动录制更新

分析 git 变更（或全量扫描），找出受影响的 UI，AI 推断交互路径，调用 headless 录制生成/更新 VF 包。

## 用户输入

向用户确认：

1. **模式** — 增量（默认，基于 git diff）还是全量扫描？
2. **指定模块**（可选）— 只更新某些功能的 VF？留空则自动分析全部受影响模块

收集完成后，使用 Agent tool 启动 `vf-recorder` agent（`.claude/agents/vf-recorder.md`），将用户选择的模式和模块传入 prompt。
