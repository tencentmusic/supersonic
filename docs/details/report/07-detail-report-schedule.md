---
status: planned
module: chat/server
key-files:
  - chat/server/src/main/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ReportScheduleQuery.java
  - headless/chat/src/main/java/com/tencent/supersonic/headless/chat/utils/QueryReqBuilder.java
  - headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/QueryConfigParser.java
  - headless/core/src/main/java/com/tencent/supersonic/headless/core/utils/SqlGenerateUtils.java
  - common/src/main/java/com/tencent/supersonic/common/util/DateModeUtils.java
depends-on:
  - docs/details/report/02-execution-engine.md
  - docs/details/report/04-scheduler-delivery.md
---

# 明细报表定时推送设计方案

## 目标

让用户可以基于一条刚刚执行过的明细查询，创建一个每天/每周定时执行并投递结果的任务，同时保证查询快照可回放、日期语义稳定、结果规模可控。

本方案解决两个问题：

1. 系统正式支持"明细报表定时推送"，而不是只对聚合报表友好。
2. 固定时间区间在调度执行时必须被冻结，不能在后续执行中被静默回退为默认 T-1。

## 背景与问题定义

当前调度系统底层已经具备以下能力：

- 使用 `query_config` 持久化调度查询配置
- Quartz 动态调度
- 报表导出
- 多渠道投递
- 执行快照审计

但在"基于刚才查询结果创建调度"这条 NL 链路里，现有实现更偏向聚合查询，具体问题包括：

- 明细查询若没有 `metrics`，可能无法被识别为可订阅查询
- 查询快照可能未完整保留 `dimensions / orders / limit / dateInfo`
- 固定日期区间在调度执行时可能被重算为默认 recent/T-1
- 缺少对 `dateField`、固定区间、明细规模的创建期校验

## 非目标

本方案不覆盖以下内容：

- 飞书私聊"只发给我本人"的触达模型
- 原始 SQL 查询的完整调度支持
- 超大明细结果的分片下载或异步对象存储扩展
- Dashboard 编排式调度
- 导出文件迁移至对象存储或分层归档

## 能力定义

### 支持的调度来源

支持以下查询来源创建调度：

- 聚合报表查询
- 明细报表查询
- 带筛选条件、排序、limit 的明细查询

其中"明细报表查询"定义为：

- `QueryType` 为 detail / non-aggregate
- 查询结果以原始记录行为主
- 不要求必须存在 `metrics`

### 不支持的来源

当前阶段以下来源不支持直接创建调度：

- 无法序列化为稳定 `QueryStructReq` 的原始 SQL
- 依赖临时上下文且不可复现的查询
- 日期语义不明确的查询

对不支持场景必须返回明确错误，而不是创建一个行为不确定的任务。

## 设计原则

### 原则 1：快照回放优先于自然语言重理解

一旦调度任务创建成功，后续执行必须基于保存下来的结构化查询快照回放，不重新从自然语言理解筛选条件、排序、时间区间。

### 原则 2：固定区间冻结，相对区间滚动

- `BETWEEN` / `LIST`：冻结
- `RECENT` / `AVAILABLE`：滚动

例如：

- 用户查询 `2025-03-04` 到 `2025-03-10` 的明细并创建每天 15:30 推送
- 后续任意一天执行时，都必须仍使用 `2025-03-04 ~ 2025-03-10`

### 原则 3：明细结果默认文件投递

明细结果通常更适合：

- `CSV`
- `XLSX`

系统不要求在消息正文里完整展示明细行，只需投递文件或可下载链接。

### 原则 4：创建期显式校验，执行期不做静默兜底修正

如果调度任务的日期语义、快照结构、limit 不完整，应在创建时拒绝，而不是等到执行时按默认规则猜测。

### 原则 5：执行与投递独立

执行成功即记录 SUCCESS，投递失败不影响执行状态。投递有独立的重试和 consecutive failure 机制。已生成的文件可通过手动重投递入口重新发送，不需要重新执行查询。

## 数据模型与快照定义

### 调度任务中的 query_config

明细报表调度的 `query_config` 继续复用 `QueryStructReq`，但要求以下字段必须被稳定保存：

- `datasetId`
- `queryType`
- `dimensions`
- `groups`
- `aggregators`
- `dimensionFilters`
- `metricFilters`
- `orders`
- `limit`
- `offset`
- `dateInfo`

其中对于明细查询：

- `dimensions` 应保留结果列
- `orders` 应保留排序规则
- `limit` 应保留原始限制
- `dateInfo` 应保留时间语义

