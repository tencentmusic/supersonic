## Project Overview

SuperSonic is a chat-based data analytics platform that converts natural language queries to SQL using LLM integration. Supports multi-tenancy, RBAC, semantic template management, and hybrid NL2SQL parsing (rule-based + LLM-based).

## Working Preferences

- When fixing bugs, trace the FULL execution path before making changes. Do not fix symptoms — identify the actual root cause first.
- Before implementing a fix, state the root cause hypothesis and evidence (`file:line`) — get confirmation before writing code.
- Do not over-engineer. If a framework provides a feature, use it instead of building a custom solution.
- For UI changes, always reference an existing page and copy its exact patterns. Do not introduce new styling patterns.
- For large multi-file tasks, break into independent subtasks by layer (Java backend / TypeScript frontend / SQL migration) and use parallel agents.
- Always verify import paths against existing files. After multi-file changes, run compilation to catch import errors.


## Tech Stack

- **Backend**: Java 21, Spring Boot 3.4.x, MyBatis-Plus, LangChain4j
- **Frontend**: TypeScript/React, Ant Design Pro, dayjs

## Module Structure

```
supersonic/
├── auth/          # Authentication & authorization (api + authentication)
├── billing/       # Subscription & billing (api + server)
├── chat/          # Chat interactions & agent management (api + server)
├── common/        # Shared utilities, tenant context, MyBatis config
├── headless/      # Core semantic layer (api + chat + core + server)
├── feishu/        # Feishu bot integration (api + server)
├── launchers/     # Application entry points (standalone/headless/chat)
└── webapp/        # React frontend (Ant Design Pro)
```

**Key patterns**: Cross-module communication via Spring `ApplicationEvent` (never reflection). SPI registration via `META-INF/spring.factories`. Multi-tenant isolation via `TenantSqlInterceptor` (MyBatis plugin, auto-injects `WHERE tenant_id = ?`).

## Build & Run

```bash
# Compile
JAVA_HOME=/path/to/jdk21 mvn compile -pl launchers/standalone -am

# Test compile
JAVA_HOME=/path/to/jdk21 mvn test-compile -pl launchers/standalone -am
```

- Default DB: H2 embedded. Set `S2_DB_TYPE=h2|mysql|postgresql` for alternatives.
- LLM env vars: `OPENAI_BASE_URL`, `OPENAI_API_KEY`, `OPENAI_MODEL_NAME`

## Flyway Migrations

Located in `launchers/standalone/src/main/resources/db/migration/{mysql,postgresql}/`. New migrations start from **V21+**. Always create both MySQL and PostgreSQL versions. Use `IF NOT EXISTS` for idempotency.

**MySQL vs PostgreSQL syntax differences:**
- `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` — **PostgreSQL only**. MySQL does NOT support `IF NOT EXISTS` on `ADD COLUMN`. For MySQL migrations that add columns idempotently, either: (a) rely on Flyway's single-execution guarantee and skip `IF NOT EXISTS`, or (b) use a stored procedure with `information_schema.columns` check.
- `CREATE TABLE IF NOT EXISTS` — supported by both MySQL and PostgreSQL.
- Always test migration SQL against both dialects before committing.

## Code Conventions

- **Dependency Injection**: Constructor injection via Lombok `@RequiredArgsConstructor` + `private final` fields. `@Autowired` only for `@Lazy` or `@Qualifier`.
- **Cross-module communication**: Spring `ApplicationEvent` — never reflection or circular imports.
- **SPI registration**: `META-INF/spring.factories` for parser, corrector, mapper, executor extensions.
- **MyBatis-Plus**: `ServiceImpl` base class; `LambdaQueryWrapper` for type-safe queries.
- **REST API (new modules)**: Google RESTful API design — resource-oriented URLs, standard methods + custom methods (`:verb`), `pageToken` pagination.
- **Strategy pattern**: Delivery channels, pool types, sync modes — add new implementations without modifying existing code.
- **Frontend date formatting**: Always use `dayjs(value).format('YYYY-MM-DD HH:mm:ss')` for timestamp display in Table columns. Never render raw ISO 8601 strings.
- **Frontend API base URLs**: `process.env.API_BASE_URL` = `/api/semantic/`, `AUTH_API_BASE_URL` = `/api/auth/`. Some APIs use direct paths (e.g., `/api/v1/connections`).

## Multi-Tenant Configuration

Enabled via `s2.tenant.enabled=true`. Key files: `TenantContext.java` (ThreadLocal), `TenantInterceptor.java` (HTTP), `TenantSqlInterceptor.java` (MyBatis). Excluded tables: `s2_tenant`, `s2_subscription_plan`, `s2_permission`, `s2_role_permission`, `s2_user_role`.

## Design Documents (AI Agent 路由)

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
