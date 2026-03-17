---
status: planned
module: headless/server
key-files: []
depends-on: []
---

# 待办事项与路线图

本文汇总模板报表子系统中所有**待开发**或**已有设计但未完成交付**的功能项，按优先级分组。

## P0 — 安全护栏（不可跳过）

- **前端下线影响范围展示**：下线按钮点击后先展示受影响的调度任务列表，要求用户逐一暂停后才允许下线（当前后端已有阻断，但前端未给出引导）
- **批量重发确认弹窗**：高风险操作（批量重发、大范围外部推送）的二次确认机制

## P1 — 核心功能补全

- **SQL 模板编辑器（前端）**：`SemanticTemplate/TemplateFormModal.tsx` 增加 SQL 模板编辑器，当前 `SqlTemplateEngine` 已在后端实现，但前端编辑入口待实现
- **参数模板高级约束能力**：
  - 枚举值动态来源统一（如 `optionsFrom=dimension:*` 的跨数据源一致性）
  - 复杂范围约束（如日期跨度、数值上下限组合规则）
  - 参数变更兼容策略（模板版本升级后老调度任务参数兼容）
- **Misfire 补跑标注**：Misfire 补跑记录在执行历史中的标注（当前仅日志记录，未在 UI 中区分）
- **任务中心全局入口**：Header 铃铛图标或独立 Drawer 入口，当前任务中心无全局触发点

## P2 — 可观测性与运维

- **执行成本报表**：基于 `scan_rows` + `execution_time_ms` + `io_bytes` 构建报表成本排行榜，识别高成本报表
- **调度任务 warning 状态可视化**：指标变更后受影响的调度任务在前端有醒目标注
- **自动禁用推送配置的恢复 UI**：当前连续失败自动禁用后，恢复流程仅靠后端 API，前端无引导
- **验收清单落地**：以下指标需在 `/actuator/prometheus` 可查询且 tags 完整：
  - `supersonic.report.schedule.dispatch.total`
  - `supersonic.report.execution.total`
  - `supersonic.report.execution.duration`
  - `supersonic.report.export.total`
  - `supersonic.report.export.pending`
  - `supersonic.report.delivery.total`
  - `supersonic.report.delivery.retry.total`
  - `supersonic.report.delivery.retry.pending`
  - `supersonic.report.delivery.config.disabled`
- **告警规则**（参考 `docker/prometheus/rules/report-slo-alert-rules.yml`）：
  - 执行成功率低于 95%
  - 执行 P95 时延超过 10 秒
  - 导出 pending 持续高于阈值
  - 推送失败率超过 5%
  - 被自动禁用推送配置数量 > 0

## P2 — 存储与扩展

- **OSS 存储后端**：导出文件当前仅支持本地目录，OSS 配置项已预留但未实现
- **导出文件分片下载**：超大文件的 Range 请求支持

## P3 — 平台演进（建议独立迭代）

- **MetricChangedEvent + ScheduleImpactListener**：指标变更事件驱动通知，影响面较大，建议独立迭代。详见 `05-governance.md`
- **Source/Destination 抽象（Connection 模型）**：将 `s2_database` 升级为独立的 Source/Destination 概念，支持 role 字段标签、视图 API、Connection 自动标记。详见正文 3.10.9，当前暂缓（P2 优先级降低）
- **Airbyte Connection 模型完整升级**：
  - Source / Destination 抽象（基于 `s2_database` 增加 `roles` 字段）
  - Connection Timeline（事件流式可观测性）
  - Checkpointing + State 管理手动重置 UI
  - `s2_data_sync_config` → `s2_connection` 数据迁移脚本（V20__connection_roles.sql）
  - API 兼容窗口：`/api/v1/dataSyncConfigs` 标记 Deprecated 后保留 2 个版本周期

## 运维 Runbook（待完善）

标准处置流程已有文字描述（见主文档 5.5），但以下具体操作手册仍需完善：

- 调度失败排查：任务状态 → 执行记录 → 连接池/超时配置
- 导出异常排查：`s2_export_task` 状态机 + 文件生命周期
- 同步异常排查：Checkpoint 状态重置流程
- 推送异常排查：渠道限流、重试次数、自动禁用恢复
- 各故障场景的止损操作步骤（临时降并发、禁用异常渠道、切换备用渠道等）
