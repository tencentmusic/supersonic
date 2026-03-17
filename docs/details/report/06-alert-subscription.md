---
status: planned
module: headless/server
key-files:
  - headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/AlertRuleServiceImpl.java
  - headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/AlertCheckDispatcher.java
  - headless/server/src/main/java/com/tencent/supersonic/headless/server/task/AlertCheckJob.java
  - headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/AlertEvaluator.java
depends-on:
  - docs/details/report/04-scheduler-delivery.md
  - docs/details/report/02-execution-engine.md
---

# 告警订阅

## 目标与边界

### 目标

支持用户配置基于语义模板查询结果的告警规则。当定时执行查询后，逐行评估阈值条件，触发时通过已有推送渠道（飞书群、Email、Webhook 等）发送告警通知。

核心验收指标：**排查时间从 4 小时降到 30 分钟以内。**

### 驱动场景

信贷系统多合作方独立部署，每日批量推数据。跑批失败有告警，但"跑批成功、数据不全"是盲区。每月 1-2 次事后才发现，每次排查 4 小时。详见 [discovery findings](../../product/02a-discovery-findings.md)。

### 边界

| 范围 | 包含 | 说明 |
|------|------|------|
| 告警规则 CRUD + 调度 | ✅ | 本文核心 |
| 阈值条件评估引擎 | ✅ | 偏差百分比、绝对值比较、缺失检测 |
| 复用现有推送渠道 | ✅ | 复用 `ReportDeliveryChannel` 策略模式 |
| 告警去重与静默期 | ✅ | 防止重复告警轰炸 |
| 告警执行记录 | ✅ | 审计追溯 |
| 告警归因分析 | ❌ | 二期考虑 |
| 自动修复/补数据 | ❌ | 超出范围 |

### SPEC Discovery 约束（Phase 1-3 确认）

| 约束 | 结论 | 来源 |
|------|------|------|
| 创建权限 | 管理员不受限 + 分析师限本人可访问 DataSet | Phase 1 Q1 |
| 规则数量 | 每租户软限制 50 条（AG-10） | Phase 1 Q2 |
| 查询超时 | 共用线程池，告警查询 30 秒超时 | Phase 1 Q3 |
| 失败处理 | 连续失败 5 次自动禁用 + 飞书通知 owner | Phase 1 Q4 |
| 事件保留 | 90 天自动清理 | Phase 1 Q5 |
| queryConfig 限制 | 禁止 QuerySqlReq，只允许 QueryStructReq / SqlTemplateConfig（AG-06） | Phase 2 |
| Cron 最小间隔 | ≥ 5 分钟（AG-07） | Phase 2 |
| 结果行数上限 | LIMIT ≤ 1000（AG-08） | Phase 2 |
| 消息模板转义 | 变量插值需转义 Markdown 特殊字符（AG-09） | Phase 2 |
| 自动禁用审计 | s2_alert_rule 新增 `disabled_reason` 字段 | Phase 3 |

### 与报表调度的关系

| 维度 | 报表调度 | 告警订阅 |
|------|---------|---------|
| 触发后行为 | 查询 → 生成文件 → 推送文件 | 查询 → 逐行评估条件 → 仅异常行推送 |
| Quartz Group | `REPORT` | `ALERT` |
| 输出物 | Excel/CSV 文件 | 告警消息卡片（无文件） |
| 推送频率 | 每次都推 | 仅异常时推，有静默期 |

---

## 核心概念模型

```
AlertRule（告警规则）
├── 名称、描述、创建者
├── 关联 DataSet + 查询配置（queryConfig）
├── Cron 表达式（检查频率）
├── conditions: AlertCondition[]（阈值条件组）
├── delivery_config_ids（推送渠道）
├── silence_minutes（静默期）
└── enabled

AlertCondition（单条阈值条件）
├── metric_field        → 评估的指标字段名
├── operator            → GT / LT / DEVIATION_GT / ABSENCE 等
├── threshold           → 阈值
├── baseline_field      → 基线字段名（用于偏差计算）
├── dimension_field     → 分组维度字段名（定位具体实体）
├── severity            → WARNING / CRITICAL
└── message_template    → 告警消息模板（支持 ${变量} 插值）

AlertExecution（执行记录）
├── rule_id, status, start_time, end_time
├── total_rows / alerted_rows / silenced_rows
└── error_message

AlertEvent（单条告警事件）
├── execution_id, rule_id, condition_index
├── alert_key（去重键：rule_id + condition_index + dimension_value）
├── dimension_value, metric_value, baseline_value, deviation_pct
├── delivery_status
└── silence_until
```

