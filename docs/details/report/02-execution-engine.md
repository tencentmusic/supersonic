---
status: implemented
module: headless/server
key-files:
  - headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/ReportExecutionOrchestrator.java
  - headless/core/src/main/java/com/tencent/supersonic/headless/core/utils/SqlTemplateEngine.java
  - headless/api/src/main/java/com/tencent/supersonic/headless/api/pojo/QueryConfig.java
  - headless/api/src/main/java/com/tencent/supersonic/headless/api/pojo/SqlTemplateConfig.java
depends-on: []
---

# 执行引擎

## 目标

统一所有执行入口（Web / Agent / Schedule / API）的编排逻辑，确保权限注入、参数校验、SQL 渲染、结果持久化在同一链路中执行，消除分散编排带来的一致性隐患。

## 当前状态

已上线。`ReportExecutionOrchestrator` 已实现完整的 10 步编排链路；`SqlTemplateEngine` 已实现 ST4 渲染 + 安全校验；三路解析（SqlTemplateConfig / QueryStructReq / QuerySqlReq）已在 Orchestrator 中实现。

## 设计决策

### 执行上下文模型（ReportExecutionContext）

为统一手动执行、调度执行、Agent 调用等多种场景，引入不可变执行上下文对象。设计收益：执行过程可追溯、可回放；调度 / 手动 / Agent 执行链路统一；为失败重跑、历史重算提供基础能力。

```java
public class ReportExecutionContext {
    private final Long tenantId;
    private final Long templateId;
    private final Long templateVersion;
    private final Long datasetId;             // 执行目标数据集
    private final Long scheduleId;            // nullable，手动执行时为空
    private final Long operatorUserId;
    private final ExecutionSource source;     // WEB / AGENT / SCHEDULE / API

    private final QueryStructReq query;       // 完整的结构化查询请求
    private final Map<String, Object> resolvedParams;
    private final PermissionSnapshot permissionSnapshot;  // 执行时刻的行级+列级权限快照
    private final OutputConfig outputConfig;              // 输出格式 + 推送渠道配置
}
```

### SQL 查询方式：结构化查询优先，SQL 模板作为补充

现有体系**不采用手写 SQL 模板**，而是通过结构化请求自动生成 SQL：

```
用户选择 DataSet + Metric + Dimension + Filter
        │
        ▼
QueryStructReq.convert() → QuerySqlReq
        │
        ▼
SemanticLayerService → SQL 生成 → 数据源执行
```

优势：天然防 SQL 注入（参数不直接拼入 SQL）；自动适配数据源方言（MySQL / ClickHouse / PostgreSQL）；权限注入在 SQL 生成阶段自动完成。

对于无法通过结构化查询表达的复杂报表（如多表 UNION、窗口函数等），支持 SQL 模板引擎（ST4）。

### 三路解析策略

Orchestrator 在 `parseQueryConfig()` 中按优先级依次尝试三种解析路径：

1. **SqlTemplateConfig**（新增）：存在 `sqlTemplateConfig` 时，调用 `SqlTemplateEngine.render()` 渲染后转为 `QuerySqlReq`
2. **QueryStructReq**：标准结构化查询，语义层自动生成 SQL
3. **QuerySqlReq**：直接传入自定义 SQL

## 接口契约

### 执行入口

```
POST /api/v1/report/execute
    Request: ReportExecuteReq（templateId, params, outputConfig）
    Response: SemanticQueryResp（data, columns, total）
```

### 各入口调用方式

```java
// Web / API 入口
@PostMapping("/api/v1/report/execute")
public SemanticQueryResp execute(@RequestBody ReportExecuteReq req) {
    ReportExecutionContext ctx = contextBuilder.build(req, currentUser);
    return orchestrator.execute(ctx);
}

// Quartz Job 入口（轻量分派，不含业务逻辑）
public class ReportScheduleJob implements Job {
    public void execute(JobExecutionContext jctx) {
        Long scheduleId = jctx.getMergedJobDataMap().getLong("scheduleId");
        dispatcher.dispatch(scheduleId);  // → 构建 context → orchestrator.execute(ctx)
    }
}

// Agent 入口
agentTool.invoke(params) → contextBuilder.buildFromAgent(params) → orchestrator.execute(ctx)
```

## 数据模型

### 运行时查询参数（QueryStructReq）

| 设计概念 | 现有实现 | 说明 |
|---------|---------|------|
| 日期参数 | `DateConf`（RECENT / LAST / BETWEEN） | 支持相对日期和绝对日期范围 |
| 维度过滤 | `List<Filter> dimensionFilters` | 支持 =, IN, LIKE, >, < 等操作符 |
| 指标过滤 | `List<Filter> metricFilters` | HAVING 条件 |
| 分组维度 | `List<String> groups` | GROUP BY 字段 |
| 聚合方式 | `List<Aggregator> aggregators` | SUM / COUNT / AVG / MAX / MIN |
| 排序 | `List<Order> orders` | ORDER BY |
| 分页 | `Long limit` | 行数限制 |

### SqlTemplateConfig（SQL 模板配置）

```java
@Data
public class QueryConfig implements Serializable {
    private DetailTypeDefaultConfig detailTypeDefaultConfig = new DetailTypeDefaultConfig();
    private AggregateTypeDefaultConfig aggregateTypeDefaultConfig = new AggregateTypeDefaultConfig();

    // 新增：SQL 模板配置（与结构化查询互斥，优先级高于 detailType/aggregateType）
    private SqlTemplateConfig sqlTemplateConfig;
}

@Data
public class SqlTemplateConfig implements Serializable {
    private String templateSql;          // SQL 模板文本（ST4 语法）
    private List<SqlVariable> variables; // 模板变量定义（复用现有 SqlVariable）
}
```