### 日期快照规则

| dateMode | 创建时要求 | 执行时行为 |
|------|------|------|
| `BETWEEN` | 必须有 `dateField/startDate/endDate` | 固定使用保存值 |
| `LIST` | 必须有 `dateField/dateList` | 固定使用保存值 |
| `RECENT` | 必须有 `dateField/unit/period` | 每次执行滚动计算 |
| `AVAILABLE` | 必须有 `dateField` | 每次执行基于可用时间计算 |
| `ALL` | 明细调度禁止使用 | — |

#### ALL 模式限制

明细调度不允许使用 `ALL` 模式。原因：

- ALL + 定时调度 = 每天全表扫描，数据量只增不减
- 如果用户需要全量数据，说明查询条件不明确，不适合做成定时任务
- `BETWEEN` 和 `RECENT` 已覆盖"固定区间"和"滚动窗口"两种合理场景

创建时校验拒绝，返回明确错误："明细调度必须指定日期范围，请选择固定区间或最近 N 天"。

### 明细结果规模约束

明细调度复用数据集自身的 `DetailTypeDefaultConfig.limit` 限制（默认 500，可在数据集上配置）。

规则：

- 若查询未显式设置 `limit`，使用数据集的明细默认 limit
- 若 `limit` 超过数据集允许阈值，按数据集限制截断
- 不单独为调度设计额外的阈值切换逻辑

### 导出文件生命周期

调度导出的 CSV/XLSX 文件保留最近 7 天，过期自动清理。

规则：

- `@Scheduled` 每天凌晨执行，扫描 `exportDir` 删除 mtime > retention days 的文件
- 保留天数可配置：`supersonic.export.retention-days=7`
- 不依赖数据库记录文件生命周期，文件名中已包含时间戳（`report_{id}_{yyyyMMddHHmmss}.xlsx`），直接按文件时间判断
- 7 天窗口覆盖投递重试周期 + 手动重投递反应时间

## 整体方案

### 方案概览

```text
创建路径 A：NL 指令
  -> ReportScheduleQuery 识别 CREATE
  -> 解析最近一条可订阅查询（无则返回"请先执行一次查询"）
  -> 生成 QueryStructReq 快照
  -> 校验明细调度可创建
  -> 保存 pending confirmation
  -> 用户确认
  -> 保存 s2_report_schedule.query_config

创建路径 B：Web 表单
  -> ScheduleForm 选择数据集
  -> 自动加载用户可见列（排除敏感字段）+ dateField 从 partition dimension 自动带入
  -> 配置日期模式、调度频率、投递渠道
  -> 服务端校验
  -> 保存 s2_report_schedule.query_config

执行路径（两种创建方式共享）：
  -> Quartz 定时触发
  -> QueryConfigParser 反序列化 QueryStructReq（原样返回，不转 SQL）
  -> StructQueryParser 按快照生成 SQL（dateInfo 严格回放）
  -> 执行查询
  -> 同步导出 CSV/XLSX
  -> 投递渠道（独立重试）
```

## 详细设计

### 一、创建链路

#### 1. 可订阅判定

需要调整 NL 调度插件对"可订阅查询"的判定逻辑。

当前实现的问题是：

- 过度依赖 `metrics`
- 对 detail query 不友好

改进规则：

- 如果 parseInfo 能构建出稳定 `QueryStructReq`，即可视为可订阅
- 明细查询不要求 `metrics` 非空
- 原始 SQL 仅在可结构化回放时支持，否则明确拒绝

建议修改文件：

- `chat/server/.../ReportScheduleQuery.java`

建议新增方法：

- `isSchedulableStructQuery(parseInfo)`
- `buildSchedulableStructReq(parseInfo)`

#### 1.1 NL 路径无上下文兜底

当 chat 上下文中没有最近的可订阅查询时（如用户一上来就说"创建推送"），直接返回明确提示："请先执行一次查询再创建调度"。不尝试在 NL 路径里重建配置流程。

#### 2. 查询快照构建

对明细查询，构建 `QueryStructReq` 时必须保留：

- 结果列
- 过滤条件
- 时间条件
- 排序
- limit

建议校验 `QueryReqBuilder.buildStructReq(parseInfo)` 对 detail query 的行为；若不足，则补齐 detail-specific 逻辑。

建议修改文件：

- `headless/chat/.../QueryReqBuilder.java`

#### 3. 创建期校验

创建调度前新增结构化校验：

