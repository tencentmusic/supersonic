## 项目概述

SuperSonic 是基于对话的数据分析平台，通过 LLM 将自然语言查询转为 SQL。支持多租户、RBAC、语义模板管理和混合式 NL2SQL 解析（规则 + LLM）。

## 设计文档

三份主文档（≤400 行）只讲边界和主链路，实现级内容在 `docs/details/` 下的自包含 spec 文件中。

| 需要做什么 | 先读哪个文件 |
|-----------|-------------|
| 理解整体架构、子系统关系 | `docs/智能运营数据中台设计方案.md` |
| 实现模板/报表/调度/导出功能 | `docs/details/report/<对应文件>.md` |
| 实现飞书机器人功能 | `docs/details/feishu/<对应文件>.md` |
| 实现数据同步/连接器/RBAC | `docs/details/platform/<对应文件>.md` |
| 找待办事项 | `docs/details/*/backlog.md` |
| 查全部 detail 文件索引 | `docs/details/README.md` |

每个 detail 文件有 frontmatter（status / module / key-files），读完即可编码。

## 技术栈

- **后端**：Java 21、Spring Boot 3.4.x、MyBatis-Plus、LangChain4j
- **前端**：TypeScript/React、Ant Design Pro、dayjs

## 模块结构

```
supersonic/
├── auth/          # 认证与授权
├── billing/       # 订阅与计费
├── chat/          # 对话交互与 Agent 管理
├── common/        # 公共工具、租户上下文、MyBatis 配置
├── headless/      # 核心语义层
├── feishu/        # 飞书机器人集成
├── launchers/     # 应用入口（standalone/headless/chat）
└── webapp/        # React 前端（Ant Design Pro）
```

## 规则

**Bug 修复**
- 修改前完整追踪执行路径，不修复症状——找真正的根本原因。
- 陈述根本原因假设及证据（`文件:行号`），确认后再写代码。
- 优先小而精准的修复，先交付最小可用修复，再视需要建议改进——未经批准不做重构。
- 主动审查相邻代码路径中是否存在相同问题模式。
- 始终检查会静默修改行为的拦截器/切面：`TenantInterceptor`、`TenantSqlInterceptor`、`S2DataPermissionAspect`、`ApiHeaderCheckAspect`。
- 读取条件语句的所有分支——Bug 常藏在测试较少的分支里。
- 各模块追踪入口：
  - **auth**：`TenantInterceptor` → `UserStrategy` → `RoleService` / `PermissionService`
  - **chat**：`ChatQueryController` → `ChatQueryServiceImpl` → executor 链（spring.factories 顺序）
  - **headless（NL2SQL）**：`NL2SQLParser` → mapper 策略 → `LLMRequestService` → `SqlCorrector`
  - **headless（核心）**：`QueryParser` 链 → `SqlQueryParser` → `DbAdaptor` → `JdbcDataSource`
  - **headless（server）**：Domain/Model/Metric/DataSet 服务 → `SemanticTemplateService` → 事件发布
  - **common**：`TenantSqlInterceptor`（MyBatis）→ `SystemConfigService` → `ExemplarService`
- 常见误诊陷阱：修复症状而非根源；代码没问题时忘记检查配置/数据；忽略拦截器/切面；把第一个可疑点当成根因。

**构建验证**
- 每次 Java 改动后运行以下命令，不得跳过：
  ```bash
  mvn compile -pl launchers/standalone -am
  ```
- 前端改动后运行对应的 lint/构建命令。前端要求 **Node.js 18+**（见 `webapp/.nvmrc`）。
- 编译失败时，立即修复，不得继续其他工作。

**Git**
- **未经用户明确授权，禁止提交或推送。** 始终先展示变更摘要并征询确认。
- 未经明确要求，不得 amend 已有 commit。

**通用**
- 不要过度设计，优先使用框架已有能力而非自定义方案。
- UI 改动时，参照已有页面并完全复用其模式，不引入新样式。
- 大型多文件任务按层拆分子任务（Java / TypeScript / SQL），并行使用多个 agent。
- 调试部署或环境问题时，先向用户确认目标环境详情（操作系统、Web 服务器、编码、运行时版本），再提出修复方案。

## 代码规范

- **依赖注入**：`@RequiredArgsConstructor` + `private final` 构造器注入；`@Autowired` 仅用于 `@Lazy` / `@Qualifier`。
- **跨模块通信**：Spring `ApplicationEvent`，禁止反射或循环导入。
- **SPI 扩展**：通过 `META-INF/spring.factories` 注册 parser、corrector、mapper、executor 扩展。
- **MyBatis-Plus**：基于 `ServiceImpl`；`LambdaQueryWrapper` 类型安全查询。
- **REST API（新模块）**：Google RESTful 设计——资源导向 URL、标准方法 + 自定义方法（`:verb`）、`pageToken` 分页。
- **策略模式**：投递渠道、连接池类型、同步模式等，新增实现无需修改现有代码。
- **前端日期**：`dayjs(value).format('YYYY-MM-DD HH:mm:ss')`，禁止直接使用原始 ISO 字符串。
- **前端 API 路径**：`API_BASE_URL` = `/api/semantic/`，`AUTH_API_BASE_URL` = `/api/auth/`；部分 API 使用直接路径（如 `/api/v1/connections`）。

## Flyway 数据库迁移

路径：`launchers/standalone/src/main/resources/db/migration/{mysql,postgresql}/`。新脚本从 **V21+** 开始编号，必须同时提供 MySQL 和 PostgreSQL 版本。

- `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`：**仅 PostgreSQL 支持**，MySQL 不支持。MySQL 版本依赖 Flyway 单次执行保证，或用 `information_schema.columns` 检查。
- `CREATE TABLE IF NOT EXISTS`：两种方言均支持。

## 多租户配置

通过 `s2.tenant.enabled=true` 启用。核心文件：`TenantContext.java`（ThreadLocal）、`TenantInterceptor.java`（HTTP）、`TenantSqlInterceptor.java`（MyBatis 自动注入 `WHERE tenant_id = ?`）。排除表：`s2_tenant`、`s2_subscription_plan`、`s2_permission`、`s2_role_permission`、`s2_user_role`。

## Skill 路由

当用户请求匹配某个 skill 时，**必须**将调用 Skill 工具作为第一个动作，不得直接回答或先使用其他工具。

| 场景 | 调用 |
|------|------|
| 产品想法、"值不值得做"、头脑风暴 | office-hours |
| Bug、报错、"为什么坏了"、500 错误 | investigate |
| 发布、部署、推送、创建 PR | ship |
| QA、测试站点、找 Bug | qa |
| 代码审查、检查 diff | review |
| 发布后更新文档 | document-release |
| 周复盘 | retro |
| 设计系统、品牌 | design-consultation |
| 视觉审查、设计打磨 | design-review |
| 架构评审 | plan-eng-review |
| 保存进度、检查点、恢复 | checkpoint |
| 代码质量、健康检查 | health |
