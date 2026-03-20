# user-journey.md — SuperSonic 用户旅程

## 旅程 1：数据分析师——自然语言查询

```
登录 → 选择 Agent → 输入自然语言问题 → 查看 SQL 结果 → 导出/可视化
```

### 关键交互点
1. Agent 路由：根据问题内容自动匹配最佳 Agent
2. NL2SQL 解析：规则解析优先，不确定时 fallback 到 LLM
3. 结果展示：表格 + 图表（指标趋势）

## 旅程 2：数据管理员——语义建模

```
创建 Domain → 配置 Database → 创建 Model → 定义 Dimension/Metric → 创建 DataSet → 配置 Agent
```

### 关键交互点
1. Model 发布：DRAFT → ONLINE 后才能被 DataSet 引用
2. DataSet 关联：选择 Model 中的 Dimension/Metric 组合
3. Agent 配置：绑定 DataSet + 设置查询权限

## 旅程 3：平台管理员——租户管理

```
创建 Tenant → 配置 LLM → 管理用户/角色 → 部署语义模板 → 监控使用量
```

### 关键交互点
1. 租户隔离：每个租户独立的 Domain/Model/Agent
2. 模板部署：从模板库选择并实例化到租户
3. 用量监控：查询次数、LLM 调用量