---

## 数据模型

### s2_alert_rule

```sql
CREATE TABLE IF NOT EXISTS `s2_alert_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(200) NOT NULL COMMENT '告警规则名称',
    `description` VARCHAR(500) COMMENT '规则描述',
    `dataset_id` BIGINT NOT NULL COMMENT '关联 DataSet',
    `query_config` TEXT NOT NULL COMMENT 'JSON: 查询配置',
    `conditions` TEXT NOT NULL COMMENT 'JSON: AlertCondition 数组',
    `cron_expression` VARCHAR(100) NOT NULL COMMENT 'Cron 表达式',
    `enabled` TINYINT DEFAULT 1,
    `owner_id` BIGINT COMMENT '规则创建者',
    `delivery_config_ids` VARCHAR(500) COMMENT '推送渠道ID（逗号分隔）',
    `silence_minutes` INT DEFAULT 60 COMMENT '静默期（分钟）',
    `retry_count` INT DEFAULT 2,
    `retry_interval` INT DEFAULT 30,
    `quartz_job_key` VARCHAR(200),
    `last_check_time` DATETIME DEFAULT NULL,
    `next_check_time` DATETIME DEFAULT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` VARCHAR(100),
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`),
    KEY `idx_alert_rule_tenant` (`tenant_id`),
    KEY `idx_alert_rule_dataset` (`dataset_id`)
);
```

### s2_alert_execution

```sql
CREATE TABLE IF NOT EXISTS `s2_alert_execution` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `rule_id` BIGINT NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `start_time` DATETIME DEFAULT NULL,
    `end_time` DATETIME DEFAULT NULL,
    `total_rows` BIGINT DEFAULT 0,
    `alerted_rows` BIGINT DEFAULT 0,
    `silenced_rows` BIGINT DEFAULT 0,
    `error_message` VARCHAR(2000),
    `execution_time_ms` BIGINT,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_alert_execution_rule` (`rule_id`),
    KEY `idx_alert_execution_status` (`status`)
);
```

### s2_alert_event

```sql
CREATE TABLE IF NOT EXISTS `s2_alert_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `execution_id` BIGINT NOT NULL,
    `rule_id` BIGINT NOT NULL,
    `condition_index` INT NOT NULL,
    `severity` VARCHAR(20) NOT NULL DEFAULT 'WARNING',
    `alert_key` VARCHAR(300) NOT NULL,
    `dimension_value` VARCHAR(500),
    `metric_value` DOUBLE,
    `baseline_value` DOUBLE,
    `deviation_pct` DOUBLE,
    `message` TEXT,
    `delivery_status` VARCHAR(20) DEFAULT 'PENDING',
    `silence_until` DATETIME,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_alert_event_execution` (`execution_id`),
    KEY `idx_alert_event_alert_key` (`alert_key`),
    KEY `idx_alert_event_severity` (`severity`)
);
```

---

## 执行主链路

```
Quartz 按 Cron 触发
    │
    ▼
AlertCheckJob.execute(ruleId)
    │
    ▼
AlertCheckDispatcher.dispatch(ruleId)
    │
    ├─→ 1. 加载 AlertRule，校验 enabled + DataSet 有效性
    ├─→ 2. 解析 queryConfig → SemanticQueryReq
    ├─→ 3. semanticLayerService.queryByReq() → 查询结果
    │
    ├─→ 4. AlertEvaluator.evaluate(rows, conditions)
    │       对每一行 × 每个 condition：
    │       ├─→ 提取 metric_value、baseline_value
    │       ├─→ 按 operator 判断是否触发
    │       └─→ 生成 AlertEventCandidate
    │
    ├─→ 5. 静默期过滤
    │       ├─→ 计算 alert_key = rule_id + condition_index + dimension_value
    │       └─→ 查 s2_alert_event 最近同 key 的 silence_until > NOW() → 跳过
    │
    ├─→ 6. 持久化 AlertExecution + AlertEvent
    │
    ├─→ 7. 聚合告警事件 → 构建飞书告警卡片
    │       调用 ReportDeliveryService.deliver()
    │
    └─→ 8. 更新 rule.lastCheckTime
```

