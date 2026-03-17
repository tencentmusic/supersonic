---
status: implemented
module: headless/server
key-files:
  - headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/SemanticTemplateServiceImpl.java
  - headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/MetricServiceImpl.java
  - headless/server/src/main/java/com/tencent/supersonic/headless/server/facade/service/impl/S2SemanticLayerService.java
depends-on:
  - docs/details/report/01-template-version.md
  - docs/details/report/04-scheduler-delivery.md
---

# 治理与审计

## 目标

防止模板下线或 DataSet 删除时破坏正在运行的调度任务；记录执行历史以满足审计和成本分析需求；在指标变更时通知受影响的调度任务负责人。

## 当前状态

已上线。DataSet 删除级联校验、模板下线阻断（`offlineTemplate()` + Offline(2) 状态）、执行日志（`s2_report_execution`）均已实现。`MetricChangedEvent` 和 `ScheduleImpactListener` 尚未实现，标注为待定（独立迭代）。

## 设计决策

**影响分析优先于阻断**：对于 DataSet 删除和模板下线，采用阻断策略（有活跃调度则拒绝操作）；对于指标变更，采用通知策略（标记 warning，不阻断执行），因为指标变更的影响面更广，强阻断会影响正常运营。

**事件驱动**：指标变更通过 Spring `ApplicationEvent` 传播，遵循跨模块通信的项目约定，不使用反射或循环导入。

## 接口契约

```
POST /templates/{id}:offline
    → 下线模板（检查活跃调度任务后设为 Offline）
    → 如存在活跃调度，返回 400 + 错误提示（含活跃调度任务数量）

GET /api/v1/reportSchedules?filter=datasetId==xxx&enabled==true
    → 查询关联某 DataSet 的活跃调度任务（用于前端展示下线影响范围）
```

## 数据模型

### 审计字段（s2_report_execution）

| 审计字段 | 来源 | 说明 |
|---------|------|------|
| 执行人 | `owner_id` / 请求用户 | 定时任务取 owner，手动执行取当前用户 |
| 执行时间 | `start_time` / `end_time` | 精确到毫秒 |
| 参数 | `query_config` | JSON 序列化的完整查询参数 |
| SQL Hash | `sql_hash` | 生成的 SQL 的 MD5，用于变更追踪 |
| 返回数据量 | `row_count` | 结果行数 |
| 执行状态 | `status` | SUCCESS / FAILED + error_message |
| 执行上下文快照 | `execution_snapshot` | 完整 ReportExecutionContext（用于历史复现） |
| 模板版本 | `template_version` | 执行时的模板版本号 |
| 扫描行数 | `scan_rows` | 预估/实际扫描行数（用于成本分析） |
| 执行耗时 | `execution_time_ms` | 查询执行耗时（毫秒） |
| IO 读取 | `io_bytes` | IO 读取字节数（仅 ClickHouse 等支持的数据源） |

### MetricChangedEvent（待实现）

```java
@Data
public class MetricChangedEvent extends ApplicationEvent {
    private Long metricId;
    private String metricName;
    private ChangeType changeType;  // DELETED, MODIFIED, DISABLED
    private List<Long> affectedDataSetIds;

    public enum ChangeType { DELETED, MODIFIED, DISABLED }
}
```

## 实现要点

### 分析场景

| 分析场景 | 实现方式 | 触发点 |
|---------|---------|--------|
| DataSet 删除阻断 | 查询 `s2_report_schedule` 中关联该 DataSet 且 `enabled=true` 的调度任务 | `DataSetServiceImpl.delete()` |
| 模板下线阻断 | 下线前检查是否存在活跃调度任务，存在则拒绝下线 | `SemanticTemplateServiceImpl.offlineTemplate()` |
| Metric 变更通知 | 通过 DataSet → Model → Metric 关联链追踪，标记受影响调度任务 | `MetricChangedEvent`（事件驱动，待实现） |

### DataSet 删除前级联校验

