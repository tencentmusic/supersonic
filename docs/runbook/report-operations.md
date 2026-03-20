---
status: active
module: headless/server
audience: [developer, ops]
last-updated: 2026-03-18
---

# 模板报表子系统运维 Runbook

本手册覆盖模板报表子系统四类高频故障场景的排查与止损流程。面向两类读者：
- **开发人员**：能查看日志、读懂 Java 异常堆栈、修改配置文件
- **运维人员**：只需执行 SQL 命令和 curl 命令，不需要读代码

所有 SQL 使用 MySQL 语法，可直接复制粘贴执行。
API 调用使用 curl，需将 `BASE_URL`、`AUTH_TOKEN`、`{id}` 替换为实际值。

---

## 场景 1：调度失败

**紧急程度**：高（影响定时报表准时交付）
**预计排查时间**：10~20 分钟

### 现象

- 调度任务未按 cron 预期时间触发
- `s2_report_execution` 中出现 `status = 'FAILED'` 或 `status = 'PENDING'` 长时间不变
- 监控告警 `ReportExecutionSuccessRateLow` 触发（成功率低于 95%）

### 排查步骤

#### 步骤 1：查最近失败的执行记录

```sql
-- 查询过去 1 小时内失败或长时间 PENDING 的执行记录
SELECT
    id,
    schedule_id,
    status,
    attempt,
    start_time,
    end_time,
    execution_time_ms,
    LEFT(error_message, 500) AS error_snippet
FROM s2_report_execution
WHERE status IN ('FAILED', 'PENDING')
  AND start_time >= NOW() - INTERVAL 1 HOUR
ORDER BY start_time DESC
LIMIT 20;
```

**判断依据**：
- `status = 'FAILED'`，`error_message` 非空 → 看错误信息，进入步骤 2
- `status = 'PENDING'`，`start_time` 是 NULL → Quartz Job 未触发，进入步骤 3
- `status = 'PENDING'`，`start_time` 非空但 `end_time` 为 NULL，且已超过 10 分钟 → 执行卡住，进入步骤 4

#### 步骤 2：分析 error_message

```sql
-- 查看指定执行记录的完整错误信息
SELECT
    id,
    schedule_id,
    status,
    attempt,
    start_time,
    end_time,
    execution_time_ms,
    error_message,
    execution_snapshot
FROM s2_report_execution
WHERE id = {execution_id};
```

常见错误模式对照：

| error_message 关键词 | 根因 | 处理方向 |
|---------------------|------|---------|
| `Connection refused` / `Communications link failure` | 数据源不可用 | 检查数据库连接配置 |
| `Query timeout` / `execution_time_ms > 600000` | 查询超时（REPORT 池默认 600 秒） | 优化 SQL 或调整超时参数 |
| `Template is not deployed` / `template_version` 相关 | 模板未发布或已下线 | 检查模板状态，见步骤 5 |
| `Pool wait timeout` / `maxWait exceeded` | 连接池耗尽 | 检查连接池使用情况，见步骤 4 |
| `Variable ... is required` | SqlTemplateConfig 参数缺失 | 检查调度任务的参数配置 |

#### 步骤 3：检查 Quartz 调度状态

查看调度配置是否正常：

```sql
-- 检查调度任务基本配置
SELECT
    id,
    name,
    cron_expression,
    enabled,
    quartz_job_key,
    last_execution_time,
    next_execution_time,
    retry_count,
    retry_interval
FROM s2_report_schedule
WHERE id = {schedule_id};
```

**判断依据**：
- `enabled = 0` → 调度已被暂停，需要确认是否是预期行为
- `quartz_job_key = NULL` → Quartz Job 注册失败，需重启应用或重新创建调度
- `next_execution_time` 与预期不符 → cron 表达式解析异常，检查表达式语法

查看 Quartz 线程池状态（需要 JMX 或 Spring Actuator 权限的运维人员）：

```bash
# 通过 Actuator 查看 Quartz Scheduler 状态
curl -s -H "Authorization: Bearer ${AUTH_TOKEN}" \
  ${BASE_URL}/actuator/quartz | jq '.'

# 查看当前调度器线程池使用情况
curl -s -H "Authorization: Bearer ${AUTH_TOKEN}" \
  ${BASE_URL}/actuator/metrics/executor.pool.size?tag=name:quartzScheduler | jq '.'
```

