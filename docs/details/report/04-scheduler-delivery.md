---
status: implemented
module: headless/server
key-files:
  - headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/ReportScheduleDispatcher.java
  - headless/server/src/main/java/com/tencent/supersonic/headless/server/pojo/ReportScheduleDO.java
depends-on:
  - docs/details/report/02-execution-engine.md
---

# 调度与投递

## 目标

支持动态 Cron 调度、集群防重复执行、失败指数退避重试，并通过多渠道策略模式（Email / Feishu / Webhook / DingTalk / WechatWork）将报表结果可靠投递到外部渠道。

## 当前状态

已上线。Quartz JDBC JobStore、`ReportScheduleJob`、`ReportScheduleDispatcher`、`ReportDeliveryService` 五渠道、推送幂等、指数退避重试、连续失败自动禁用均已实现。前端调度管理页面和推送配置管理页面均已实现。

## 设计决策

### 调度引擎选型：Quartz（内嵌）

| 方案 | 动态 Cron | 持久化 | 集群防重复 | 重试/Misfire | 额外基础设施 | Spring Boot 3 |
|------|----------|--------|-----------|-------------|-------------|--------------|
| Spring `@Scheduled` + DB 轮询 | 自行实现 | 自行实现 | 需加 ShedLock | 自行实现 | 无 | 原生 |
| **Quartz（内嵌）** | **内置** | **内置（JDBC JobStore）** | **内置（DB 行锁）** | **内置 Misfire 策略** | **无** | **原生支持** |
| XXL-JOB（内嵌） | 内置 | 内置 | 内置 | 内置 | 无 | **不兼容**（javax） |

选择 Quartz 的原因：Spring Boot 3.x 原生支持 `spring-boot-starter-quartz`，零额外基础设施；JDBC JobStore 重启不丢失；内置集群模式（DB 行锁协调）；内置 Misfire 策略（停机恢复后可自动补跑）；内嵌在 SuperSonic 进程中，部署形态不变。

Quartz 仅用于**用户自定义的报表调度**，现有系统级 `@Scheduled` 任务保持不变，两者互不干扰：

| 调度类型 | 框架 | 管理方式 |
|---------|------|---------|
| 系统内部任务（固定周期） | Spring `@Scheduled` | 硬编码在代码中 |
| 用户报表调度（动态 Cron） | Quartz Scheduler | 数据库持久化 + Web 管理界面 |

### Job 轻量分派原则

Quartz Job 保持轻量，只负责分派；所有业务逻辑统一由 `ReportExecutionOrchestrator` 处理。

### 推送渠道策略模式

定义 `ReportDeliveryChannel` 接口，各渠道独立实现，通过 `@Component` 注册，运行时按 `delivery_type` 查找对应实现，新增渠道只需实现接口，无需修改现有代码。

### 投递幂等

通过 `delivery_key = schedule_id + execution_time + channel` 唯一标识每次投递，投递前检查 key 是否已存在，存在则跳过，防止重复推送。

### 连续失败自动禁用

`consecutiveFailures` 计数，超阈值后自动禁用推送配置，防止持续推送无效渠道。通过 `DeliveryRetryTask` 定时任务执行指数退避自动重试（重试间隔 1→2→4→8→16 分钟，最多 5 次）。

## 接口契约

### 调度配置 REST API

**标准方法**：

| 方法 | URL | 功能 |
|------|-----|------|
| POST | `/api/v1/reportSchedules` | 创建调度任务 |
| GET | `/api/v1/reportSchedules` | 查询调度列表（?pageSize=20&pageToken=...&filter=enabled==true） |
| GET | `/api/v1/reportSchedules/{scheduleId}` | 查询单个调度详情 |
| PATCH | `/api/v1/reportSchedules/{scheduleId}` | 部分更新调度配置（自动同步 Quartz Trigger） |
| DELETE | `/api/v1/reportSchedules/{scheduleId}` | 删除调度任务（同步删除 Quartz Job） |

**自定义方法**：

| 方法 | URL | 功能 |
|------|-----|------|
| POST | `/api/v1/reportSchedules/{scheduleId}:pause` | 暂停调度 |
| POST | `/api/v1/reportSchedules/{scheduleId}:resume` | 恢复调度 |
| POST | `/api/v1/reportSchedules/{scheduleId}:trigger` | 立即执行一次 |

**执行记录子资源**：

| 方法 | URL | 功能 |
|------|-----|------|
| GET | `/api/v1/reportSchedules/{scheduleId}/executions` | 查询执行记录 |
| GET | `/api/v1/reportSchedules/{scheduleId}/executions/{executionId}` | 查询单条执行详情 |
| GET | `/api/v1/reportSchedules/{scheduleId}/executions/{executionId}:download` | 下载执行结果文件 |

### 推送配置 REST API