---

## 条件类型

| Operator | 说明 | 评估逻辑 | 需要 baseline_field |
|----------|------|---------|-------------------|
| `GT` | 大于 | `value > threshold` | 否 |
| `LT` | 小于 | `value < threshold` | 否 |
| `GTE` | 大于等于 | `value >= threshold` | 否 |
| `LTE` | 小于等于 | `value <= threshold` | 否 |
| `DEVIATION_GT` | 偏差超阈值 | `abs((value - baseline) / baseline) * 100 > threshold` | 是 |
| `DEVIATION_LT_NEGATIVE` | 负偏差超阈值（只检测下降） | `(value - baseline) / baseline * 100 < -threshold` | 是 |
| `ABSENCE` | 数据缺失 | `value == null 或 value == 0` | 否 |

baseline 为 0 或 null 时，偏差类 operator 自动跳过不触发。

---

## 与现有组件集成

### Quartz 调度

```
Quartz Scheduler
├── Group: REPORT     → ReportScheduleJob     (已有)
├── Group: DATA_SYNC  → DataSyncJob           (已有)
└── Group: ALERT      → AlertCheckJob         (新增)
```

### 查询引擎

复用 `SemanticLayerService.queryByReq()`。`queryConfig` 解析逻辑从 `ReportExecutionOrchestrator` 提取为共享工具类 `QueryConfigParser`。

### 推送渠道

复用 `ReportDeliveryService.deliver()` + 五渠道策略模式。`DeliveryContext` 扩展：

```java
// 新增字段
private String alertContent;       // 非空时启用告警卡片模板
private String alertSeverity;      // 最高严重级别
private Integer alertedCount;      // 告警条数
private Integer totalChecked;      // 检查总行数
```

`FeishuDeliveryChannel.buildPayload()` 检查 `alertContent != null` 时走告警卡片分支。

---

## API 设计

资源前缀：`/api/v1/alertRules`

### 标准方法

| 方法 | URL | 功能 |
|------|-----|------|
| POST | `/api/v1/alertRules` | 创建告警规则 |
| GET | `/api/v1/alertRules` | 列表查询 |
| GET | `/api/v1/alertRules/{ruleId}` | 详情 |
| PATCH | `/api/v1/alertRules/{ruleId}` | 更新 |
| DELETE | `/api/v1/alertRules/{ruleId}` | 删除 |

### 自定义方法

| 方法 | URL | 功能 |
|------|-----|------|
| POST | `/api/v1/alertRules/{ruleId}:pause` | 暂停 |
| POST | `/api/v1/alertRules/{ruleId}:resume` | 恢复 |
| POST | `/api/v1/alertRules/{ruleId}:trigger` | 立即执行一次 |
| POST | `/api/v1/alertRules/{ruleId}:test` | 试运行（不推送） |

### 执行记录

| 方法 | URL | 功能 |
|------|-----|------|
| GET | `/api/v1/alertRules/{ruleId}/executions` | 执行记录列表 |
| GET | `/api/v1/alertEvents` | 全局告警事件（支持 ruleId/severity/日期过滤） |

### 创建请求示例

```json
{
  "name": "数据到达行数偏差告警",
  "datasetId": 42,
  "queryConfig": "...",
  "conditions": [
    {
      "metricField": "deviation_pct",
      "operator": "DEVIATION_LT_NEGATIVE",
      "threshold": 20.0,
      "baselineField": "avg_row_count_7d",
      "dimensionField": "partner_code",
      "severity": "WARNING",
      "messageTemplate": "⚠️ ${partner_name} ${data_date} 到达 ${row_count}，低于均值 ${avg_row_count_7d} 的 ${deviation_pct}%"
    },
    {
      "metricField": "row_count",
      "operator": "ABSENCE",
      "threshold": 0,
      "dimensionField": "partner_code",
      "severity": "CRITICAL",
      "messageTemplate": "🔴 ${partner_name} ${data_date} 数据未到达"
    }
  ],
  "cronExpression": "0 0 9 * * ?",
  "deliveryConfigIds": "1,3",
  "silenceMinutes": 1440
}
```

