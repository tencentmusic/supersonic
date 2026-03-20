# 评测流程

1. 正常启动项目(必须包括LLM服务)
2. 执行 `evaluation.sh` 脚本，流程包括构建表数据、创建隔离的语义资源、轮询等待解析能力就绪、获取模型预测结果并执行对比。
3. 命令行会输出执行准确率，失败样例会写到 `error_case.json`，完整结构化报告会写到 `evaluation_report.json`。

# 评测意义

制定评测工具方便supersonic快速对接其他大模型、更改参数配置，对于评估提示词、代码更改所带来的影响至关重要，可以帮助我们了解这些变化是否会提高或降低准确率、响应速度。

# 可配置项

支持通过 `evaluation/config/config.yaml` 或环境变量覆盖关键参数：

- `EVAL_BASE_URL`
- `EVAL_USERNAME`
- `EVAL_TOKEN` / `EVAL_TOKEN_SECRET`
- `EVAL_DATABASE_ID`
- `EVAL_PREPARE_TIMEOUT_SECONDS`
- `EVAL_POLL_INTERVAL_SECONDS`
- `EVAL_QUERY_INTERVAL_SECONDS`
- `EVAL_RUN_ID`
- `EVAL_RUN_PREFIX`
- `EVAL_CLEANUP_LOCAL_ARTIFACTS`
