# rule-engine.md — SuperSonic 规则引擎定义

## 原则
所有业务规则必须通过 Rule Key 引用，禁止 AI 在代码中硬编码魔法数字或业务常量。

---

## 规则 Key 定义

### 租户隔离规则

| Rule Key | 描述 | 值 |
|----------|------|---|
| TENANT_ISOLATION_ENABLED | 是否启用多租户隔离 | s2.tenant.enabled (boolean) |
| TENANT_EXCLUDED_TABLES | 不需要租户隔离的表 | s2_tenant, s2_subscription_plan, s2_permission, s2_role_permission, s2_user_role |

### NL2SQL 解析规则

| Rule Key | 描述 | 值 |
|----------|------|---|
| NL2SQL_PARSE_TIMEOUT | 单次解析超时（ms） | 配置化 |
| NL2SQL_LLM_FALLBACK | 规则解析失败是否 fallback 到 LLM | true |
| NL2SQL_MAX_RESULT_ROWS | 查询结果最大行数 | 配置化 |

### 模型管理规则

| Rule Key | 描述 | 约束 |
|----------|------|------|
| MODEL_PUBLISH_REQUIRE | 模型发布前置条件 | 至少 1 个维度 + 1 个指标 |
| MODEL_DELETE_CHECK | 模型删除前检查 | 无关联 DataSet 引用 |
| DATASET_MODEL_STATUS | 数据集引用模型状态要求 | 模型必须为 ONLINE |

### Agent 规则

| Rule Key | 描述 | 约束 |
|----------|------|------|
| AGENT_DATASET_SCOPE | Agent 查询范围 | 限于关联 DataSet |
| AGENT_MAX_DATASETS | 单个 Agent 最大关联 DataSet 数 | 配置化 |

### 语义模板规则

| Rule Key | 描述 | 约束 |
|----------|------|------|
| TEMPLATE_DATE_DIM_TYPE | 模板日期维度类型 | 必须为 partition_time（非 time） |
| TEMPLATE_DEPLOY_CHECK | 模板部署前检查 | 配置完整性校验 |
