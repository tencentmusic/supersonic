## Project Overview

SuperSonic is a chat-based data analytics platform that converts natural language queries to SQL using LLM integration. The system supports multi-tenancy, RBAC, semantic template management, and a hybrid parsing approach combining rule-based and LLM-based strategies for NL2SQL conversion.

## Architecture

### Module Structure

```
supersonic/
├── auth/                    # Authentication & authorization
│   ├── api/                 # Interfaces: UserService, TenantService, RoleService, PermissionService
│   └── authentication/      # Implementations: OAuth, JWT, session management
├── billing/                 # Subscription & billing management
│   ├── api/
│   └── server/
├── chat/                    # Chat interactions & agent management
│   ├── api/
│   └── server/              # AgentService, PluginService, MemoryService, event listeners
├── common/                  # Shared utilities, tenant context, MyBatis config
├── headless/                # Core semantic layer
│   ├── api/
│   ├── chat/                # NL2SQL parsing, mapping, correction
│   ├── core/                # SQL translation, execution, caching
│   └── server/              # Domain/model/metric services, template deployment, events
├── launchers/               # Application entry points
│   ├── standalone/          # All-in-one launcher (default)
│   ├── headless/            # Headless-only launcher
│   └── chat/                # Chat-only launcher
└── webapp/                  # React frontend (Ant Design Pro)
```

### Core Components

- **LLM Integration Layer**: LangChain4j orchestrating multiple providers (OpenAI, Ollama, Dify) `ModelProvider.java`
- **NL2SQL Parser**: Multi-stage parsing with rule-based and LLM-based strategies `NL2SQLParser.java`
- **SQL Generation**: Self-consistency voting mechanism `OnePassSCSqlGenStrategy.java`
- **SQL Correction**: Post-generation validation `LLMPhysicalSqlCorrector.java`
- **Multi-Tenant System**: ThreadLocal context + MyBatis SQL interceptor for data isolation
- **Event-Driven Cross-Module Communication**: Spring ApplicationEvent for headless→chat decoupling

### Cross-Module Communication

Headless and chat modules communicate via Spring events, not direct dependencies or reflection:

1. `headless/server` publishes `TemplateDeployedEvent` after template deployment
2. `chat/server` listens via `TemplateDeployedEventListener` and auto-creates Agent/Plugin
3. This avoids circular dependencies — chat depends on headless, never the reverse

## Multi-Tenant System

### Request Flow

1. `TenantInterceptor` resolves tenant ID (header `X-Tenant-Id` → user token → default)
2. Sets `TenantContext` (ThreadLocal)
3. `TenantSqlInterceptor` (MyBatis plugin) auto-injects `WHERE tenant_id = ?` into SQL
4. Context cleared in `afterCompletion`

### Key Files

| File | Purpose |
|------|---------|
| `common/.../context/TenantContext.java` | ThreadLocal tenant ID storage |
| `common/.../interceptor/TenantInterceptor.java` | HTTP interceptor for tenant resolution |
| `common/.../mybatis/TenantSqlInterceptor.java` | Auto-injects tenant_id into SQL |
| `common/.../config/TenantConfig.java` | Enable/disable, excluded tables/paths |
| `common/.../service/CurrentUserProvider.java` | Interface to obtain current user without reflection |

### Configuration

Enabled via `s2.tenant.enabled=true` in application.yaml. Excluded tables (no tenant filtering): `s2_tenant`, `s2_subscription_plan`, `s2_permission`, `s2_role_permission`, `s2_user_role`.

## Authentication & RBAC

### Auth Flow

1. `DefaultAuthenticationInterceptor` validates JWT token from request
2. `UserStrategy` implementations handle different auth modes:
   - `FakeUserStrategy`: Development/testing (no real auth)
   - `HttpHeaderUserStrategy`: Header-based user extraction
   - `OAuthUserStrategy`: Full OAuth with JWT + session + refresh tokens
3. `User` object carries `tenantId`, `roleId`, `permissions`

### RBAC Model

