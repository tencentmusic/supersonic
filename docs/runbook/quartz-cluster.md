---
status: active
module: launchers/standalone
audience: [developer, ops]
last-updated: 2026-04-17
---

# Quartz 集群调度运维 Runbook

本手册覆盖 JDBC JobStore + 集群模式下的启停、扩缩容、故障恢复流程。
前置条件：已完成 P0-1（Quartz 迁移到 JDBC JobStore + isClustered=true）。

---

## 1. 架构速览

- JobStore：Spring `LocalDataSourceJobStore`（基于 `JobStoreTX`，Spring 事务集成版本）
- 集群协调：通过 `QRTZ_LOCKS` 表行锁 + `QRTZ_SCHEDULER_STATE` 心跳
- Check-in 频率：20 秒（`clusterCheckinInterval=20000`）
- 失败恢复时间：最长 20 秒（检测到节点失效后其它节点接管）
- 持久化位置：SuperSonic 主数据库（MySQL 或 PostgreSQL），`QRTZ_*` 表（V29 迁移创建）

## 2. 启动检查

节点启动后日志里必须能看到以下两行（缺一不可）：

```
Using job-store 'org.springframework.scheduling.quartz.LocalDataSourceJobStore' - which supports persistence. and is clustered.
Scheduler SuperSonicScheduler_$_<host><timestamp> started.
```

如果日志显示 `RAMJobStore` 或 `is not clustered`，立刻排查：
1. `spring.profiles.active` 是否包含 `mysql` 或 `postgres`
2. `application.yaml` 的 `spring.quartz.job-store-type: jdbc` 是否被覆盖
3. `org.quartz.jobStore.isClustered: true` 是否在 profile yaml 中被改为 false

## 3. 扩容（新增节点）

1. 确认新节点使用与现有节点**相同的数据库连接串**
2. 确认新节点的 `org.quartz.scheduler.instanceName=SuperSonicScheduler`（必须一致）
3. 确认新节点的 `org.quartz.scheduler.instanceId=AUTO`（会自动生成唯一 ID）
4. 启动新节点。20 秒内应能在 `QRTZ_SCHEDULER_STATE` 表看到新行：

```sql
SELECT instance_name, last_checkin_time, checkin_interval
FROM QRTZ_SCHEDULER_STATE
WHERE sched_name = 'SuperSonicScheduler';
```

期望：每个在线节点一行，`last_checkin_time` 在近 20 秒内（`UNIX_TIMESTAMP()*1000 - last_checkin_time < 40000`）。

## 4. 缩容 / 滚动重启

Quartz 通过心跳超时（2× checkInInterval ≈ 40 秒）判定节点失效。优雅下线流程：

1. 从 LB 摘除节点，停止新流量
2. 调用 `SIGTERM`（Spring Boot 30 秒优雅关闭，已在 `application.yaml` 配置）
3. 正在执行的 Job 会继续跑完（`wait-for-jobs-to-complete-on-shutdown: true`）
4. 心跳停止后约 40 秒，存活节点将此前该节点"未完成但已开火"的 trigger 标记为 `MISFIRED`，按 `misfireHandlingInstruction` 恢复（默认 `fireAndProceed`，即立刻补跑一次）

## 5. 故障：某个节点心跳长期不更新

现象：`QRTZ_SCHEDULER_STATE` 里某行 `last_checkin_time` 落后 > 60 秒，但进程仍活着。

排查：
1. 看该节点 GC 日志 —— Full GC 长时间停顿会导致心跳线程饿死
2. 看数据库连接池：`HikariPool-1 stats`，若 active=max 说明连接耗尽
3. 看 `QRTZ_LOCKS` 表是否有 `TRIGGER_ACCESS` 行被长期持有

临时止损：
```sql
-- 强制删掉僵死实例的心跳行，让其它节点认为它已下线
DELETE FROM QRTZ_SCHEDULER_STATE
WHERE sched_name = 'SuperSonicScheduler'
  AND instance_name = '<僵死节点 instanceId>';
```

> **警告**：仅在确认该节点不会再触发 job 时执行（最好先 kill -9 该进程）。

