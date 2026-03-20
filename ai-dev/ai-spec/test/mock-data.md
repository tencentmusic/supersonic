# mock-data.md — SuperSonic 测试 Mock 数据规范

## Mock 数据原则
1. 所有 Mock 数据必须包含 tenantId 字段
2. 使用固定的测试租户 ID：`test-tenant-001`
3. 日期相关字段使用 `2025-01-01` 系列固定值
4. ID 字段使用可预测的值（1L, 2L, 3L...）

## 标准 Mock 数据集

### Tenant
```json
{ "id": 1, "name": "测试租户", "isActive": true }
```

### Domain
```json
{ "id": 1, "tenantId": "test-tenant-001", "name": "访问统计", "bizName": "visit_stats" }
```

### Model
```json
{ "id": 1, "tenantId": "test-tenant-001", "domainId": 1, "name": "页面访问", "bizName": "page_visit", "status": 1 }
```

### Dimension
```json
{ "id": 1, "tenantId": "test-tenant-001", "modelId": 1, "name": "访问日期", "bizName": "visit_date", "type": "partition_time" }
```

### Metric
```json
{ "id": 1, "tenantId": "test-tenant-001", "modelId": 1, "name": "访问量", "bizName": "pv", "type": "ATOMIC" }
```

### DataSet
```json
{ "id": 1, "tenantId": "test-tenant-001", "domainId": 1, "name": "访问数据集" }
```

### Agent
```json
{ "id": 1, "tenantId": "test-tenant-001", "name": "访问分析助手", "status": 1 }
```
