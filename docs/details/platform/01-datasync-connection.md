---
status: partial
module: headless/server
key-files:
  - headless/core/src/main/java/com/tencent/supersonic/headless/core/pojo/JdbcDataSource.java
  - headless/core/src/main/java/com/tencent/supersonic/headless/core/utils/JdbcDuckDbUtils.java
  - headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/DownloadServiceImpl.java
depends-on: []
---

# 数据同步与连接器设计

主文档：[智能运营数据中台设计方案](../../智能运营数据中台设计方案.md)

## 1. 文档目标

本文承接平台架构中的**数据同步与连接器**两大平台共享能力：

- Connection 生命周期管理（连接器配置、发现、校验）
- 数据同步执行（增量/全量同步、任务调度）
- Airbyte 风格的 Catalog 发现机制
- 通用 HTTP 连接器架构

这两部分是平台基础设施能力，模板报表子系统、飞书机器人分发、MCP 工具层均依赖它们。

## 2. Connection 生命周期

### 2.1 设计目标

Connection 模型的目标是统一管理所有外部数据源连接和外部服务连接，避免各子系统自行维护凭据，同时支持测试连通性和 Schema 发现。

| 连接类型 | 用途 | 当前状态 |
|---------|------|---------|
| JDBC 数据源 | 查询分析库（ClickHouse/Hive/MySQL/PG） | **已在主干** — `JdbcDataSource` |
| HTTP Service | 飞书 API / 风控系统 / 外部 webhook | **已有设计** — `ConnectorConfig` |
| S2 内部 | SuperSonicApiClient 回环调用 | **已上线** |

### 2.2 Connection 配置模型

```
s2_connection
├── id, name, description
├── type          # JDBC | HTTP_SERVICE | FEISHU | MCP
├── config        # JSON — 连接参数（host/port/db 或 baseUrl/auth）
├── status        # ACTIVE | INACTIVE | ERROR
├── tenant_id
└── created_at, updated_at
```

JDBC 连接参数示例：
```json
{
  "jdbcUrl": "jdbc:clickhouse://host:8123/db",
  "username": "readonly",
  "password": "{{encrypted}}",
  "driverClass": "com.clickhouse.jdbc.ClickHouseDriver"
}
```

HTTP Service 连接参数示例（Airbyte-inspired）：
```json
{
  "baseUrl": "https://open.feishu.cn/open-apis",
  "auth": {
    "type": "OAUTH2_CLIENT_CREDENTIALS",
    "tokenUrl": "/auth/v3/tenant_access_token/internal",
    "clientId": "{{APP_ID}}",
    "clientSecret": "{{APP_SECRET}}"
  },
  "defaultHeaders": {
    "Content-Type": "application/json"
  }
}
```

### 2.3 Catalog 发现（Airbyte 借鉴）

对于 JDBC 连接，支持自动发现库/表/字段结构，供语义建模使用：

```
POST /api/connections/{id}/catalog:discover

响应：
{
  "streams": [
    {
      "name": "order_fact",
      "namespace": "dw",
      "jsonSchema": {
        "properties": {
          "order_id": { "type": "string" },
          "amount":   { "type": "number" },
          "city":     { "type": "string" }
        }
      },
      "supportedSyncModes": ["full_refresh", "incremental"],
      "sourceDefinedCursor": true,
      "defaultCursorField": ["updated_at"]
    }
  ]
}
```

实现映射：`SchemaService.fetchSemanticSchema()` → 可在此基础上增加 catalog discover 接口。

## 3. 数据同步执行

### 3.1 同步策略

| 同步模式 | 说明 | 适用场景 |
|---------|------|---------|
| `FULL` | 全量覆盖 | 小维表、字典表 |
| `INCREMENTAL` | 按 cursor 字段增量追加 | 交易流水、日志 |
| `INCREMENTAL_DEDUP` | 增量追加 + 去重 | 幂等更新场景 |
| `PARTITION_OVERWRITE` | 按分区覆盖 | 按日分区的 ODS 表 |

### 3.2 核心数据模型

```sql
-- 同步任务配置
CREATE TABLE s2_data_sync_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    source_conn_id  BIGINT NOT NULL,        -- 源 Connection ID
    target_conn_id  BIGINT NOT NULL,        -- 目标 Connection ID
    source_table    VARCHAR(200) NOT NULL,
    target_table    VARCHAR(200) NOT NULL,
    sync_mode       VARCHAR(50)  NOT NULL,  -- FULL | INCREMENTAL | ...
    cursor_field    VARCHAR(100),           -- 增量游标字段
    schedule_cron   VARCHAR(100),           -- Quartz cron 表达式
    is_enabled      TINYINT DEFAULT 1,
    extra_config    JSON,                   -- 字段映射、过滤条件等
    tenant_id       BIGINT,
    created_at      DATETIME,
    updated_at      DATETIME
);

-- 同步执行记录
CREATE TABLE s2_data_sync_execution (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id       BIGINT NOT NULL,
    status          VARCHAR(20),  -- RUNNING | SUCCESS | FAILED
    rows_synced     BIGINT,
    started_at      DATETIME,
    finished_at     DATETIME,
    error_message   TEXT,
    tenant_id       BIGINT
);
```