## 6. 故障：Job 在多节点重复触发

**这不应该发生**。如果发生，立刻走 P0 故障流程：

1. 查 `QRTZ_FIRED_TRIGGERS`：

```sql
SELECT trigger_name, instance_name, fired_time
FROM QRTZ_FIRED_TRIGGERS
WHERE trigger_name = '<出事的 trigger>'
ORDER BY fired_time DESC
LIMIT 10;
```

若同一 `sched_time` 出现多行且 `instance_name` 不同 → 集群协调失败。常见原因：
- 多个节点配置了不同的 `instanceName`（必须相同！）
- 数据库主从延迟，`QRTZ_LOCKS` 的 `FOR UPDATE` 跨主从
- `isClustered=false` 被误配置

2. 紧急止损：停到只剩一个节点，排查完再恢复。

## 7. 多租户安全性（已审计）

所有 Quartz `Job` 实现均从 `JobDataMap` 读 `tenantId`，并在 `finally` 中 `TenantContext.clear()`。故障节点上已获取但未完成的 trigger 被其它节点 recover 时会重新反序列化 `JobDataMap`，租户上下文不跨节点泄漏。

审计表：

| Job 类 | 文件 | tenantId 来源 | clear() 在 finally | 恢复安全 |
|--------|------|---------------|---------------------|---------|
| ReportScheduleJob | headless/server/.../task/ReportScheduleJob.java | JobDataMap.get("tenantId") | 是 | 是 |
| AlertCheckJob | headless/server/.../task/AlertCheckJob.java | JobDataMap.getLong("tenantId") | 是 | 是 |
| ConnectionSyncJob | headless/server/.../task/ConnectionSyncJob.java | JobDataMap.getLong("tenantId") | 是 | 是 |
| DataSyncJob (已废弃) | headless/server/.../task/DataSyncJob.java | JobDataMap.getLong("tenantId") | 是 | 是 |

> **新增 Job 必须遵守**：`tenantId` 放 `JobDataMap`，`execute()` 入口 `setTenantId`，`finally` 里 `clear()`。禁止依赖调用线程的 ThreadLocal，因为恢复时 Job 跑在不同节点的 Quartz 工作线程上。

## 8. TenantSqlInterceptor 豁免

Quartz 的 `QRTZ_*` 表没有 `tenant_id` 列。`common/.../mybatis/TenantSqlInterceptor.java#shouldExcludeTable` 在 V29 之后会对表名以 `QRTZ_` 开头的大小写不敏感地跳过租户条件注入。**新增以 QRTZ 开头的业务表是禁忌**（会绕过租户隔离）；如需使用该前缀，必须改造拦截器。

## 9. Rollback 方案

**快速 rollback（保留数据，禁用集群）**：设置环境变量
```
SPRING_QUARTZ_PROPERTIES_ORG_QUARTZ_JOBSTORE_ISCLUSTERED=false
```
重启单个实例即可。数据保留在 QRTZ_* 表中，其他节点不影响。

**中级 rollback（回退到 RAMJobStore）**：设置
```
SPRING_QUARTZ_JOB_STORE_TYPE=memory
```
重启后当前调度内存中重建（`ReportScheduleServiceImpl`、`AlertRuleServiceImpl`、`ConnectionServiceImpl` 的启动钩子会重建全部触发器）。历史状态丢失，但服务恢复。

**完整 rollback（清除 QRTZ_* 表）**：仅在表数据损坏时使用：
```sql
-- MySQL
DROP TABLE IF EXISTS QRTZ_FIRED_TRIGGERS, QRTZ_PAUSED_TRIGGER_GRPS,
    QRTZ_SCHEDULER_STATE, QRTZ_LOCKS, QRTZ_SIMPLE_TRIGGERS,
    QRTZ_SIMPROP_TRIGGERS, QRTZ_CRON_TRIGGERS, QRTZ_BLOB_TRIGGERS,
    QRTZ_TRIGGERS, QRTZ_JOB_DETAILS, QRTZ_CALENDARS;

DELETE FROM flyway_schema_history WHERE version = '29';
```
> 警告：此操作销毁所有调度状态，仅在节点全部停止后执行。
```
