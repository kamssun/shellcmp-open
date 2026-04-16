# 提测流程

功能开发完成后，执行全套检查并生成提测报告。

## 用户输入

向用户确认：

1. **分支** — 当前分支是否正确（如在 master 上则提示先创建功能分支）
2. **变更概述** — 一句话描述本次变更（用于报告标题）

收集完成后，使用 Agent tool 启动 `submit-test` agent（`.claude/agents/submit-test.md`），将用户输入传入 prompt。
