# 详细设计目录

本目录承接三份主设计文档中的实现级内容，目标是降低主文档阅读负担，让 AI 辅助开发（vibe-coding）可以按专题精准加载上下文。

## 导航

- 平台架构 → [智能运营数据中台设计方案](../智能运营数据中台设计方案.md)
- 模板报表 → [模板报表子系统技术设计说明书](../模板报表子系统技术设计说明书.md)
- 飞书机器人 → [飞书机器人用户交互层设计方案](../飞书机器人用户交互层设计方案.md)

---

## 平台能力 (platform/)

| 文件 | 主题 | 状态 |
|------|------|------|
| [01-datasync-connection.md](platform/01-datasync-connection.md) | Connection 生命周期、数据同步执行、Catalog 发现、通用 HTTP 连接器 | 已有设计 |
| [02-rbac-tenant.md](platform/02-rbac-tenant.md) | 多租户模型、PLATFORM/TENANT 双作用域角色、行级/字段级权限、动作型工具治理 | 已实现（部分扩展待开发）|
| [03-monitoring-alerts.md](platform/03-monitoring-alerts.md) | Prometheus 指标、Grafana 仪表盘、SLO 告警规则（5 条 P0）、Alertmanager 路由 | 已上线（部分指标待补全）|

参考文档：
- [platform-tenant-rbac-migration.md](../platform-tenant-rbac-migration.md) — RBAC 拆分迁移变更记录（前后端文件清单、API 清单、Bug 修复）
- [monitoring/report-alert-drill-checklist.md](../monitoring/report-alert-drill-checklist.md) — 告警演练清单与通过标准

---

## 模板报表 (report/)

| 文件 | 主题 | 状态 |
|------|------|------|
| [01-template-version.md](report/01-template-version.md) | 模板生命周期、版本冻结、部署快照 | 已上线 |
| [02-execution-engine.md](report/02-execution-engine.md) | `ReportExecutionOrchestrator`、三路执行（NL/结构化/SQL 模板） | 已上线 |
| [03-export-async.md](report/03-export-async.md) | 同步/异步导出分流、ExportTaskService、流式写入防 OOM | 已上线 |
| [04-scheduler-delivery.md](report/04-scheduler-delivery.md) | Quartz 动态调度、集群防重复、失败重试、多渠道投递策略 | 已上线 |
| [05-governance.md](report/05-governance.md) | 模板下线保护、影响分析、审计日志 | 已上线 |
| [07-detail-report-schedule.md](report/07-detail-report-schedule.md) | 明细报表定时推送、固定区间冻结、结构化快照回放 | 待实施 |
| [P0-上线收口实施方案.md](report/P0-上线收口实施方案.md) | P0 上线收口：联调、监控、审计回放、Runbook、压测 | 待实施 |
| [backlog.md](report/backlog.md) | 待开发功能清单 | 待开发 |

---

## 飞书机器人 (feishu/)

| 文件 | 主题 | 状态 |
|------|------|------|
| [01-event-receiving.md](feishu/01-event-receiving.md) | 事件接收与验签（Webhook + WebSocket）、幂等去重 | 已上线 |
| [02-identity-mapping.md](feishu/02-identity-mapping.md) | open_id → 平台账号映射、PENDING 审核流程 | 已上线 |
| [03-message-handlers.md](feishu/03-message-handlers.md) | 消息路由 + 7 个 Handler（Query/Export/Help/Template/History/Preview/UseAgent） | 已上线 |
| [04-api-client.md](feishu/04-api-client.md) | `SuperSonicApiClient`、HTTP 回环调用、ResultData 解包、会话隔离 | 已上线 |
| [05-card-rendering.md](feishu/05-card-rendering.md) | `FeishuCardRenderer`、卡片模板、列名中文翻译、消息发送 | 已上线 |
| [06-infra.md](feishu/06-infra.md) | 缓存（Caffeine/Redis）、限流、异步线程池、Prometheus 指标 | 已上线 |
| [backlog.md](feishu/backlog.md) | 待开发功能清单 | 待开发 |

---

## 文件命名约定

- 文件名格式：`{两位序号}-{kebab-case-主题}.md`
- frontmatter 字段：`status` / `module` / `key-files` / `depends-on`
- `status` 词汇（frontmatter）：`implemented` / `partial` / `planned`
- 文档内状态词汇：`已上线` / `已在主干` / `已有设计` / `待开发`

## 已废弃文件（内容已合并到结构化目录）

以下文件已删除，其内容已被当前结构化文件替代：

| 原文件 | 内容去向 |
|--------|---------|
| `飞书机器人-详细设计索引.md` | 导航表格在本 README；各专题已拆入 `feishu/` |
| `飞书机器人-消息链路与身份映射详细设计.md` | 内容已拆入 `feishu/02-identity-mapping.md` + `feishu/04-api-client.md` |
| `飞书机器人-导出与文件发送详细设计.md` | 内容已拆入 `feishu/03-message-handlers.md` |
| `模板报表-详细设计索引.md` | 导航表格在本 README；各专题已拆入 `report/` |
| `模板报表-数据同步与连接器依赖说明.md` | 内容已合并到 `platform/01-datasync-connection.md` |
| `飞书机器人-交互命令与限流详细设计.md` | 内容已拆入 `feishu/03-message-handlers.md` + `feishu/06-infra.md` |