SQL 模板示例（ST4 语法）：

```sql
SELECT city, SUM(gmv) AS gmv
FROM dwd_order_di
WHERE dt BETWEEN '{{start_date}}' AND '{{end_date}}'
{{#if city}}
AND city IN ({{city}})
{{/if}}
GROUP BY city
```

## 实现要点

### 编排主流程（10 步）

```
ReportExecutionOrchestrator.execute(ReportExecutionContext ctx)
 │
 ├── 1. buildContext        → 解析入口参数，构建 ReportExecutionContext
 ├── 2. validateTemplate    → 校验模板状态（必须 Online / Deployed）
 ├── 3. validateParams      → 运行时参数强校验（类型校验 + 必填检查）
 ├── 4. injectPermission    → 注入数据权限（行级 / 字段级）
 ├── 5. renderSQL           → 渲染 SQL（结构化生成或模板渲染）
 ├── 6. executeQuery        → 执行查询
 ├── 7. persistExecution    → 写入 s2_report_execution（含 execution_snapshot）
 ├── 8. routeExport         → 判断同步/异步导出（EXPLAIN 预估行数 vs 阈值）
 ├── 9. formatOutput        → 输出格式化（Excel / CSV / JSON）
 └── 10. deliverOutput      → 返回 / 推送 / 导出（异步时写入 s2_export_task）
```

步骤映射：

| 步骤 | Orchestrator 方法 | 实现 | 状态 |
|------|-------------------|---------|------|
| 1. buildContext | `contextBuilder.build()` | `ReportExecutionContextBuilder` | ✅ 已实现 |
| 2. validateTemplate | `validateTemplate()` | `SemanticTemplateServiceImpl.getTemplateById()` | ✅ 已实现 |
| 3. validateParams | `validateParams()` | 类型校验 + 必填检查 | ✅ 已实现 |
| 4. injectPermission | `injectPermission()` | `S2DataPermissionAspect` + `PermissionSnapshot` | ✅ 已实现 |
| 5. renderSQL | `renderSQL()` | `QueryStructReq.convert()` + `SemanticLayerService` | ✅ 已实现 |
| 6. executeQuery | `executeQuery()` | `QueryExecutor` 多数据源适配 | ✅ 已实现 |
| 7. persistExecution | `persistExecution()` | `s2_report_execution` 表 | ✅ 已实现 |
| 8. routeExport | `routeExport()` | `RowCountEstimator` EXPLAIN 估算 → 同步/异步分流 | ✅ 已实现 |
| 9. formatOutput | `formatOutput()` | Excel + CSV + JSON | ✅ 已实现 |
| 10. deliverOutput | `deliverOutput()` | `ReportDeliveryService` 五渠道推送 | ✅ 已实现 |

### SqlTemplateEngine 核心组件

```java
@Component
public class SqlTemplateEngine {

    /**
     * 渲染 SQL 模板。
     * 1. 预处理条件块 {{#if var}}...{{/if}} → ST4 条件语法
     * 2. 使用 ST4 渲染变量
     * 3. SQL 安全校验（复用 SqlVariableParseUtils.checkSensitiveSql）
     */
    public String render(String templateSql, Map<String, Object> params) {
        String st4Sql = convertConditionals(templateSql);
        ST st = new ST(st4Sql, '$', '$');
        params.forEach(st::add);
        String rendered = st.render();
        SqlVariableParseUtils.checkSensitiveSql(rendered);
        return rendered;
    }
}
```

### 安全措施

| 安全措施 | 现有实现 | 说明 |
|---------|---------|------|
| 禁止 DDL/DML | `SqlEditEnum` 校验 | 查询请求仅允许 SELECT |
| 禁止多语句 | SQL 解析层拦截 | JSqlParser 解析确保单语句 |
| 只读执行 | 数据源配置 | 查询使用只读连接池（Druid） |
| 行数限制 | `Constants.DEFAULT_DOWNLOAD_LIMIT` | 防止大结果集 |
| DDL/DML 拦截（模板渲染后） | 复用 `SqlVariableParseUtils.checkSensitiveSql()` | — |
| 变量类型校验 | 复用 `SqlVariable.valueType`（STRING/NUMBER/EXPR） | — |
| 注入防护 | STRING 类型自动加引号，EXPR 类型走敏感词校验 | — |

### 影响文件清单

| 文件 | 变更 | 状态 |
|------|------|------|
| `headless/api/.../pojo/QueryConfig.java` | 新增 `sqlTemplateConfig` 字段 | ✅ 已实现 |
| `headless/api/.../pojo/SqlTemplateConfig.java` | SQL 模板配置 POJO | ✅ 已实现 |
| `headless/core/.../utils/SqlTemplateEngine.java` | 模板渲染引擎（ST4 + `checkSensitiveSql`） | ✅ 已实现 |
| `headless/server/.../ReportExecutionOrchestrator.java` | 三路解析 + 完整 10 步编排 | ✅ 已实现 |
| `headless/core/pom.xml` | 引入 `org.antlr:ST4:4.0.8` | ✅ 已实现 |

## 待办

- 前端 `SemanticTemplate/TemplateFormModal.tsx` 增加 SQL 模板编辑器（可选，待排期）
- 参数模板高级约束能力：动态枚举来源统一、复杂范围规则
- 参数变更兼容策略：模板版本升级后老调度任务参数兼容