- `BETWEEN` 必须有 `dateField/startDate/endDate`
- `RECENT` 必须有 `dateField/unit/period`
- `ALL` 模式明细调度直接拒绝
- detail query 必须有可接受的 `limit`（受数据集 `DetailTypeDefaultConfig.limit` 约束）
- 不支持的 SQL 模式必须明确报错

建议新增校验器：

- `ReportScheduleQueryConfigValidator`

放置位置建议：

- `headless/server` 或 `chat/server/plugin/support/reportschedule`

### 二、执行链路

#### 1. QueryConfig 解析

调度执行时，对于 `QueryStructReq` 类型的 `query_config`，必须直接返回结构化请求对象，不再提前转成 `QuerySqlReq`。

原因：

- 结构化路径会保留 `dateInfo`
- 转成 SQL 的旧路径可能触发默认日期兜底
- 明细列也更适合由 translator 按数据集 schema 生成

建议修改文件：

- `headless/server/.../QueryConfigParser.java`

目标行为：

- `SqlTemplateConfig` -> 渲染 SQL
- `QueryStructReq` -> 原样返回
- `QuerySqlReq` -> 仅在明确允许时使用

#### 2. 日期 SQL 生成

固定区间必须在执行时严格回放，不允许退化为 recent。

建议收敛为以下规则：

- `BETWEEN` -> `betweenDateStr(dateInfo)`
- `LIST` -> `listDateStr(dateInfo)`
- `RECENT` -> 允许 `defaultRecentDateInfo(dateInfo)`
- `ALL` -> 不追加日期过滤（仅聚合调度可用）

建议修改文件：

- `headless/core/.../SqlGenerateUtils.java`
- `common/.../DateModeUtils.java`

#### 3. 明细导出

调度导出保持同步方式（EasyExcel 直接写文件）。原因：

- Quartz 线程本身已是异步执行，不阻塞用户
- 结果规模受数据集 `DetailTypeDefaultConfig.limit` 约束，在可控范围内
- 不需要接入 `03-export-async` 的异步 worker pool

导出格式无需区分"明细/聚合"两套链路，但应在调度创建时优先默认：

- 明细：`CSV` 或 `XLSX`
- 聚合：保持现有默认

前端可在选择明细调度时提示：

- 明细结果建议使用 `CSV/XLSX`

#### 4. 执行期异常处理

执行期不做静默兜底修正。以下异常统一依赖现有 consecutive failure 机制处理：

- **Owner 权限漂移**：Owner 失去数据集访问权后，`S2DataPermissionAspect` 执行期校验失败，记录 FAILED + errorMessage 写明具体原因（如 "owner 无数据集访问权限"）。连续失败达阈值后自动 disable schedule。不单独加权限预检链路。
- **Schema 变更**：数据集列改名/删除后，SQL 执行报 "column not found"，同样走失败 + consecutive failure 流程。不做 schema 兼容性预检，数据库报错比预检更准确。

### 三、前端设计

#### 1. ScheduleForm

需要增强手工创建/编辑调度的表达能力，分为日期配置和列配置两部分。

##### 日期配置

- 日期模式选择：`BETWEEN`（固定区间）/ `RECENT`（最近 N 天）
- `ALL` 模式对明细调度不可选
- `BETWEEN` 模式下 `dateRange` 必填
- `RECENT` 模式下 `recentUnit` 必填
- `dateField` 从数据集的 partition dimension 自动带入
- **dateField 缺失兜底**：若数据集未配置 partition dimension，显示警告文案"该数据集未配置日期分区，请手动输入日期字段名"，同时回退显示 dateField 手动输入框

##### 列配置（Web 表单 queryConfig 构建规则）

Web 表单创建明细调度时，列选择规则：

- 自动获取当前用户对该数据集可见的所有列
- 排除标记为敏感字段的列
- 如果数据集未配置敏感字段过滤，则取全部列
- 用户无需手动选列，系统自动构建 `dimensions` 列表
- **列信息对用户不可见**，不在表单上展示列名列表。用户可在执行快照中查看实际查询的列

建议修改文件：

- `webapp/.../ScheduleForm.tsx`

##### queryConfig 构建流程

```text
Web 表单提交时：
  1. 从数据集 schema 获取用户可见列（排除敏感字段）→ dimensions
  2. 从数据集 partition dimension 获取 dateField
  3. 根据日期模式构建 dateInfo
  4. 组装完整 QueryStructReq：
     {
       datasetId, queryType: "DETAIL",
       dimensions, groups: dimensions,
       dateInfo: { dateMode, dateField, startDate, endDate, ... },
       limit: 数据集默认明细 limit
     }
  5. 序列化为 queryConfig JSON
```

