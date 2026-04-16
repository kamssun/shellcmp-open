# 提交

执行完整的 pre-commit 检查流程并提交代码。

用户补充说明（可选，如变更描述、scope 提示）：$ARGUMENTS

1. 读取 `.claude/instincts/commit-convention.yaml`、`.claude/instincts/code-review.yaml` 和 `.claude/instincts/testing.yaml`
2. 按 `commit-convention.yaml` 定义的流程顺序执行全部检查
3. 全部通过后，按消息格式生成 commit message，确认后提交
