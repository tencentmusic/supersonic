# entities.md — SuperSonic 核心实体定义

## 实体关系概览

```
Tenant 1──N Domain 1──N Model 1──N Dimension/Metric
                                        │
Domain 1──N DataSet ←── Agent (N:N)     │
                │                        │
                └── View                 │
                                         │
SemanticTemplate ──→ Model/Dimension/Metric/DataSet（模板实例化）
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

### SemanticTemplate（语义模板）
- id: Long
- name: String
- description: String
- config: JSON（模板配置）
- status: String

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
