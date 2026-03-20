---
status: planned
module: headless/server
key-files: []
depends-on: []
---

# 待办事项与路线图

本文汇总模板报表子系统中所有**待开发**或**已有设计但未完成交付**的功能项，并与主文档中的 `P0 / P1 / P2` 迭代规划保持一致。

## P0 — 上线收口

目标：把“已上线”的主干能力真正变成稳定可交付能力。

### 全链路联调

- 模板执行、调度、导出、推送、同步全链路联调通过
- 飞书/邮件/Webhook 等渠道实际连通性验证通过
- 用户调度、手动触发、失败重试路径全量覆盖

### 压测与阈值校准

- 导出阈值校准：同步/异步分流阈值是否合理
- 调度并发校准：租户级并发、线程池容量、Misfire 场景
- 连接池隔离校准：REPORT / EXPORT / SYNC 四类池容量与等待时间
- 扫描行数阈值校准：`RowCountEstimator` 阈值和拦截策略

### 指标与告警落地

以下指标需在 `/actuator/prometheus` 可查询且 tags 完整：

- `supersonic.report.schedule.dispatch.total`
- `supersonic.report.execution.total`
- `supersonic.report.execution.duration`
- `supersonic.report.export.total`
- `supersonic.report.export.pending`
- `supersonic.report.delivery.total`
- `supersonic.report.delivery.retry.total`
- `supersonic.report.delivery.retry.pending`
- `supersonic.report.delivery.config.disabled`

告警规则建议：

- 执行成功率低于 95%
- 执行 P95 时延超过 10 秒
- 导出 pending 持续高于阈值
- 推送失败率超过 5%
- 被自动禁用推送配置数量 > 0

参考：`docker/prometheus/rules/report-slo-alert-rules.yml`

### Runbook 完善

需补齐以下标准处置手册：

- 调度失败排查：任务状态 → 执行记录 → 连接池/超时配置
- 导出异常排查：`s2_export_task` 状态机 + 文件生命周期
- 同步异常排查：Checkpoint 状态重置流程
- 推送异常排查：渠道限流、重试次数、自动禁用恢复
- 各类故障的止损步骤：临时降并发、禁用异常渠道、切换备用渠道等

### 审计回放验证

- `execution_snapshot` 可支撑历史执行回放
- 模板版本与执行记录可关联追溯
- 推送记录、失败记录、重试记录可关联定位

## P1 — 可管理性增强

目标：把模板报表从“能跑”提升到“好管”。

### 高级参数约束

- 枚举值动态来源统一（如 `optionsFrom=dimension:*` 的跨数据源一致性）
- 复杂范围约束（如日期跨度、数值上下限组合规则）
- 默认值规则增强（按租户/角色/时间上下文）
- 参数变更兼容策略（模板版本升级后老调度任务参数兼容）

### 调度可视化增强

- Misfire 补跑记录在执行历史中明确标注
- 失败重试状态、补跑状态、等待状态在 UI 可视化
- 受影响任务提示：模板/指标变更后，调度任务显示 warning 状态

### 推送治理增强

- 批量重发确认弹窗：高风险操作需二次确认
- 自动禁用推送配置后的恢复 UI 和恢复引导
- 渠道级权限和外部分发权限校验

### 全局任务中心入口

- Header 铃铛图标或独立 Drawer 入口
- 导出、调度、推送失败统一查看和处理
- 大任务、失败任务、重试中任务统一聚合

### 成本分析面板

- 基于 `scan_rows` + `execution_time_ms` + `io_bytes` 生成高成本报表榜单
- 识别高频高成本模板、慢 SQL、异常租户

## P2 — 体验增强

目标：吸收竞品长处，但不偏离“报表执行内核”定位。

### 推荐问题 / 推荐模板

- 基于模板使用频率、角色、租户、最近访问历史推荐模板和常见问题
- 对标：ThoughtSpot / Sigma

### 查询结果解释

- 对单值结果、趋势结果、异常结果生成自然语言解释
- 对标：ThoughtSpot / Tableau

### 业务术语调优

- 增加领域术语、别名、样例问题库
- 改善 NL2SQL 和模板命中率
- 对标：Databricks Genie

### 领域样例库

- 为每个模板或数据域维护样例问题
- 提升模板复用和训练效果
- 对标：Databricks / Looker

### 结构化 + 知识说明联动

- 在返回结果旁挂接指标口径、字段说明、业务背景
- 结果不仅有数据，也有解释
- 对标：Qlik Answers

## P3 — 平台演进（建议独立迭代）

以下内容更接近平台共享能力或高影响面改造，建议独立排期，不和报表子系统短周期迭代混排。

### MetricChangedEvent + ScheduleImpactListener

- 指标变更事件驱动通知
- 影响面较大，建议独立迭代
- 详见 `05-governance.md`

### Source / Destination 抽象（Connection 模型）

- 将 `s2_database` 升级为独立的 Source / Destination 概念
- 支持 role 字段标签、视图 API、Connection 自动标记

### Airbyte Connection 模型完整升级

- Source / Destination 抽象（基于 `s2_database` 增加 `roles` 字段）
- Connection Timeline（事件流式可观测性）
- Checkpointing + State 管理手动重置 UI
- `s2_data_sync_config` → `s2_connection` 数据迁移脚本（`V20__connection_roles.sql`）
- API 兼容窗口：`/api/v1/dataSyncConfigs` 标记 Deprecated 后保留 2 个版本周期

### 存储与分发扩展

- OSS 存储后端：导出文件从本地目录扩展到对象存储
- 导出文件分片下载：支持超大文件 Range 请求

## 暂不建议优先

以下方向当前不建议前置：

- 复杂 Dashboard 体系
- 连接器市场化
- 大而全 Agent 工作流平台化
- 重新做一套 BI 前端