#### 步骤 4：检查连接池状态

各连接池默认参数（来自 `PoolType.java`）：

| 连接池类型 | 最大连接数 | 最大等待时间 | 查询超时 | 适用场景 |
|-----------|-----------|------------|---------|---------|
| INTERACTIVE | 10 | 30 秒 | 30 秒 | UI/API 交互查询 |
| REPORT | 3 | 300 秒 | 600 秒 | 报表生成 |
| EXPORT | 2 | 600 秒 | 1800 秒 | 导出任务 |
| SYNC | 2 | 600 秒 | 3600 秒 | 数据同步 |

通过 Prometheus 指标检查连接池：

```bash
# 查看 REPORT 连接池当前活跃连接数
curl -s ${BASE_URL}/actuator/prometheus | \
  grep 'hikaricp_connections_active.*pool="REPORT"'

# 查看连接池等待超时次数（持续增加说明池已满）
curl -s ${BASE_URL}/actuator/prometheus | \
  grep 'hikaricp_connections_timeout.*pool="REPORT"'
```

#### 步骤 5：检查模板是否已下线

```sql
-- 查询对应调度关联的模板状态
SELECT
    rs.id AS schedule_id,
    rs.name AS schedule_name,
    rs.template_version,
    rs.dataset_id,
    rs.cron_expression,
    rs.enabled
FROM s2_report_schedule rs
WHERE rs.id = {schedule_id};
```

若 `template_version` 对应的模板版本已被删除或下线，执行记录会报 `Template is not deployed` 错误。需联系模板负责人重新部署模板后，再恢复调度。

### 止损操作

#### 暂停问题调度任务（防止持续失败刷日志和告警）

```bash
# 暂停指定调度任务（使 Quartz Job 停止触发）
curl -s -X POST \
  -H "Authorization: Bearer ${AUTH_TOKEN}" \
  "${BASE_URL}/api/v1/reportSchedules/{schedule_id}:pause"
```

**影响范围**：该调度任务不再自动触发，不影响其他调度任务和手动执行。

#### 手动触发单次执行（验证修复是否生效）

```bash
# 修复根因后，手动触发一次执行进行验证
curl -s -X POST \
  -H "Authorization: Bearer ${AUTH_TOKEN}" \
  "${BASE_URL}/api/v1/reportSchedules/{schedule_id}:trigger"
```

### 回滚步骤

```bash
# 恢复调度任务（问题修复后）
curl -s -X POST \
  -H "Authorization: Bearer ${AUTH_TOKEN}" \
  "${BASE_URL}/api/v1/reportSchedules/{schedule_id}:resume"
```

恢复后确认 `s2_report_schedule.next_execution_time` 已更新为下次预期执行时间。

---

## 场景 2：导出异常

**紧急程度**：中（影响用户下载报表文件，但不阻断数据生成）
**预计排查时间**：15~25 分钟

### 现象

- 用户反馈导出任务长时间转圈（PENDING 状态超过 10 分钟）
- 导出任务状态显示 FAILED，用户无法下载文件
- 监控告警 `ExportTaskPendingHigh` 触发（积压超过 10 个导出任务持续 10 分钟）

### 排查步骤

#### 步骤 1：查导出任务状态

```sql
-- 查当前 PENDING 和 RUNNING 的导出任务
SELECT
    id,
    task_name,
    user_id,
    dataset_id,
    output_format,
    status,
    file_location,
    file_size,
    row_count,
    created_at,
    expire_time,
    LEFT(error_message, 500) AS error_snippet
FROM s2_export_task
WHERE status IN ('PENDING', 'RUNNING')
ORDER BY created_at ASC;
```

```sql
-- 查过去 1 小时内失败的导出任务
SELECT
    id,
    task_name,
    user_id,
    output_format,
    status,
    file_location,
    created_at,
    error_message
FROM s2_export_task
WHERE status = 'FAILED'
  AND created_at >= NOW() - INTERVAL 1 HOUR
ORDER BY created_at DESC
LIMIT 20;
```

**判断依据**：
- PENDING 任务数量多，created_at 时间早 → EXPORT 线程池满，进入步骤 2
- FAILED 且 `error_message` 含磁盘相关错误 → 进入步骤 3
- FAILED 且 `error_message` 含权限相关错误 → 进入步骤 4

