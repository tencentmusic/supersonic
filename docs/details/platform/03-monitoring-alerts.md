---
status: partial
module: common
key-files:
  - docker/prometheus/rules/report-slo-alert-rules.yml
  - docker/grafana/dashboards/template-report-slo-dashboard.json
depends-on: []
---

# 监控、告警与 SLO 设计

主文档：[智能运营数据中台设计方案](../../智能运营数据中台设计方案.md)
告警演练清单：[monitoring/report-alert-drill-checklist.md](../../monitoring/report-alert-drill-checklist.md)

## 1. 文档目标

本文承接平台架构中的**监控与告警**实现细节，重点回答：

- Prometheus 指标暴露与采集
- Grafana 仪表盘结构
- 报表执行 SLO 定义与告警规则
- 多租户告警隔离策略

## 2. 指标体系

### 2.1 暴露端点

SuperSonic 通过 Spring Boot Actuator + Micrometer 暴露 Prometheus 格式指标：

```
GET /actuator/prometheus
```

配置：
```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus,health,info
  metrics:
    export:
      prometheus:
        enabled: true
```

### 2.2 核心指标列表

| 指标名 | 类型 | 标签 | 说明 |
|--------|------|------|------|
| `supersonic_report_execution_total` | Counter | `result` (success/error), `tenant_id`, `template_id` | 报表执行次数 |
| `supersonic_report_execution_duration_seconds` | Histogram | `tenant_id`, `template_id` | 报表执行时长 |
| `supersonic_report_export_pending` | Gauge | `tenant_id` | 导出任务 pending 数 |
| `supersonic_report_delivery_total` | Counter | `result` (success/error), `channel`, `tenant_id` | 报表推送次数 |
| `supersonic_report_delivery_config_disabled` | Gauge | `tenant_id` | 因连续失败被自动禁用的推送配置数 |
| `supersonic_nl2sql_latency_seconds` | Histogram | `parser_type`, `tenant_id` | NL2SQL 解析时延 |
| `supersonic_query_cache_hit_total` | Counter | `cache_type` | 查询缓存命中次数 |

### 2.3 多租户指标隔离

所有业务指标强制携带 `tenant_id` 标签，Prometheus 告警规则按租户聚合，避免一个租户的异常影响平台整体告警。

## 3. Grafana 仪表盘

### 3.1 仪表盘文件

| 文件 | 内容 |
|------|------|
| `docker/grafana/dashboards/template-report-slo-dashboard.json` | 模板报表 SLO 仪表盘 |

### 3.2 核心面板结构

```
模板报表 SLO 仪表盘
├── 1. 执行成功率（7 天滚动，按租户分组）
├── 2. 执行 P50/P95/P99 时延
├── 3. 导出任务 Pending 趋势
├── 4. 推送成功率（按渠道分组）
├── 5. 被自动禁用的推送配置数
└── 6. NL2SQL 平均时延趋势
```

### 3.3 导入方式

```bash
# Docker Compose 启动时自动挂载
docker compose up -d

# 或手动导入
# Grafana UI → Dashboards → Import → 上传 JSON 文件
```

## 4. 告警规则（SLO）

### 4.1 规则文件

```
docker/prometheus/rules/report-slo-alert-rules.yml
```

### 4.2 P0 告警（5 条）

**告警 1：执行成功率低于 95%**

```yaml
alert: ReportExecutionSuccessRateLow
expr: |
  (
    sum(rate(supersonic_report_execution_total{result="success"}[5m])) by (tenant_id)
    /
    sum(rate(supersonic_report_execution_total[5m])) by (tenant_id)
  ) < 0.95
for: 5m
labels:
  severity: critical
annotations:
  summary: "租户 {{ $labels.tenant_id }} 报表执行成功率低于 95%"
```

**告警 2：执行 P95 时延超过 10 秒**

```yaml
alert: ReportExecutionP95High
expr: |
  histogram_quantile(0.95,
    sum(rate(supersonic_report_execution_duration_seconds_bucket[10m])) by (le, tenant_id)
  ) > 10
for: 5m
labels:
  severity: warning
```

**告警 3：导出 pending 堆积**

```yaml
alert: ReportExportPendingBacklogHigh
expr: supersonic_report_export_pending > 100
for: 5m
labels:
  severity: warning
```

**告警 4：推送失败率超过 5%**

```yaml
alert: ReportDeliveryFailureRateHigh
expr: |
  (
    sum(rate(supersonic_report_delivery_total{result="error"}[5m])) by (tenant_id, channel)
    /
    sum(rate(supersonic_report_delivery_total[5m])) by (tenant_id, channel)
  ) > 0.05
for: 5m
labels:
  severity: warning
```

**告警 5：推送配置自动禁用**

```yaml
alert: ReportDeliveryConfigAutoDisabled
expr: supersonic_report_delivery_config_disabled > 0
for: 1m
labels:
  severity: critical
```

### 4.3 SLO 定义

| SLO | 目标 | 告警阈值 | 滚动窗口 |
|-----|------|---------|---------|
| 执行成功率 | ≥ 99% | < 95% 触发告警 | 7 天 |
| 执行 P95 时延 | ≤ 5s | > 10s 触发告警 | 10 分钟 |
| 导出任务 pending | ≤ 50 | > 100 触发告警 | 即时 |
| 推送成功率 | ≥ 98% | < 95% 触发告警 | 5 分钟 |

## 5. Alertmanager 路由

### 5.1 推荐路由配置

```yaml
route:
  group_by: ['tenant_id', 'alertname']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
  receiver: 'default'
  routes:
    - match:
        severity: critical
      receiver: 'oncall-feishu'
    - match:
        severity: warning
      receiver: 'team-feishu'

receivers:
  - name: 'oncall-feishu'
    webhook_configs:
      - url: 'https://open.feishu.cn/open-apis/bot/v2/hook/{{ONCALL_WEBHOOK_ID}}'
  - name: 'team-feishu'
    webhook_configs:
      - url: 'https://open.feishu.cn/open-apis/bot/v2/hook/{{TEAM_WEBHOOK_ID}}'
```

### 5.2 租户隔离原则

- `group_by` 必须包含 `tenant_id`，避免跨租户告警合并
- Critical 告警路由到值班渠道，Warning 告警路由到团队渠道
- 同一告警最多每 4 小时重复发送一次

## 6. 告警演练

演练清单与通过标准见 [report-alert-drill-checklist.md](../../monitoring/report-alert-drill-checklist.md)。

5 条 P0 告警的演练要求：
- 至少覆盖 3 条完整演练通过（建议全覆盖）
- 每条演练需有告警截图、处置记录、恢复后 30 分钟观察结论
- 不影响非演练租户核心链路

## 7. 实现状态

| 能力 | 状态 |
|------|------|
| Actuator + Prometheus 端点 | 已上线 |
| SLO 告警规则（5 条）| 已上线 |
| Grafana 仪表盘 JSON | 已上线 |
| Alertmanager 飞书路由 | 已上线 |
| 多租户指标 `tenant_id` 标签 | 已在主干（部分指标待补全） |
| NL2SQL 时延直方图 | 待开发 |
| 导出任务 pending 指标 | 待开发（随 ExportTaskService 实现） |
| 推送成功率指标 | 待开发（随 DeliveryService 实现） |
