# test-matrix.md — SuperSonic 测试矩阵

## 分层测试矩阵

| 模块 | 测试类型 | 框架 | 位置 |
|------|---------|------|------|
| headless/server | MockMvc + 单元测试 | Spring Test + Mockito | launchers/standalone/src/test/ |
| chat/server | MockMvc + 单元测试 | Spring Test + Mockito | launchers/standalone/src/test/ |
| auth | 单元测试 | JUnit5 + Mockito | launchers/standalone/src/test/ |
| common | 单元测试 | JUnit5 | launchers/standalone/src/test/ |
| webapp | 组件测试 | Jest + React Testing Library | webapp/ |

## 必测场景

### 多租户隔离测试
- [ ] 设置 TenantContext 后查询只返回当前租户数据
- [ ] 未设置 TenantContext 时查询被拒绝
- [ ] 排除表查询不受租户过滤影响

### NL2SQL 测试
- [ ] 规则解析成功路径
- [ ] LLM fallback 路径
- [ ] 解析超时处理
- [ ] SQL 注入防护

### 状态机测试
- [ ] Model: DRAFT→ONLINE→OFFLINE→DELETED 合法流转
- [ ] Model: 所有非法跳跃抛出异常
- [ ] Agent: ENABLED↔DISABLED 双向流转

### 权限测试
- [ ] RBAC 角色权限校验
- [ ] 数据集级别权限控制
- [ ] Agent 数据集范围限制
