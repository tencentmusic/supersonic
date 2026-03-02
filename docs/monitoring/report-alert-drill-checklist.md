# 模板报表 P0 告警演练清单

本文用于测试环境演练 `docker/prometheus/rules/report-slo-alert-rules.yml` 中的 5 条告警，确保“告警可触发、可定位、可处置、可恢复”。

## 一、演练前准备

- 监控链路可用：
  - `actuator/prometheus` 可抓取
  - Prometheus 规则已加载
  - Alertmanager 路由到值班渠道
- 仪表盘已导入：
  - `docker/grafana/dashboards/template-report-slo-dashboard.json`
- 准备 2 个测试租户：
  - `tenantA`：用于故障注入
  - `tenantB`：用于验证隔离（不受影响）

## 二、告警演练用例

### 用例 1：执行成功率低于 95%

- 目标告警：`ReportExecutionSuccessRateLow`
- 注入方式：
  - 在 `tenantA` 创建调度任务，配置错误 SQL 或无权限 DataSet，使连续执行失败。
- 预期：
  - 5~10 分钟后成功率下降，触发告警。
  - `supersonic_report_execution_total{result="error"}` 明显上升。
- 处置动作：
  - 暂停异常调度任务，修复 SQL/权限配置，恢复后观察 30 分钟。

### 用例 2：执行 P95 时延超过 10 秒

- 目标告警：`ReportExecutionP95High`
- 注入方式：
  - 构造大范围查询（高扫描行数），并提高并发触发。
- 预期：
  - `histogram_quantile(0.95, ...execution_duration...)` 持续 > 10s。
- 处置动作：
  - 临时降低并发，限制查询范围，必要时转异步导出。

### 用例 3：导出 pending 堆积

- 目标告警：`ReportExportPendingBacklogHigh`
- 注入方式：
  - 批量提交导出任务，超过导出线程池处理能力。
- 预期：
  - `supersonic_report_export_pending` 持续高于阈值（默认 100）。
- 处置动作：
  - 扩容导出线程池或限流提交入口，清理失效任务。

### 用例 4：推送失败率超过 5%

- 目标告警：`ReportDeliveryFailureRateHigh`
- 注入方式：
  - 将测试推送渠道改为无效 URL/密钥，触发连续推送失败。
- 预期：
  - `supersonic_report_delivery_total{result="error"}` 占比持续升高。
- 处置动作：
  - 切换备用渠道或修复配置，重放失败记录（人工 retry）。

### 用例 5：推送配置自动禁用

- 目标告警：`ReportDeliveryConfigAutoDisabled`
- 注入方式：
  - 让某一 delivery config 连续失败达到阈值（默认 5 次）。
- 预期：
  - `supersonic_report_delivery_config_disabled > 0`，告警触发。
- 处置动作：
  - 修复配置后手动启用并验证一条测试推送成功。

## 三、演练记录模板

每次演练建议记录：

- 演练编号/时间/值班人
- 注入动作
- 告警触发时间（TTD）
- 定位与止损时间（TTA）
- 恢复时间（TTR）
- 根因与改进项

## 四、通过标准（P0）

- 5 条告警至少覆盖 3 条完整演练通过（建议全覆盖）。
- 每条演练都具备：
  - 告警截图
  - 处置记录
  - 恢复后 30 分钟观察结论
- 不影响非演练租户（`tenantB`）核心链路。