### 3.3 执行引擎设计

同步任务与报表调度共享 Quartz，但通过线程池隔离避免相互影响：

```
SyncJobQuartzTrigger
  → SyncExecutorService.execute(configId)
    → ConnectionService.getConnection(sourceConnId) → JDBC
    → StreamingReader.readBatch(cursorValue, batchSize)
    → ConnectionService.getConnection(targetConnId) → JDBC
    → BatchWriter.writeBatch(rows)
    → SyncExecutionService.recordProgress()
```

关键约束：
- 同步线程池与报表执行线程池硬隔离（独立 `ThreadPoolExecutor`）
- 单次同步批量大小默认 5000 行，可配置
- 超时保护：JDBC Statement timeout 注入（参考 `模板报表子系统技术设计说明书` 3.10.7）
- 失败后按指数退避重试，最大 3 次；超过上限标记 FAILED，不再自动重试

### 3.4 关键取舍

- 当前同步能力定位为"报表查询卸载工具"，不是完整的数据集成平台（Airbyte/DataX 层级）
- 不引入外部同步平台作为强依赖；轻量场景内置实现，重量级数据集成需外接
- Connection 模型属于后续增强，不反向扩大模板报表主文档范围

## 4. 通用 HTTP 连接器

### 4.1 角色定位

通用连接器用于支撑飞书消息推送、风控 API 对接、第三方 Webhook 等 HTTP 集成场景。本身是平台共享基础设施，不属于任何单一子系统。

### 4.2 ConnectorConfig 模型

```
ConnectorConfig
├── 基础配置
│   ├── name, type (HTTP_REQUEST | GRAPHQL | WEBHOOK | INTERNAL)
│   └── description
├── 请求配置
│   ├── url               # 支持 {{placeholder}} 占位符
│   ├── method            # GET | POST | PUT | DELETE
│   ├── contentType       # application/json | x-www-form-urlencoded
│   ├── headers, queryParams, bodyParams
│   └── bodyTemplate      # JSON 模板
├── 认证配置 (AuthConfig)
│   ├── NONE | API_KEY | BASIC | BEARER | OAUTH2
│   └── tokenUrl, clientId, clientSecret（OAUTH2 时）
├── 响应处理 (ResponseConfig)
│   ├── dataPath          # JSONPath 提取数据
│   ├── errorPath         # JSONPath 提取错误信息
│   └── successCodes      # HTTP 成功状态码
└── 高级配置
    ├── timeoutMs (默认 30000)
    ├── retryCount (默认 2)
    └── retryDelayMs (默认 1000)
```

### 4.3 执行引擎能力

| 能力 | 说明 | 实现类 |
|------|------|--------|
| 参数替换 | `{{param}}` 占位符动态替换 | `ConnectorExecutor` |
| 认证注入 | 自动添加 Auth Header / Token 刷新 | `ConnectorExecutor.addAuthHeaders()` |
| 超时控制 | 连接/读取超时独立配置 | `SimpleClientHttpRequestFactory` |
| 重试机制 | 指数退避、仅网络错误重试 | `ConnectorExecutor.executeWithRetry()` |
| 响应提取 | JSONPath 灵活提取数据 | FastJSON `JSONPath.eval()` |
| 配置验证 | 上线前校验配置有效性 | `ConnectorExecutor.testConfig()` |

### 4.4 飞书连接器配置示例

```json
{
  "name": "飞书机器人 Webhook",
  "type": "HTTP_REQUEST",
  "url": "https://open.feishu.cn/open-apis/bot/v2/hook/{{webhook_id}}",
  "method": "POST",
  "contentType": "application/json",
  "bodyTemplate": "{\"msg_type\":\"{{msg_type}}\",\"content\":{\"text\":\"{{message}}\"}}",
  "auth": { "type": "NONE" },
  "response": {
    "dataPath": "$.data",
    "errorPath": "$.msg",
    "successCodes": [0]
  },
  "timeoutMs": 10000,
  "retryCount": 2
}
```

### 4.5 后续扩展

| 扩展项 | 优先级 | 状态 |
|--------|--------|------|
| OAuth2 Token 自动刷新 | P1 | 待开发 |
| 预置连接器市场（飞书/企微/钉钉模板） | P1 | 待开发 |
| 可视化配置界面 + 在线测试 | P2 | 待开发 |
| GraphQL 支持 | P3 | 待开发 |
| 连接器版本管理 | P3 | 待开发 |

## 5. 工程文件索引

| 文件 | 说明 | 状态 |
|------|------|------|
| `headless/core/.../pojo/JdbcDataSource.java` | JDBC 数据源封装 | 已在主干 |
| `headless/core/.../utils/JdbcDuckDbUtils.java` | DuckDB 工具类 | 已在主干 |
| `headless/server/.../service/impl/DownloadServiceImpl.java` | 导出服务（EasyExcel） | 已上线 |
| `ConnectorConfig.java`（待建） | 通用连接器配置模型 | 已有设计 |
| `ConnectorExecutor.java`（待建） | 连接器执行引擎 | 已有设计 |
| `s2_data_sync_config`（DDL 见上） | 同步任务配置表 | 待开发 |
| `s2_data_sync_execution`（DDL 见上） | 同步执行记录表 | 待开发 |