#### 步骤 2：检查 EXPORT 线程池使用率

EXPORT 连接池默认最大并发数为 **2**，极易被占满。

```bash
# 查看 EXPORT 连接池活跃连接数（值接近 2 说明池已满）
curl -s ${BASE_URL}/actuator/prometheus | \
  grep 'hikaricp_connections_active.*pool="EXPORT"'

# 查看 EXPORT 连接池等待中的请求数
curl -s ${BASE_URL}/actuator/prometheus | \
  grep 'hikaricp_connections_pending.*pool="EXPORT"'

# 查看 Prometheus 指标：当前积压导出任务数
curl -s ${BASE_URL}/actuator/prometheus | \
  grep 'supersonic_report_export_pending'
```

通过 JMX 查看（运维人员）：

```bash
# 查看线程池状态（需要 JMX 端口开放，默认 9010）
# MBean: com.zaxxer.hikari:type=Pool (EXPORT)
# 属性: ActiveConnections, IdleConnections, PendingThreads
```

#### 步骤 3：检查磁盘空间

```bash
# 检查导出文件目录所在磁盘剩余空间
df -h /path/to/export/directory

# 检查导出目录实际路径（从应用配置获取）
curl -s -H "Authorization: Bearer ${AUTH_TOKEN}" \
  ${BASE_URL}/actuator/env | jq '.propertySources[] | .properties | to_entries[] | select(.key | contains("export"))'

# 统计导出文件目录大小
du -sh /path/to/export/directory/
```

**危险阈值**：磁盘使用率超过 85% 时应立即清理。

#### 步骤 4：检查文件目录权限

```bash
# 检查导出目录权限（应用进程用户需要读写权限）
ls -la /path/to/export/directory/

# 检查应用进程运行用户
ps aux | grep java | grep supersonic

# 验证写入权限（替换为实际目录路径）
touch /path/to/export/directory/.write_test && rm /path/to/export/directory/.write_test
echo "Write permission OK"
```

### 止损操作

#### 手动将超时的 PENDING 任务标记为 FAILED

⚠️ **影响范围**：直接修改 DB 记录，不可自动撤销。执行前请确认任务确实已超时（建议 PENDING 超过 30 分钟才执行）。
**回滚步骤**：无直接回滚，用户需重新提交导出。

```sql
-- ⚠️ 将超过 30 分钟仍为 PENDING 的任务标记为 FAILED
-- 执行前先 SELECT 确认影响范围
SELECT id, task_name, status, created_at
FROM s2_export_task
WHERE status = 'PENDING'
  AND created_at < NOW() - INTERVAL 30 MINUTE;

-- 确认无误后再执行 UPDATE
UPDATE s2_export_task
SET status = 'FAILED',
    error_message = 'Manually marked as FAILED by ops: task timeout (>30min in PENDING)'
WHERE status = 'PENDING'
  AND created_at < NOW() - INTERVAL 30 MINUTE;
```

#### 清理僵尸导出文件（已过期的文件）

```bash
# 查出已过期的文件路径（expire_time 已过但文件还在）
# 在 MySQL 中执行，获取需要清理的文件路径
```

```sql
SELECT id, file_location, expire_time
FROM s2_export_task
WHERE expire_time < NOW()
  AND file_location IS NOT NULL
  AND status = 'SUCCESS';
```

```bash
# 根据上面查出的路径逐一删除（或批量删除过期目录）
# 示例：删除 7 天前的导出文件（替换为实际目录）
find /path/to/export/directory/ -name "*.xlsx" -mtime +7 -delete
find /path/to/export/directory/ -name "*.csv"  -mtime +7 -delete
```

### 回滚步骤

```bash
# 修复根因后（磁盘清理完成 / 权限修复 / 线程池压力降低），
# 用户重新提交导出请求即可。无需其他回滚操作。

# 验证：提交一个测试导出任务，确认状态从 PENDING → RUNNING → SUCCESS
curl -s -H "Authorization: Bearer ${AUTH_TOKEN}" \
  "${BASE_URL}/api/v1/reportSchedules/{schedule_id}:execute" -X POST
```

```sql
-- 验证导出任务是否正常完成
SELECT id, status, file_location, row_count, created_at
FROM s2_export_task
ORDER BY created_at DESC
LIMIT 5;
```