| 方法 | URL | 功能 |
|------|-----|------|
| POST | `/api/v1/reportDeliveryConfigs` | 创建推送配置 |
| GET | `/api/v1/reportDeliveryConfigs` | 配置列表 + 统计接口 |
| GET/PATCH/DELETE | `/api/v1/reportDeliveryConfigs/{configId}` | 标准 CRUD |
| POST | `/api/v1/reportDeliveryConfigs/{configId}:test` | 测试推送 |

## 数据模型

### s2_report_schedule（报表调度配置 — 业务元数据）

```
s2_report_schedule
├── id
├── name                  → 调度任务名称
├── dataset_id            → 关联 DataSet
├── query_config          → JSON：QueryStructReq 模板（含参数默认值）
├── output_format         → EXCEL / CSV / JSON
├── cron_expression       → Cron 表达式（同步到 Quartz Trigger）
├── enabled               → 是否启用
├── owner_id              → 权限归属用户
├── retry_count           → 最大重试次数（默认 3）
├── retry_interval        → 重试间隔基数（秒，默认 30，指数退避）
├── template_version      → 绑定的模板版本（模板升级不影响已有调度）
├── delivery_config_ids   → 关联的推送渠道配置（逗号分隔 ID）
├── quartz_job_key        → Quartz Job 标识（{group}.{name}，自动生成）
├── last_execution_time
├── next_execution_time
├── created_at / updated_at / created_by / tenant_id
```

> `s2_report_schedule` 是业务配置，`QRTZ_*` 表是调度引擎状态。两者通过 `quartz_job_key` 关联。

### s2_report_execution（执行记录 — 审计日志）

```
s2_report_execution
├── id
├── schedule_id           → 关联调度任务
├── attempt               → 当前执行次数（1=首次，2=第一次重试...）
├── status                → PENDING / RUNNING / SUCCESS / FAILED
├── start_time / end_time
├── result_location       → 结果文件路径
├── error_message
├── row_count             → 返回数据量
├── sql_hash              → 执行 SQL 的 MD5（用于审计）
├── execution_snapshot    → JSON：完整的 ReportExecutionContext 快照（用于历史复现）
├── template_version      → 执行时的模板版本号
├── engine_version        → 执行时的系统版本号
├── scan_rows             → 预估/实际扫描行数（来自 EXPLAIN 或执行统计）
├── execution_time_ms     → 查询执行耗时（毫秒）
├── io_bytes              → IO 读取字节数（仅 ClickHouse 等支持的数据源）
└── tenant_id
```

> `execution_snapshot` + `template_version` + `scan_rows` + `execution_time_ms` 字段设计目标：历史复现（回放执行参数和权限）、成本分析（构建报表成本排行榜）、风险审计（追踪系统状态变化）。

### s2_report_delivery_config（推送配置）

```
s2_report_delivery_config
├── id
├── report_schedule_id    → 关联调度任务
├── delivery_type         → EMAIL / WEBHOOK / FEISHU / DINGTALK / WECHAT_WORK
├── delivery_config       → JSON（收件人、Webhook URL 等渠道专属配置）
└── tenant_id
```

### s2_report_delivery_record（推送记录）

```
s2_report_delivery_record
├── id
├── delivery_config_id
├── execution_id
├── delivery_key          → 唯一标识（schedule_id + execution_time + channel）
├── status                → SUCCESS / FAILED / RETRYING
├── consecutive_failures  → 连续失败次数（超阈值自动禁用）
├── error_message
└── tenant_id
```

## 实现要点

### Quartz 配置

```yaml
spring:
  quartz:
    job-store-type: jdbc
    jdbc:
      initialize-schema: always
    properties:
      org.quartz.scheduler.instanceName: SuperSonicScheduler
      org.quartz.scheduler.instanceId: AUTO
      org.quartz.jobStore.isClustered: true
      org.quartz.jobStore.clusterCheckinInterval: 15000
      org.quartz.jobStore.misfireThreshold: 60000
      org.quartz.threadPool.threadCount: 5
```

> Quartz JDBC JobStore 会自动创建 11 张 `QRTZ_*` 表。`initialize-schema: always` 在首次启动时自动建表，后续启动会跳过已存在的表。

线程组隔离：

```
Quartz Scheduler（统一调度引擎）
├── Group: REPORT     → ReportScheduleJob（报表定时执行）  threadCount=5
├── Group: DATA_SYNC  → DataSyncJob（数据同步任务）        threadCount=2
└── Group: ...        → 未来可扩展其他调度类型
```

### ReportScheduleJob（轻量分派器）

```java
@Slf4j
public class ReportScheduleJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long scheduleId = context.getMergedJobDataMap().getLong("scheduleId");
        Long tenantId = context.getMergedJobDataMap().getLong("tenantId");

        TenantContext.setTenantId(tenantId);
        try {
            ReportScheduleDispatcher dispatcher =
                    ContextUtils.getBean(ReportScheduleDispatcher.class);
            dispatcher.dispatch(scheduleId);
        } finally {
            TenantContext.clear();
        }
    }
}
```

### ReportScheduleDispatcher（指数退避重试）