- **Role**: Scoped to PLATFORM or TENANT, can be system-builtin
- **Permission**: Types: MENU, BUTTON, API, DATA; hierarchical (parent_id)
- **Organization**: Tree structure for tenant org hierarchy
- **Junction**: `RolePermissionDO` links roles to permissions; `UserRoleDO` links users to roles

### Key Interfaces (auth/api)

| Interface | Purpose |
|-----------|---------|
| `UserService` | User CRUD, login, token management |
| `TenantService` | Tenant CRUD, resource limit checks |
| `RoleService` | Role management, permission assignment |
| `PermissionService` | Permission CRUD, scope filtering |
| `OrganizationService` | Organization tree management |

## Semantic Template System

Templates declaratively define a complete semantic domain (models, dimensions, metrics, datasets, agents, plugins) and deploy them in one operation.

### Template Config Structure (SemanticTemplateConfig)

```
SemanticTemplateConfig
├── DomainConfig          # Domain name, permissions
├── ModelConfig[]         # Models with dimensions, measures, identifiers
├── ModelRelationConfig[] # Join relations between models
├── DataSetConfig         # Aggregate dataset + query templates
├── AgentConfig           # Chat agent with chatAppOverrides
├── PluginConfig[]        # Web page/service plugins
├── TermConfig[]          # Business term definitions
└── ConfigParam[]         # Key-value parameters
```

### Deployment Flow

1. `SemanticTemplateServiceImpl.deployTemplate()` creates domain, models, metrics, dimensions, datasets
2. Publishes `TemplateDeployedEvent` (Spring ApplicationEvent)
3. `TemplateDeployedEventListener` (in chat/server) creates Agent and Plugins
4. Result stored in `SemanticDeployResult` with all created entity IDs

### Builtin Initializers (ordered)

| Order | Initializer | Purpose |
|-------|-------------|---------|
| 0 | `BuiltinDatabaseInitializer` | Detects DB type, creates demo database |
| 1 | `BuiltinChatModelInitializer` | Creates OpenAI chat model from env vars |
| 2 | `BuiltinSemanticTemplateInitializer` | Deploys builtin templates if auto-deploy enabled |

## NL2SQL Parsing

### Two-Phase Approach

1. **Rule-Based Parsing** (`RuleSqlParser`): Progressive matching STRICT → MODERATE → LOOSE
2. **LLM-Based Parsing** (`LLMSqlParser`): Self-consistency generation with exemplar retrieval

### Key Classes

| Class | Purpose |
|-------|---------|
| `NL2SQLParser` | Main orchestration |
| `OnePassSCSqlGenStrategy` | Self-consistency SQL generation |
| `LLMSqlCorrector` | SQL validation and correction |
| `PromptHelper` | Exemplar retrieval for few-shot prompting |
| `SearchMatchStrategy` | Knowledge base search for entity matching |

## LLM Configuration

### Supported Providers

- **OpenAI**: GPT models with custom endpoints `OpenAiModelFactory.java`
- **Ollama**: Local model deployment `LLMConfigUtils.java`
- **Dify**: Custom API integration `DifyAiChatModel.java`

### Parameters (ChatModelParameters)

- `provider`, `baseUrl`, `apiKey`, `modelName`
- `temperature` (0.0-1.0), `timeOut` (seconds)

## Frontend (webapp/packages/supersonic-fe)

### Page Structure

```
src/pages/
├── AdminTenant/              # Super admin: tenant CRUD, subscription assignment
├── ChatPage/                 # Main chat interface
├── Login/                    # Auth + OAuthCallback
├── Platform/                 # Platform-level admin
│   ├── PermissionManagement  # → ScopePermissionManagement(scope=PLATFORM)
│   ├── RoleManagement        # → ScopeRoleManagement(scope=PLATFORM)
│   └── SubscriptionManagement
├── Tenant/                   # Tenant-level admin
│   ├── MemberManagement
│   ├── PermissionManagement  # → ScopePermissionManagement(scope=TENANT)
│   └── RoleManagement        # → ScopeRoleManagement(scope=TENANT)
├── SemanticModel/            # Domain, model, dimension, metric management
├── SemanticTemplate/         # Template CRUD and deployment
├── System/OrganizationManagement
└── TenantSettings/           # Current tenant config + usage
```