---

## 场景 3：推送失败

**紧急程度**：高（影响合作方/运营人员及时收到数据报表）
**预计排查时间**：10~15 分钟

### 现象

- 飞书群没有收到预期的报表消息
- `s2_report_delivery_record` 中出现 `status = 'FAILED'` 记录
- 监控告警 `DeliveryFailRateHigh` 触发（推送失败率超过 5%）

### 排查步骤

#### 步骤 1：查推送记录，定位失败原因

```sql
-- 通过 execution_id 查该次执行的所有推送记录
SELECT
    dr.id,
    dr.schedule_id,
    dr.execution_id,
    dr.config_id,
    dr.delivery_type,
    dr.status,
    dr.retry_count,
    dr.max_retries,
    dr.next_retry_at,
    dr.delivery_time_ms,
    dr.started_at,
    dr.completed_at,
    dr.created_at,
    LEFT(dr.error_message, 1000) AS error_snippet
FROM s2_report_delivery_record dr
WHERE dr.execution_id = {execution_id}
ORDER BY dr.created_at DESC;
```

```sql
-- 查过去 1 小时内所有失败的飞书推送记录
SELECT
    dr.id,
    dr.schedule_id,
    dr.execution_id,
    dr.config_id,
    dr.delivery_type,
    dr.status,
    dr.retry_count,
    dr.started_at,
    dr.completed_at,
    LEFT(dr.error_message, 500) AS error_snippet
FROM s2_report_delivery_record dr
WHERE dr.status = 'FAILED'
  AND dr.delivery_type = 'FEISHU'
  AND dr.created_at >= NOW() - INTERVAL 1 HOUR
ORDER BY dr.created_at DESC
LIMIT 20;
```

**判断依据**：

| error_message 关键词 | 根因 | 处理方向 |
|---------------------|------|---------|
| `Connection refused` / `connect timed out` | 网络问题或 Webhook URL 不可达 | 步骤 2：验证 Webhook |
| `Feishu API error: ... token invalid` | 机器人 Token 失效 | 步骤 3：检查签名配置 |
| `Feishu API error: ... sign check failed` | 签名密钥不匹配 | 步骤 3：检查签名配置 |
| `Feishu API error: ... too many requests` | 飞书接口限流 | 步骤 3：降低推送频率 |
| `Feishu API returned non-2xx status: 404` | Webhook URL 已失效 | 步骤 2：重新获取 Webhook URL |

#### 步骤 2：验证 Webhook URL 有效性

```bash
# 查出对应推送配置的 Webhook URL
```

```sql
-- 获取推送配置详情（包含 delivery_config JSON，其中有 webhookUrl）
SELECT
    id,
    name,
    delivery_type,
    delivery_config,
    enabled,
    consecutive_failures,
    max_consecutive_failures,
    updated_at
FROM s2_report_delivery_config
WHERE id = {config_id};
```

```bash
# 从 delivery_config JSON 中提取 webhookUrl，手动测试（替换 WEBHOOK_URL）
curl -s -X POST \
  -H "Content-Type: application/json" \
  -d '{"msg_type":"text","content":{"text":"Runbook 测试消息，请忽略"}}' \
  "WEBHOOK_URL"

# 预期返回：{"StatusCode":0,"StatusMessage":"success","code":0,"data":{},"msg":"success"}
# 如返回 404 或连接超时，说明 URL 已失效
```

#### 步骤 3：检查签名配置

飞书自定义机器人签名验证：`sign = base64(HmacSHA256(timestamp + "\n" + secret))`

```bash
# 如果 Webhook 配置了 Secret，验证签名是否正确
# 以下脚本模拟签名生成（替换 SECRET 和 WEBHOOK_URL）
TIMESTAMP=$(date +%s)
SECRET="your_secret_here"
SIGN=$(echo -n "${TIMESTAMP}\n${SECRET}" | openssl dgst -sha256 -hmac "${SECRET}" -binary | base64)

curl -s -X POST \
  -H "Content-Type: application/json" \
  -d "{\"msg_type\":\"text\",\"content\":{\"text\":\"签名测试\"},\"timestamp\":\"${TIMESTAMP}\",\"sign\":\"${SIGN}\"}" \
  "WEBHOOK_URL"
```