#### 2. 投递管理

在 ExecutionSnapshotDrawer 的"推送记录"表格中，为投递失败的记录增加"重试"操作：

- **按钮位置**：推送记录表的操作列，仅投递状态为 FAILED 时显示
- **确认交互**：Popconfirm "确定重新推送到该渠道？"
- **执行行为**：不重新执行查询，使用已生成的文件重新调用该渠道的投递服务
- **成功反馈**：`message.success('已重新推送')` + 刷新推送记录表状态
- **失败反馈**：`message.error('推送失败: ' + errorMessage)`
- **按钮状态**：推送中 disabled 并显示 loading spinner，防止重复点击
- 归入 Phase 3 体验增强

#### 3. 创建成功反馈

创建调度成功后的用户反馈：

- `message.success('调度任务创建成功')` toast 提示
- 关闭 ScheduleForm Modal
- 刷新任务列表

#### 4. 错误提示

##### 表单校验错误

表单校验使用 Ant Design Form.Item 内联错误（红色文字在字段下方），与现有表单一致：

- dateField 为空且未自动带入："请输入日期字段名"
- BETWEEN 模式未选日期范围："请选择日期范围"
- RECENT 模式未填天数："请输入天数"

##### 业务错误提示

对不支持场景在 NL 聊天消息或 API 响应中返回明确文案：

- 当前查询暂不支持创建定时推送，请先保存为结构化报表
- 当前查询缺少明确日期字段，无法创建定时任务
- 明细调度不支持"全部数据"模式，请选择固定区间或最近 N 天
- 请先执行一次查询再创建调度（NL 路径无上下文时）

## 接口与校验策略

### 后端创建接口校验

无论任务从 Web 表单还是 NL 创建，都在服务端统一校验 `query_config`：

- 不依赖前端单独保证正确性
- 防止脏数据直接入库

实现位置：

- `ReportScheduleServiceImpl.createSchedule/updateSchedule` 内联校验（不建独立 validator 类）

### 失败策略

校验失败时：

- 返回 4xx
- 附清晰错误消息
- 不创建 Quartz job

## 测试方案

测试按 Phase 分配，确保每个 Phase 交付时自带回归测试。

### Phase 1 测试（与 Phase 1 代码同步交付）

#### `QueryConfigParserTest`（新建）

- `QueryStructReq` 被 parse 后仍是 `QueryStructReq`（不转为 `QuerySqlReq`）
- `dateInfo.BETWEEN` 不被降级，startDate/endDate 保持原值
- 空 queryConfig 抛 `IllegalArgumentException`
- `SqlTemplateConfig` 正常渲染为 `QuerySqlReq`

#### `DateModeUtilsTest`（新建或扩展）

- RECENT 模式 + dateDate=null → 调用 `defaultRecentDateInfo`，返回非空日期条件
- BETWEEN 模式 → `betweenDateStr` 返回固定日期
- dateInfo=null → 返回空字符串
- dateField=null → 返回空字符串

#### `SqlGenerateUtilsTest`（新建）

- `getSelect()` groups + aggregators 为空 → 返回 `*`
- `getSelect()` 有 groups → 返回 group 列名
- `getDateWhereClause()` BETWEEN + dateDate=null → 返回固定日期 WHERE
- `getDateWhereClause()` RECENT + dateDate=null → 返回动态日期 WHERE
- `generateWhere()` dateInfo=null → 不追加日期条件

### Phase 2 测试（与 Phase 2 代码同步交付）

#### `ReportScheduleQueryTest`（扩展）

- 明细查询（无 metrics）可以创建 pending confirmation
- 保存的 `queryConfig` 包含 `dimensions/orders/limit/dateInfo`
- 固定区间 `2025-03-04 ~ 2025-03-10` 被完整带入
- chat 上下文无可订阅查询时返回明确提示

#### `ReportScheduleServiceImplTest`（扩展）

- 缺失 `dateField` 的 BETWEEN/RECENT 任务被拒绝
- 缺失 `startDate/endDate` 的 `BETWEEN` 任务被拒绝
- `ALL` 模式明细调度被拒绝
- `limit` 超过数据集阈值时被截断

### 集成测试（Phase 2 交付后）

### 集成测试

建议补一条端到端场景：

1. 执行一条明细查询，条件为 `workday between 2025-03-04 and 2025-03-10`
2. 创建每天 15:30 推送
3. 手动 trigger
4. 断言最终执行 SQL 保持固定区间

