# intent.md — SuperSonic 业务意图声明（最高优先级）

## 核心业务目标
SuperSonic 是一个基于对话的数据分析平台，将自然语言查询转换为 SQL，支持多租户、RBAC、语义模板管理和混合 NL2SQL 解析。

---

## Anti-Goals（AI 生成代码时禁止违反）

### AG-01: 跨租户数据隔离
不允许任何 API 接口返回非当前租户的数据。所有查询必须经过 TenantSqlInterceptor 过滤。

### AG-02: NL2SQL 注入防护
不允许用户输入的自然语言直接拼接到 SQL 中。所有 NL2SQL 输出必须经过参数化处理。

### AG-03: Agent 权限边界
不允许 Agent 执行超出其关联数据集范围的查询。Agent 的查询能力必须受限于其配置的 dataSet。

### AG-04: 模板权限控制
不允许非管理员角色修改语义模板配置。模板的 CRUD 操作必须有 RBAC 校验。

### AG-05: 跨模块直接依赖
不允许使用 Class.forName 或反射方式进行跨模块调用。必须使用 Spring ApplicationEvent。

### AG-06: 告警规则禁止原始 SQL
告警规则的 queryConfig 不允许使用 QuerySqlReq（原始 SQL）模式，只允许 QueryStructReq 和 SqlTemplateConfig。防止 SQL 注入攻击面。

### AG-07: 告警 Cron 最小间隔
告警规则的 Cron 表达式执行间隔不得小于 5 分钟。防止高频轮询耗尽 Quartz 线程池和数仓资源。

### AG-08: 告警查询结果行数上限
AlertCheckDispatcher 执行查询前强制注入 LIMIT ≤ 1000。防止大结果集导致 OOM。

### AG-09: 告警消息模板转义
告警消息模板的 `${variable}` 插值结果必须转义 Markdown 特殊字符（`[]()` `**` `` ` ``），防止飞书卡片渲染异常或注入恶意链接。

### AG-10: 租户告警规则数量上限
每租户告警规则上限 50 条（软限制，管理员可通过系统配置调整）。防止滥用导致 Quartz 过载。

---

## 数据敏感级别

| 字段 | 敏感级别 | 处理要求 |
|------|---------|---------|
| userId | 高敏 | 日志脱敏 |
| queryText（用户输入） | 中敏 | 不传给外部 LLM 时脱敏 |
| tenantId | 高敏 | 不可跨租户暴露 |
| API Key（LLM） | 极高敏 | 禁止出现在日志和前端 |
| queryConfig（告警查询配置） | 中敏 | 含表名/字段名，API 返回时按权限过滤 |
| dimension_value（告警维度值，如合作方编码） | 中敏 | 商业信息，飞书群推送需确保群成员有权查看 |
| alert message（渲染后告警文本） | 中敏 | 含业务指标数据，推送到飞书群后不可撤回 |

---

## 业务边界

- 语义层（headless）：核心 NL2SQL 解析引擎，支持规则解析 + LLM 解析混合模式
- 对话层（chat）：Agent 管理、多轮对话、查询路由
- 权限层（auth）：RBAC、多租户、OAuth
- 计费层（billing）：订阅计划、用量计量
- 告警层（headless/alert）：基于语义查询结果的阈值告警，复用 Quartz 调度 + 推送渠道