如果测试成功但系统推送失败，可能是 Secret 配置在 `delivery_config` JSON 中有误。

#### 步骤 4：检查是否被自动禁用

```sql
-- 检查推送配置是否因连续失败被自动禁用
SELECT
    id,
    name,
    enabled,
    consecutive_failures,
    max_consecutive_failures,
    updated_at
FROM s2_report_delivery_config
WHERE id = {config_id};
```

**判断依据**：
- `enabled = 0` → 已被自动禁用，跳转到 **场景 4** 处理
- `consecutive_failures` 接近 `max_consecutive_failures` → 即将被禁用，需尽快修复根因

### 止损操作

#### 手动重发失败的推送记录

```bash
# 重新触发指定推送记录的投递
curl -s -X POST \
  -H "Authorization: Bearer ${AUTH_TOKEN}" \
  "${BASE_URL}/api/semantic/delivery/records/{delivery_record_id}:retry"
```

**影响范围**：仅重发该单条推送记录，不影响其他记录和调度任务。

#### 测试推送配置是否可用

```bash
# 向指定推送配置发送测试消息（验证修复后配置是否正常）
curl -s -X POST \
  -H "Authorization: Bearer ${AUTH_TOKEN}" \
  "${BASE_URL}/api/semantic/delivery/configs/{config_id}:test"
```

### 回滚步骤

推送操作无副作用，无需回滚。若重发后飞书群收到重复消息，属于正常现象（用户可识别时间戳区分）。

---

## 场景 4：推送被自动禁用

**紧急程度**：严重（整个推送渠道停止工作，持续影响所有关联的调度任务）
**预计排查时间**：20~30 分钟

### 现象

- 监控告警 `DeliveryConfigAutoDisabled` 触发（`supersonic_report_delivery_config_disabled > 0`）
- 飞书群连续多次未收到消息
- `s2_report_delivery_config.enabled = 0`（该记录被自动禁用）

### 排查步骤

#### 步骤 1：找出被禁用的推送配置

```sql
-- 查所有被禁用的推送配置
SELECT
    id,
    name,
    delivery_type,
    enabled,
    consecutive_failures,
    max_consecutive_failures,
    disabled_reason,
    updated_at,
    created_by
FROM s2_report_delivery_config
WHERE enabled = 0
ORDER BY updated_at DESC;
```

注意：`consecutive_failures >= max_consecutive_failures` 时系统自动将 `enabled` 置为 0，并在 `disabled_reason` 字段记录禁用原因和最后一次错误信息。默认 `max_consecutive_failures = 5`。

#### 步骤 2：查关联的连续失败推送记录

```sql
-- 查该推送配置的最近推送记录，找到连续失败的时间窗口和错误信息
SELECT
    id,
    execution_id,
    status,
    retry_count,
    delivery_time_ms,
    started_at,
    completed_at,
    created_at,
    LEFT(error_message, 1000) AS error_message
FROM s2_report_delivery_record
WHERE config_id = {config_id}
ORDER BY created_at DESC
LIMIT 20;
```

```sql
-- 统计各状态数量，快速了解失败比例
SELECT
    status,
    COUNT(*) AS cnt,
    MIN(created_at) AS earliest,
    MAX(created_at) AS latest
FROM s2_report_delivery_record
WHERE config_id = {config_id}
  AND created_at >= NOW() - INTERVAL 24 HOUR
GROUP BY status;
```

#### 步骤 3：定位根因

根据步骤 2 的 `error_message` 识别根因：

| 根因 | 典型 error_message | 处理方案 |
|------|-------------------|---------|
| Webhook URL 失效 | `404 Not Found` / `Connection refused` | 在飞书群重新创建机器人，获取新 URL，更新 `delivery_config` |
| 签名密钥过期/更换 | `sign check failed` | 在飞书开放平台更新签名密钥，同步更新 `delivery_config` |
| 飞书群被解散 | `group does not exist` / `404` | 更换目标群，重新创建推送配置 |
| 机器人被移出群 | `bot is not in the group` | 将机器人重新添加到飞书群 |
| 接口限流 | `too many requests` / `429` | 降低推送频率（调整 cron_expression 间隔） |
| 消息内容超限 | `message too long` | 精简报表卡片内容模板 |

#### 步骤 4：验证修复

