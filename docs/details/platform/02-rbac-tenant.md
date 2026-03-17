---
status: partial
module: auth/authentication
key-files:
  - auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/rest/RoleController.java
  - auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/service/RoleServiceImpl.java
  - auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/persistence/mapper/UserRoleDOMapper.java
  - common/src/main/java/com/tencent/supersonic/common/config/TenantConfig.java
  - common/src/main/java/com/tencent/supersonic/headless/core/utils/TenantSqlInterceptor.java
depends-on: []
---

# 多租户模型与 RBAC 权限体系

主文档：[智能运营数据中台设计方案](../../智能运营数据中台设计方案.md)
迁移变更记录：[platform-tenant-rbac-migration.md](../../platform-tenant-rbac-migration.md)

## 1. 文档目标

本文承接平台架构中的**权限与多租户**实现细节，重点回答：

- 多租户隔离如何在 MyBatis 层自动实现
- 平台级 (PLATFORM) 与租户级 (TENANT) 角色的边界
- 数据访问权限、工具调用权限、外部分发权限三层如何分设
- 动作型工具的治理约束

## 2. 多租户模型

### 2.1 租户隔离机制

SuperSonic 通过 MyBatis 拦截器实现**自动租户隔离**，不需要业务代码手动加 `tenant_id` 条件：

```
HTTP 请求
  → TenantInterceptor (Servlet Filter)
      → 从 JWT/Session 解析 tenantId
      → TenantContext.setTenantId(tenantId)   # ThreadLocal
  → MyBatis SQL 执行
      → TenantSqlInterceptor (MyBatis Plugin)
          → JSQLParser 解析 SQL AST
          → 自动注入 WHERE tenant_id = ?
          → 对排除表（excludedTables）跳过注入
```

排除表（`TenantConfig.excludedTables`）：
```java
private List<String> excludedTables = Arrays.asList(
    "s2_tenant",           // 租户主表，无需过滤
    "s2_subscription_plan", // 计划表，平台级
    "s2_permission",       // 权限主表，无 tenant_id 列
    "s2_role_permission",  // 角色-权限关联，无 tenant_id 列
    "s2_user_role"         // 用户-角色关联，无 tenant_id 列
);
```

### 2.2 租户配置开关

通过 `s2.tenant.enabled=true` 启用多租户（默认 false — 单租户模式）。

关键配置文件：
- `TenantContext.java` — ThreadLocal 存储当前租户
- `TenantInterceptor.java` — HTTP 层租户解析
- `TenantSqlInterceptor.java` — MyBatis SQL 层自动注入

## 3. 角色层级（PLATFORM / TENANT 双作用域）

### 3.1 权限模型

```
┌─────────────────────────────────────────────────────────────────┐
│                        SuperSonic SaaS                          │
├─────────────────────────────────────────────────────────────────┤
│  平台级 (Platform Scope)              │  租户级 (Tenant Scope)   │
│  ─────────────────────                │  ────────────────────    │
│  • 租户管理                           │  • 组织架构管理           │
│  • 订阅计划管理                       │  • 成员管理              │
│  • 平台角色管理                       │  • 租户角色管理          │
│  • 平台权限管理                       │  • 租户权限管理          │
│  • 系统设置                           │  • 租户设置              │
│                                       │  • 用量统计              │
└─────────────────────────────────────────────────────────────────┘
```

`s2_role.scope` 字段区分作用域：
- `PLATFORM` — 平台超级管理员、平台运营
- `TENANT` — 租户管理员、分析师、查看者

### 3.2 预置角色

| ID | code | scope | 说明 |
|----|------|-------|------|
| 1 | ADMIN | TENANT | 系统管理员 |
| 2 | ANALYST | TENANT | 分析师 |
| 3 | VIEWER | TENANT | 查看者 |
| 4 | TENANT_ADMIN | TENANT | 租户管理员 |
| 5 | SAAS_ADMIN | PLATFORM | SaaS 管理员 |
| 6 | PLATFORM_SUPER_ADMIN | PLATFORM | 平台超级管理员 |
| 7 | PLATFORM_OPERATOR | PLATFORM | 平台运营 |

### 3.3 权限码分级

| 权限码前缀 | 作用域 | 示例 |
|-----------|--------|------|
| `PLATFORM_*` | PLATFORM | `PLATFORM_TENANT_MANAGE`, `PLATFORM_ROLE_MANAGE` |
| `TENANT_*` | TENANT | `TENANT_ORG_MANAGE`, `TENANT_MEMBER_MANAGE` |
| `API_TEMPLATE_*` | TENANT | `API_TEMPLATE_DEPLOY`, `API_TEMPLATE_CREATE` |
| `MENU_*` | TENANT | `MENU_SEMANTIC_TEMPLATE`, `MENU_DATASOURCE` |

**设计原则**：租户管理员只能管理租户级角色，不能看到或影响平台级角色。平台级角色只能通过平台管理页面操作。