## 分阶段实施

### Phase 1：修复固定区间回退问题

目标：先止血，避免固定区间被重算成 T-1。

实施内容：

- `QueryConfigParser`：返回 `QueryStructReq` 原样，不转 SQL
- `SqlGenerateUtils`：空 select 时返回 `*`
- `DateModeUtils`：RECENT 模式 null dateDate 时 fallback 到 `defaultRecentDateInfo`
- 回归测试

### Phase 2：支持明细查询创建调度

目标：让 NL 和 Web 都能创建明细调度任务。

实施内容：

- `ReportScheduleQuery`：放开明细查询可订阅判定 + NL 无上下文兜底提示
- `QueryReqBuilder`：补齐 detail-specific 快照构建
- 服务端 validator：dateField/dateMode/limit 校验 + ALL 模式拦截
- 前端 ScheduleForm：dateField 自动带入 + 列自动获取 + 日期模式校验
- 导出文件清理：`@Scheduled` 定时清理 + 可配置保留天数

### Phase 3：体验增强

目标：让明细调度更易用。

实施内容：

- 明细默认文件格式推荐
- 手动重投递入口（不重新执行查询）
- UI 明确区分"固定区间/最近 N 天"

## 风险与取舍

### 风险 1：原始 SQL 兼容性

如果继续允许 SQL 模式调度，行为边界会变复杂，尤其是日期快照和权限回放。

取舍：

- 当前阶段优先支持结构化查询
- 原始 SQL 明确限制

### 风险 2：明细结果过大

无上限的明细调度会造成：

- 查询成本高
- 导出时间长
- 投递失败率升高

取舍：

- 复用数据集 `DetailTypeDefaultConfig.limit` 限制
- ALL 模式明细调度直接禁止

### 风险 3：前后端校验不一致

若只在前端校验，仍可能有非法任务通过 API 落库。

取舍：

- 后端校验为准
- 前端仅做体验增强

### 风险 4：Owner 权限变更

Owner 创建调度后可能丧失数据集访问权，导致后续执行全部失败。

取舍：

- 不单独加权限预检链路，复用 consecutive failure 自动 disable 机制
- errorMessage 写明具体权限失败原因，方便 Owner 自查
- 权限恢复后 resume schedule 即可恢复执行

### 风险 5：Schema 演进

数据集列改名/删除后，保存的 queryConfig 可能引用不存在的列。

取舍：

- 不做 schema 兼容性预检，执行时 SQL 报 "column not found" 更准确
- 靠 consecutive failure 机制自动 disable
- execution 记录的 errorMessage 包含具体的列错误信息

## 验收标准

- 用户可以基于一条明细查询创建定时推送任务
- 固定区间在后续执行中不发生漂移
- 最近 N 天在后续执行中按预期滚动
- ALL 模式明细调度在创建时被拒绝
- 明细调度的 `query_config` 可稳定回放
- 非法日期配置在创建期被拦截
- Web 表单自动获取可见列和 dateField，不需要用户手动输入
- 导出文件保留 7 天后自动清理
- NL 路径无上下文时返回明确提示
- 执行成功但投递失败时可手动重推送
- 关键测试全部通过

## 与现有文档关系

本方案补充以下文档中未明确的行为边界：

- `04-scheduler-delivery.md`：补充明细调度语义与日期冻结规则
- `02-execution-engine.md`：补充结构化快照回放要求
- `03-export-async.md`：明确调度导出保持同步路径，不接入异步 worker pool
- `05-governance.md`：导出文件生命周期管理（7 天保留 + 定时清理）
- `backlog.md`：后续可将"明细调度支持"从待办转为实施项

## GSTACK REVIEW REPORT

| Review | Trigger | Why | Runs | Status | Findings |
|--------|---------|-----|------|--------|----------|
| CEO Review | `/plan-ceo-review` | Scope & strategy | 0 | — | — |
| Codex Review | `/codex review` | Independent 2nd opinion | 0 | — | — |
| Eng Review | `/plan-eng-review` | Architecture & tests (required) | 1 | CLEAN (PLAN) | 2 issues (scope reduced: validator inlined; test phases assigned), 0 critical gaps |
| Design Review | `/plan-design-review` | UI/UX gaps | 1 | CLEAN (FULL) | score: 5/10 → 7/10, 5 decisions |

- **UNRESOLVED:** 0
- **VERDICT:** ENG + DESIGN CLEARED — ready to implement
