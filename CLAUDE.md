## Project Overview

SuperSonic is a chat-based data analytics platform that converts natural language queries to SQL using LLM integration. Supports multi-tenancy, RBAC, semantic template management, and hybrid NL2SQL parsing (rule-based + LLM-based).

## Rules

**Bug fixing**
- Trace the FULL execution path before making changes. Do not fix symptoms — identify the actual root cause first.
- State the root cause hypothesis and evidence (`file:line`) — get confirmation before writing code.
- Prefer small, targeted fixes over large refactors. Deliver the minimal working fix first, then optionally suggest improvements — do not refactor without approval.
- Proactively audit adjacent code paths for the same issue pattern — do not wait for external review to surface them.
- Always check interceptors/aspects that silently modify behavior: `TenantInterceptor`, `TenantSqlInterceptor`, `S2DataPermissionAspect`, `ApiHeaderCheckAspect`.
- Read ALL branches of conditionals — bugs often hide in the less-tested branch.
- Module entry points for tracing:
  - **auth**: `TenantInterceptor` → `UserStrategy` → `RoleService` / `PermissionService`
  - **chat**: `ChatQueryController` → `ChatQueryServiceImpl` → executor chain (spring.factories order)
  - **headless (NL2SQL)**: `NL2SQLParser` → mapper strategies → `LLMRequestService` → `SqlCorrector`
  - **headless (core)**: `QueryParser` chain → `SqlQueryParser` → `DbAdaptor` → `JdbcDataSource`
  - **headless (server)**: Domain/Model/Metric/DataSet services → `SemanticTemplateService` → event publishing
  - **common**: `TenantSqlInterceptor` (MyBatis) → `SystemConfigService` → `ExemplarService`
- Common misdiagnosis traps: fixing symptom site instead of origin; blaming code when config/data is wrong; ignoring interceptors/aspects; assuming first suspicious thing is the cause.

**Build verification**
- After every Java edit, run `mvn compile -pl launchers/standalone -am` to verify compilation. Do not skip this step.
- For frontend changes, run the appropriate lint/build command.
- If compilation fails, fix it immediately before moving on.

**Git**
- **Never commit or push without explicit user permission.** Always show a change summary and ask first.
- Do not amend existing commits unless explicitly asked.

**Debugging environment**
- When debugging deployment or environment issues, ask the user to confirm target environment details (OS, web server, encoding, runtime versions) before proposing fixes. Do not assume.

**General**
- Do not over-engineer. Use framework features instead of custom solutions.
- For UI changes, reference an existing page and copy its exact patterns. Do not introduce new styling.
- For large multi-file tasks, break into subtasks by layer (Java / TypeScript / SQL) and use parallel agents.

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.4.x, MyBatis-Plus, LangChain4j
- **Frontend**: TypeScript/React, Ant Design Pro, dayjs

## Module Structure

```
supersonic/
├── auth/          # Authentication & authorization
├── billing/       # Subscription & billing
├── chat/          # Chat interactions & agent management
├── common/        # Shared utilities, tenant context, MyBatis config
├── headless/      # Core semantic layer
├── feishu/        # Feishu bot integration
├── launchers/     # Application entry points (standalone/headless/chat)
└── webapp/        # React frontend (Ant Design Pro)
```

## Build & Run

```bash
mvn compile -pl launchers/standalone -am
mvn test-compile -pl launchers/standalone -am
```

- Default DB: H2 embedded. `S2_DB_TYPE=h2|mysql|postgresql` for alternatives.
- LLM env vars: `OPENAI_BASE_URL`, `OPENAI_API_KEY`, `OPENAI_MODEL_NAME`

## Code Conventions

- **DI**: Constructor injection via `@RequiredArgsConstructor` + `private final`. `@Autowired` only for `@Lazy` / `@Qualifier`.
- **Cross-module**: Spring `ApplicationEvent` — never reflection or circular imports.
- **SPI**: `META-INF/spring.factories` for parser, corrector, mapper, executor extensions.
- **MyBatis-Plus**: `ServiceImpl` base; `LambdaQueryWrapper` for type-safe queries.
- **REST API (new modules)**: Google RESTful design — resource-oriented URLs, standard + custom methods (`:verb`), `pageToken` pagination.
- **Strategy pattern**: Delivery channels, pool types, sync modes — add implementations without modifying existing code.
- **Frontend dates**: `dayjs(value).format('YYYY-MM-DD HH:mm:ss')` — never raw ISO strings.
- **Frontend API base URLs**: `API_BASE_URL` = `/api/semantic/`, `AUTH_API_BASE_URL` = `/api/auth/`. Some APIs use direct paths (e.g., `/api/v1/connections`).

## Flyway Migrations

Path: `launchers/standalone/src/main/resources/db/migration/{mysql,postgresql}/`. New migrations start from **V21+**. Always create both MySQL and PostgreSQL versions.

- `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` — **PostgreSQL only**. MySQL does NOT support this. For MySQL: rely on Flyway single-execution guarantee, or use `information_schema.columns` check.
- `CREATE TABLE IF NOT EXISTS` — both dialects support.

## Multi-Tenant Configuration

Enabled via `s2.tenant.enabled=true`. Key files: `TenantContext.java` (ThreadLocal), `TenantInterceptor.java` (HTTP), `TenantSqlInterceptor.java` (MyBatis auto-injects `WHERE tenant_id = ?`). Excluded tables: `s2_tenant`, `s2_subscription_plan`, `s2_permission`, `s2_role_permission`, `s2_user_role`.

## Design Documents

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
