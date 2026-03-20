# entities.md — SuperSonic 核心实体定义

## 实体关系概览

```
Tenant 1──N Domain 1──N Model 1──N Dimension/Metric
                         │
                    Database (databaseId)

Domain 1──N DataSet ←── Agent (N:N)
                │
                └── ReportSchedule 1──N ReportExecution
                         │                    │
                         └──N ReportDeliveryConfig    1──N ReportDeliveryRecord

SemanticTemplate ──→ Model/Dimension/Metric/DataSet（模板实例化）

AlertRule ──→ DataSet（datasetId）
AlertRule 1──N AlertExecution 1──N AlertEvent
AlertRule ──→ ReportDeliveryConfig（deliveryConfigIds，复用推送渠道）
```

## 核心实体

### Tenant（租户）
- id: Long
- name: String
- description: String
- isActive: Boolean

### Domain（主题域）
- id: Long
- tenantId: String
- name: String
- parentId: Long（支持层级）
- bizName: String
- status: Integer

### Model（数据模型）
- id: Long
- tenantId: String
- domainId: Long
- name: String
- bizName: String
- alias: String
- status: Integer（DRAFT/ONLINE/OFFLINE）
- databaseId: Long

### Dimension（维度）
- id: Long
- tenantId: String
- modelId: Long
- name: String
- bizName: String
- alias: String
- type: String（partition_time / categorical / ...）
- semanticType: String

### Metric（指标）
- id: Long
- tenantId: String
- modelId: Long
- name: String
- bizName: String
- alias: String
- type: String（ATOMIC / DERIVED / CUSTOM）

### DataSet（数据集）
- id: Long
- tenantId: String
- domainId: Long
- name: String
- description: String
- queryType: String

### Agent（对话代理）
- id: Long
- tenantId: String
- name: String
- description: String
- status: Integer
- enableSearch: Integer

### Database（数据源）
- id: Long
- tenantId: String
- name: String
- description: String
- type: String（MySQL / PostgreSQL / ClickHouse / ...）
- config: JSON（JDBC连接配置）
- poolConfig: JSON（连接池参数）
- admin: String（管理员用户名列表）
- viewer: String（只读用户名列表）
- isOpen: Integer（0=私有 1=公开）

### SemanticTemplate（语义模板）
- id: Long
- name: String
- description: String
- config: JSON（模板配置）
- status: String

### ReportSchedule（报表调度）
- id: Long
- tenantId: String
- name: String
- datasetId: Long（关联 DataSet）
- queryConfig: JSON（查询配置）
- outputFormat: String（EXCEL / CSV / ...）
- cronExpression: String
- enabled: Boolean
- ownerId: Long（以 owner 身份执行查询）
- retryCount: Integer（最大重试次数）
- retryInterval: Integer（重试间隔秒数）
- templateVersion: String
- deliveryConfigIds: String（关联推送渠道 ID 列表）
- quartzJobKey: String（Quartz 任务标识）
- lastExecutionTime: DateTime
- nextExecutionTime: DateTime

### ReportDeliveryConfig（推送渠道配置）
- id: Long
- tenantId: String
- name: String
- deliveryType: String（FEISHU / EMAIL / WEBHOOK / ...）
- deliveryConfig: JSON（渠道专属参数）
- enabled: Boolean
- description: String
- consecutiveFailures: Integer（当前连续失败次数）
- maxConsecutiveFailures: Integer（触发自动禁用阈值）
- disabledReason: String（自动禁用原因）

### ReportExecution（报表执行记录）
- id: Long
- tenantId: String
- scheduleId: Long
- attempt: Integer（重试次数）
- status: String（PENDING/RUNNING/SUCCESS/FAILED）
- startTime: DateTime
- endTime: DateTime
- resultLocation: String（结果文件路径/URL）
- errorMessage: String
- rowCount: Long（结果行数）
- sqlHash: String（SQL 指纹，用于缓存复用）
- executionSnapshot: JSON（快照数据，DeliveryContext 序列化）
- templateVersion: String
- engineVersion: String
- scanRows: Long
- executionTimeMs: Long
- ioBytes: Long

### ReportDeliveryRecord（推送记录）
- id: Long
- tenantId: String
- deliveryKey: String（幂等键：scheduleId+executionId+configId）
- scheduleId: Long
- executionId: Long
- configId: Long（关联 ReportDeliveryConfig）
- deliveryType: String
- status: String（PENDING/SUCCESS/FAILED/RETRYING）
- fileLocation: String
- errorMessage: String
- retryCount: Integer
- maxRetries: Integer
- nextRetryAt: DateTime
- startedAt: DateTime
- completedAt: DateTime
- deliveryTimeMs: Long

### AlertRule（告警规则）
- id: Long
- tenantId: Long
- name: String
- datasetId: Long（关联 DataSet）
- queryConfig: JSON（QueryStructReq / SqlTemplateConfig，禁止 QuerySqlReq）
- conditions: JSON（AlertCondition 数组）
- cronExpression: String
- enabled: Boolean
- ownerId: Long（权限基线，以 owner 身份执行查询）
- deliveryConfigIds: String（推送渠道）
- silenceMinutes: Integer（默认 60）
- maxConsecutiveFailures: Integer（默认 5，连续失败后自动禁用）
- disabledReason: String（自动禁用原因记录）

### AlertExecution（告警执行记录）
- id: Long
- ruleId: Long
- status: String（PENDING/RUNNING/SUCCESS/FAILED）
- totalRows: Long
- alertedRows: Long
- silencedRows: Long

### AlertEvent（告警事件）
- id: Long
- executionId: Long
- ruleId: Long
- alertKey: String（去重键：ruleId + conditionIndex + dimensionValue）
- severity: String（WARNING/CRITICAL）
- dimensionValue: String
- metricValue: Double
- baselineValue: Double
- deliveryStatus: String（PENDING/SUCCESS/FAILED/SILENCED）
- silenceUntil: DateTime