### Shared Component Pattern

Permission and Role management pages use scope-parameterized shared components:
- `components/ScopePermissionManagement` — accepts `scope`, `title`, `scopeTag`, `api`
- `components/ScopeRoleManagement` — same pattern with role-specific API

Page files are thin wrappers (~25 lines) passing scope-specific config to shared components.

### Service Layer

API calls organized by scope:
- `services/platform.ts` — Platform admin APIs (`/api/auth/admin/*`)
- `services/tenant.ts` — Tenant-scoped APIs (`/api/auth/tenant/*`, `/api/auth/role/*`)
- `services/subscription.ts` — Billing APIs

## Build & Run

### Compile

```bash
JAVA_HOME=/path/to/jdk21 mvn compile -pl launchers/standalone -am
```

### Test Compile

```bash
JAVA_HOME=/path/to/jdk21 mvn test-compile -pl launchers/standalone -am
```

### Database

- Default: H2 embedded (`org.h2.Driver`)
- Environment variable: `S2_DB_TYPE=h2|mysql|postgresql`
- Profiles in `application.yaml` select datasource config

### Environment Variables for Builtin LLM

- `OPENAI_BASE_URL`: API endpoint
- `OPENAI_API_KEY`: Authentication key
- `OPENAI_MODEL_NAME`: Model identifier

## Code Conventions

- **Dependency Injection**: Constructor injection via Lombok `@RequiredArgsConstructor` with `private final` fields. `@Autowired` field injection only for `@Lazy` (circular dependency breaking) or `@Qualifier` cases.
- **Abstract classes**: May still use `@Autowired` field injection to avoid cascading constructor changes in subclasses (e.g., `ParameterConfig`, `SqlGenStrategy`, `BaseMatchStrategy`).
- **Cross-module communication**: Spring `ApplicationEvent` — never reflection or direct circular imports.
- **SPI registration**: `META-INF/spring.factories` for parser, corrector, mapper, executor extensions.
- **MyBatis-Plus**: Used for all persistence; `ServiceImpl` base class; `LambdaQueryWrapper` for type-safe queries.

## Skills (Reusable Claude Code Skill Packs)

Project-specific skills in `.claude/skills/` for repeatable architecture tasks:

| Skill | Trigger | What It Does |
|-------|---------|--------------|
| `supersonic-crud-module` | Add new backend entity | Generates Controller→Service→ServiceImpl→DO→Mapper full chain |
| `supersonic-scope-page` | Add new frontend management page | Generates shared component + thin wrapper pages (Platform/Tenant) |
| `supersonic-event-driven` | Cross-module communication needed | Creates ApplicationEvent + @EventListener pair |
| `supersonic-refactor-audit` | Code quality review | Scans 8 anti-pattern categories, outputs prioritized report |
| `supersonic-cloud-native` | Containerization / K8s deployment | Docker, health checks, config externalization, feature flags |
| `supersonic-tenant-migration` | Add tenantId to existing tables | Adds tenantId field to DO classes + SQL migration scripts |
| `supersonic-full-stack-feature` | End-to-end feature (backend+frontend) | Generates Service→Controller→Component→Page full stack |
| `supersonic-test-generator` | Generate test scaffolds | Creates unit (Mockito) + integration (MockMvc) test files |
| `supersonic-semantic-template` | Author semantic templates | Validates template structure, generates domain/model/metric configs |

Run audit: `bash .claude/skills/supersonic-refactor-audit/scripts/run_audit.sh`
Scaffold entity: `bash .claude/skills/supersonic-crud-module/scripts/scaffold.sh <Module> <Entity>`
Generate tests: `bash .claude/skills/supersonic-test-generator/scripts/generate_tests.sh <Module> <Entity>`
Migrate tenantId: `bash .claude/skills/supersonic-tenant-migration/scripts/migrate_all.sh`

## Dependencies

Core dependencies:
- `langchain4j` + `langchain4j-open-ai`: LLM integration
- `mybatis-plus-spring-boot3-starter`: ORM with auto tenant SQL injection
- `pagehelper-spring-boot-starter`: Pagination
- Spring Boot 3.x with Jakarta EE
- Java 21