## 4. 数据权限（行级 + 字段级）

### 4.1 三层权限分设

| 权限层级 | 说明 | 示例 |
|---------|------|------|
| **数据访问权限** | 控制能看哪些库表、字段、租户、维度 | 城市行级过滤、敏感字段脱敏 |
| **工具调用权限** | 控制能否调用某类工具或动作 | 是否允许执行 `reconcileData`、`createRiskEvent` |
| **外部分发权限** | 控制结果是否可发往外部渠道 | 是否允许发送指定飞书群、创建公开分享链接 |

### 4.2 角色权限矩阵

| 角色 | 数仓查询 | 提数对账 | 风控监控 | 飞书协作 | 特殊限制 |
|------|---------|---------|---------|---------|---------|
| 运营专员 | 业务表 | 只读 | 不可用 | 可用 | 只能查本城市 |
| 数据分析师 | 全部 | 只读 | 只读 | 可用 | 敏感字段脱敏 |
| 财务人员 | 财务表 | 全部 | 不可用 | 可用 | 需要审批 |
| 风控人员 | 风控表 | 不可用 | 全部 | 可用 | 审计日志 |
| 管理层 | 汇总表 | 汇总 | 汇总 | 可用 | 只看汇总 |

### 4.3 行级权限实现

行级权限注入流程（AOP + JSqlParser）：

```
S2DataPermissionAspect (@Around)
  → 获取当前用户维度权限（DimensionFilter 列表）
  → QueryStructReq: 注入 Filter(FilterOperatorEnum.SQL_PART)
  → QuerySqlReq: SqlAddHelper.addWhere() 修改 SQL AST
  → 多个维度过滤条件以 OR 连接
```

设计方案中的 `row_filters` 配置映射为 SuperSonic 的**维度权限配置**（DataSet 级别为每个角色设置维度过滤条件）：

```yaml
# 设计层描述
row_filters:
  运营专员: "city = '${user.city}'"
  区域经理: "region = '${user.region}'"
```

对应工程实现：DataSet 授权配置 → `DataSetAuthService.checkDataSetViewPermission()`。

### 4.4 字段级脱敏

`sensitive_columns` 映射为 Dimension/Metric 的 `sensitiveLevel` 属性：

```yaml
# 设计层描述
sensitive_columns:
  user_mobile:   phone_mask    # 138****8888
  user_id_card:  idcard_mask   # 110***********1234
  user_name:     name_mask     # 张**
```

对应工程：`SensitiveLevelEnum` (LOW / MID / HIGH) + `SensitiveLevelConfig`。

## 5. 动作型工具治理

对账、风控、飞书、审批等存在副作用的工具，必须满足以下治理规范：

| 约束项 | 要求 |
|-------|------|
| **幂等键** | 所有写操作必须支持 `request_id` / `business_id`，避免重复创建事件、重复发送通知 |
| **确认机制** | 高风险操作默认需要二次确认或审批，如拉黑、发起审批、批量通知 |
| **审计日志** | 记录操作人、触发入口、请求参数摘要、目标对象、结果、重试次数 |
| **失败补偿** | 定义可重试/不可重试动作，失败后可补发、可人工接管，不允许静默丢失 |
| **超时与重试** | 区分查询类与动作类工具；动作类默认禁止无限重试 |
| **权限校验** | 在工具执行前校验工具调用权限和外部分发权限，而不只校验数据权限 |

## 6. 工程实现状态

| 能力 | 核心类 | 状态 |
|------|--------|------|
| RBAC 体系（双作用域） | `RoleService`, `PermissionService`, `RolePermissionDO` | 已实现 |
| 行级权限（WHERE 注入） | `S2DataPermissionAspect`, `SqlAddHelper.addWhere()` | 已实现 |
| 字段级脱敏 | `SensitiveLevelEnum`, `SensitiveLevelConfig` | 已实现 |
| 多租户隔离（自动注入） | `TenantSqlInterceptor`, `TenantContext` | 已实现 |
| DataSet 访问控制 | `DataSetAuthService.checkDataSetViewPermission()` | 已实现 |
| 工具调用权限 | — | 待开发 |
| 外部分发权限 | — | 待开发 |
| 动作审计日志（完整） | — | 待开发 |

## 7. 相关 API（已实现）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/auth/role/{roleId}/permission-ids` | 获取角色的权限 ID 列表 |
| POST | `/api/auth/user/role` | 为用户分配角色 |
| GET | `/api/auth/user/{userId}/role-ids` | 获取用户的角色 ID 列表 |
| GET | `/platform/roles` | 获取平台角色列表 |
| POST | `/platform/roles/{id}/permissions` | 为平台角色分配权限 |
| GET | `/tenant/roles` | 获取当前租户角色列表 |

完整 API 列表见 [platform-tenant-rbac-migration.md](../../platform-tenant-rbac-migration.md)。