```bash
# 修复根因后，先测试推送配置是否可用
curl -s -X POST \
  -H "Authorization: Bearer ${AUTH_TOKEN}" \
  "${BASE_URL}/api/semantic/delivery/configs/{config_id}:test"

# 检查响应，如果没有报错说明推送配置已可用
```

### 止损操作

**修复根因是前提**。在根因未修复前不要手动恢复，否则系统会再次积累失败次数并重新禁用。

#### 修复根因后手动恢复推送配置

⚠️ **影响范围**：重新启用推送配置后，所有关联该配置的调度任务在下次触发时将尝试推送。若根因未彻底修复，会再次失败并重新禁用。
**回滚步骤**：见下方"如恢复后仍失败"。

```sql
-- ⚠️ 重置连续失败计数并重新启用推送配置
-- 执行前请先通过 :test API 验证推送配置已可用

-- 先 SELECT 确认影响范围
SELECT id, name, enabled, consecutive_failures, max_consecutive_failures
FROM s2_report_delivery_config
WHERE id = {config_id};

-- 确认无误后执行 UPDATE
UPDATE s2_report_delivery_config
SET enabled = 1,
    consecutive_failures = 0,
    updated_at = NOW()
WHERE id = {config_id};
```

#### 验证恢复是否成功

```sql
-- 确认配置已恢复
SELECT id, name, enabled, consecutive_failures, updated_at
FROM s2_report_delivery_config
WHERE id = {config_id};
```

```bash
# 手动触发一次调度，验证推送是否成功到达飞书群
curl -s -X POST \
  -H "Authorization: Bearer ${AUTH_TOKEN}" \
  "${BASE_URL}/api/v1/reportSchedules/{schedule_id}:trigger"
```

```sql
-- 查看最新推送记录状态（等待约 1 分钟后查询）
SELECT
    id,
    status,
    delivery_time_ms,
    completed_at,
    error_message
FROM s2_report_delivery_record
WHERE config_id = {config_id}
ORDER BY created_at DESC
LIMIT 5;
```

### 回滚步骤（恢复后仍失败）

⚠️ **影响范围**：重新禁用推送配置，该渠道的所有推送暂停。
**执行时机**：恢复后触发的推送仍持续失败，且 `consecutive_failures` 重新接近阈值时。

```sql
-- ⚠️ 重新禁用推送配置，防止系统持续重试消耗资源
-- 执行前先 SELECT 确认

UPDATE s2_report_delivery_config
SET enabled = 0,
    updated_at = NOW()
WHERE id = {config_id};
```

然后重新进入排查步骤，深入定位根因（可能是间歇性问题，需要进一步分析飞书 API 返回的错误码）。

---

## 附录：常用查询速查

### 查近 24 小时执行总览

```sql
SELECT
    status,
    COUNT(*) AS total,
    AVG(execution_time_ms) AS avg_ms,
    MAX(execution_time_ms) AS max_ms,
    SUM(row_count) AS total_rows
FROM s2_report_execution
WHERE start_time >= NOW() - INTERVAL 24 HOUR
GROUP BY status;
```

### 查某调度任务的执行历史

```sql
SELECT
    id,
    status,
    attempt,
    start_time,
    end_time,
    execution_time_ms,
    row_count,
    LEFT(error_message, 200) AS error_snippet
FROM s2_report_execution
WHERE schedule_id = {schedule_id}
ORDER BY start_time DESC
LIMIT 20;
```

### 查当前各状态导出任务数量

```sql
SELECT status, COUNT(*) AS cnt, MIN(created_at) AS oldest
FROM s2_export_task
GROUP BY status;
```

### 查所有推送配置健康状态

```sql
SELECT
    id,
    name,
    delivery_type,
    enabled,
    consecutive_failures,
    max_consecutive_failures,
    ROUND(consecutive_failures / max_consecutive_failures * 100, 1) AS failure_pct,
    updated_at
FROM s2_report_delivery_config
ORDER BY consecutive_failures DESC;
```

### 查推送成功率（近 7 天）

```sql
SELECT
    delivery_type,
    status,
    COUNT(*) AS cnt,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (PARTITION BY delivery_type), 1) AS pct
FROM s2_report_delivery_record
WHERE created_at >= NOW() - INTERVAL 7 DAY
GROUP BY delivery_type, status
ORDER BY delivery_type, status;
```