```java
/**
 * retryInterval 为基数（秒），实际间隔 = base * 2^(attempt-1)。
 * 例如 retryInterval=30: 首次重试等 30s，第二次 60s，第三次 120s。
 */
private void executeWithRetry(ReportSchedule schedule) {
    int maxAttempts = schedule.getRetryCount() + 1;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
        try {
            ReportExecutionContext ctx = contextBuilder.buildFromSchedule(schedule, attempt);
            orchestrator.execute(ctx);  // 统一编排入口
            break;
        } catch (Exception e) {
            if (attempt < maxAttempts) {
                long delay = schedule.getRetryInterval() * (1L << (attempt - 1));
                Thread.sleep(delay * 1000);
            }
        }
    }
}
```

### 调度创建核心流程

```java
public void createSchedule(ReportSchedule schedule) {
    // 1. 写入 s2_report_schedule
    scheduleMapper.insert(schedule);

    // 2. 创建 Quartz Job
    JobDetail job = JobBuilder.newJob(ReportScheduleJob.class)
            .withIdentity("report_" + schedule.getId(), "REPORT")
            .usingJobData("scheduleId", schedule.getId())
            .usingJobData("tenantId", schedule.getTenantId())
            .storeDurably()
            .build();

    // 3. 创建 Cron Trigger（Misfire 策略：错过后立即补执行一次）
    CronTrigger trigger = TriggerBuilder.newTrigger()
            .withSchedule(CronScheduleBuilder
                    .cronSchedule(schedule.getCronExpression())
                    .withMisfireHandlingInstructionFireAndProceed())
            .build();

    scheduler.scheduleJob(job, trigger);
}
```

### 权限模型（定时报表）

定时报表执行时无实时用户上下文，权限规则：

- **报表创建者**的权限作为执行时的权限基线
- 报表消费者（订阅人）只能查看创建者权限范围内的数据
- 权限变更后，定时报表需重新校验

实现方式：`s2_report_schedule` 中记录 `owner_id`，执行时以 owner 身份构建 `User` 对象，通过 `ReportExecutionContextBuilder` 构建权限快照，注入 `S2DataPermissionAspect`。

### 动作型操作治理

| 约束项 | 要求 |
|-------|------|
| 幂等 | 调度触发、结果投递、失败重试都应具备唯一业务键，避免重复执行和重复发送 |
| 审计 | 记录操作人、入口、目标模板/任务/渠道、执行结果 |
| 确认 | 高风险操作默认需二次确认，如批量重发、大范围外部推送 |
| 权限校验 | 在执行前同时校验报表执行权限和外部分发权限，而不只校验数据权限 |

权限分层：

| 权限层级 | 说明 | 典型控制点 |
|---------|------|-----------|
| **数据访问权限** | 控制能查哪些库表、字段、维度范围 | 行级过滤、字段脱敏、租户隔离 |
| **报表执行权限** | 控制谁可执行模板、创建调度、重跑任务、导出结果 | 模板执行、调度管理、导出任务取消/重试 |
| **外部分发权限** | 控制谁可把结果发到外部渠道 | 飞书群、邮件收件人、Webhook、公开分享 |

### 前端页面规划

```
src/pages/ReportSchedule/
├── index.tsx              → 调度任务列表页（主页面）
├── components/
│   ├── ScheduleForm.tsx   → 创建/编辑调度任务表单（Modal）
│   ├── CronInput.tsx      → Cron 表达式输入组件（可视化 + 手动输入双模式）
│   ├── ExecutionList.tsx  → 执行记录列表（抽屉/子页面）
│   └── ExecutionDetail.tsx → 执行详情（状态、耗时、错误信息、下载链接）
└── service.ts             → API 调用层

src/pages/DeliveryConfig/
├── index.tsx              → 推送配置管理（Tabs：统计仪表盘 + 配置列表）
└── components/
    └── ConfigForm.tsx     → 动态渲染渠道专属字段
```

**CronInput 组件**（`CronInput.tsx`，可复用）提供两种模式：

- **简易模式**：下拉选择"每天 / 每周X / 每月X日" + 时间选择器，自动生成 Cron 表达式
- **高级模式**：直接输入 Cron 表达式，实时预览下次 5 次执行时间

**调度任务列表列**：任务名称 / 关联数据集 / Cron 表达式（悬浮显示人类可读描述）/ 输出格式 / 状态（Switch）/ 上次执行 / 下次执行 / 操作（编辑 / 立即执行 / 执行记录 / 删除）

**创建/编辑表单字段**：任务名称 / 关联数据集 / 查询参数 / Cron 表达式 / 输出格式 / 重试次数（默认 3）/ 重试间隔（秒）/ 推送渠道（多选）

## 待办

- 推送配置管理页面：自动禁用配置的恢复流程 UI
- `MetricChangedEvent` → 调度影响通知（`ScheduleImpactListener`，独立迭代）
- 批量重发确认弹窗（高风险操作二次确认）
- Misfire 补跑记录在执行历史中的标注（当前仅日志记录）