```java
// DataSetServiceImpl.delete() — 增加拦截逻辑
@Override
public void delete(Long id, User user) {
    // 检查是否有活跃调度任务关联此 DataSet
    Page<ReportScheduleDO> page = reportScheduleService
        .getScheduleList(new Page<>(1, 1), id, /*enabled=*/ true);
    if (page.getTotal() > 0) {
        throw new InvalidArgumentException(
            "无法删除数据集：关联 " + page.getTotal() + " 个活跃调度任务，请先暂停或删除相关调度");
    }

    // 原有逻辑
    DataSetDO dataSetDO = getById(id);
    dataSetDO.setStatus(StatusEnum.DELETED.getCode());
    updateById(dataSetDO);
}
```

### 模板下线阻断

```java
// SemanticTemplateServiceImpl — 新增方法
@Transactional
public void offlineTemplate(Long id, User user) {
    SemanticTemplateDO templateDO = baseMapper.selectById(id);
    if (templateDO == null || !STATUS_DEPLOYED.equals(templateDO.getStatus())) {
        throw new InvalidArgumentException("Only deployed templates can be taken offline");
    }

    // 查询该模板部署生成的 DataSet，检查是否有活跃调度任务
    SemanticDeployResult deployResult = getLatestDeployResult(id);
    if (deployResult != null && deployResult.getDataSetId() != null) {
        Page<ReportScheduleDO> page = reportScheduleService
            .getScheduleList(new Page<>(1, 1), deployResult.getDataSetId(), true);
        if (page.getTotal() > 0) {
            throw new InvalidArgumentException(
                "无法下线：该模板关联 " + page.getTotal() + " 个活跃调度任务");
        }
    }

    templateDO.setStatus(STATUS_OFFLINE);  // 2
    baseMapper.updateById(templateDO);
}
```

模板状态流转：

```
Draft(0) → Deployed(1) → Offline(2)
                ↑              │
                └──────────────┘ （重新部署可回到 Deployed）
```

### MetricChangedEvent 处理（待实现）

```java
// headless/server 发布事件（待实现）
@Component
public class ScheduleImpactListener {
    @EventListener
    public void onMetricChanged(MetricChangedEvent event) {
        for (Long datasetId : event.getAffectedDataSetIds()) {
            Page<ReportScheduleDO> schedules = reportScheduleService
                .getScheduleList(new Page<>(1, 100), datasetId, true);
            schedules.getRecords().forEach(schedule -> {
                // 标记 warning 状态或发送通知（不阻断执行）
                log.warn("Schedule [{}] affected by metric change: {} {}",
                    schedule.getId(), event.getChangeType(), event.getMetricName());
            });
        }
    }
}
```

### 影响文件清单

| 文件 | 变更 | 状态 |
|------|------|------|
| `DataSetServiceImpl.java` | `delete()` 增加调度任务存在性检查 | ✅ 已实现 |
| `SemanticTemplateServiceImpl.java` | 新增 `STATUS_OFFLINE = 2` + `offlineTemplate()` | ✅ 已实现 |
| `SemanticTemplateService.java` | 接口新增 `offlineTemplate(Long id, User user)` | ✅ 已实现 |
| `SemanticTemplateController.java` | 新增 `POST /templates/{id}:offline` 端点 | ✅ 已实现 |
| `ReportScheduleServiceImpl.java` | `getScheduleList(datasetId, enabled)` | ✅ 已实现 |
| 新建 `MetricChangedEvent.java` | 指标变更事件 | 待实现（独立迭代） |
| 新建 `ScheduleImpactListener.java` | 调度影响监听器 | 待实现（独立迭代） |

## 待办

- `MetricChangedEvent` + `ScheduleImpactListener`（独立迭代，影响面较大）
- 前端展示"下线影响范围"：下线按钮点击后先展示受影响的调度任务列表，要求用户逐一暂停后才允许下线
- 执行成本报表：基于 `scan_rows` + `execution_time_ms` + `io_bytes` 构建报表成本排行榜
- 调度任务 warning 状态在前端可视化展示（指标变更后的受影响标注）