---

## 飞书告警卡片

### 有告警时推送

```
┌─────────────────────────────────────┐
│ 🚨 数据到达告警 — 2026-03-17        │  ← header: red
├─────────────────────────────────────┤
│ 规则: 数据到达行数偏差告警            │
│ 检查时间: 09:00:12                   │
│ 检查行数: 15 | 告警行数: 3           │
├─────────────────────────────────────┤
│ 🔴 CRITICAL                         │
│ DUXM-CL-X 2026-03-16 数据未到达     │
│                                     │
│ ⚠️ WARNING                          │
│ DUXM-CL-N 到达 1203，均值 1680，    │
│ 偏差 -28.4%                         │
│                                     │
│ ⚠️ WARNING                          │
│ DUXM-CL-B 到达 856，均值 1102，     │
│ 偏差 -22.3%                         │
├─────────────────────────────────────┤
│ ✅ 正常: 12 个合作方                  │
├─────────────────────────────────────┤
│ [查看详情]                           │
│ 静默期: 24h 内同一告警不重复发送      │
└─────────────────────────────────────┘
```

### 无告警时

**不推送**——避免"天天收到正常通知"导致告警疲劳。

### 卡片颜色

| 最高 Severity | Header 颜色 |
|--------------|------------|
| CRITICAL | 红色 (`red`) |
| WARNING | 橙色 (`orange`) |

---

## 实现步骤

| # | 任务 | 工期 | 依赖 | 产出文件 |
|---|------|------|------|---------|
| 1 | Flyway 迁移 V23（MySQL + PostgreSQL） | 0.5d | 无 | `V23__alert_subscription.sql` |
| 2 | DO + Mapper + Enum | 0.5d | 1 | AlertRuleDO, AlertExecutionDO, AlertEventDO |
| 3 | AlertEvaluator 评估引擎 | 1d | 2 | 纯函数，易测试 |
| 4 | AlertCheckDispatcher + AlertCheckJob | 1d | 3, 5 | 主链路编排 |
| 5 | QueryConfigParser 提取（从 Orchestrator 共享） | 0.5d | 无 | 报表+告警共用 |
| 6 | DeliveryContext 扩展 + 告警卡片分支 | 0.5d | 无 | FeishuDeliveryChannel 改动 |
| 7 | AlertRuleService CRUD + Quartz 集成 | 1d | 2 | 参照 ReportScheduleServiceImpl |
| 8 | AlertRuleController REST API | 0.5d | 7 | 参照 ReportScheduleController |
| 9 | 静默期去重逻辑 | 0.5d | 4 | alert_key 查询过滤 |
| 10 | 单元测试 | 1d | 3, 4, 9 | Evaluator + Dispatcher + 静默期 |
| **合计** | | **~7d** | | 任务 1/5/6 可并行 |

关键路径：1 → 2 → 3 → 4 → 9 → 10（4.5d）

---

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| 误报过多，用户关闭告警 | 首版只监控钉子户合作方；提供 `:test` 试运行；静默期默认 24h |
| 查询返回大量行导致评估慢 | queryConfig 应含合理 LIMIT；总超时 5 分钟 |
| baseline 不足（新合作方无历史） | DEVIATION 类 operator 在 baseline 为 0/null 时跳过 |
| Quartz 线程池争抢 | 使用独立 ALERT Group |

---

## 首个告警规则配置（数据到达监控）

基于 [discovery findings](../../product/02a-discovery-findings.md)，首版上线后配置：

- **合作方**: DUXM-CL-N
- **约定推送时间**: 19:00
- **告警检查时间**: 每日 21:00（Cron: `0 0 21 * * ?`）
- **条件 1**: 行数偏差 > 20% → WARNING
- **条件 2**: 行数 = 0（未到达）→ CRITICAL
- **条件 3**: 金额偏差 > 15% → WARNING
- **静默期**: 24 小时
- **推送**: 飞书群
- **观察期**: 1 周，根据误报率调整阈值
