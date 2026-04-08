# SCRATCHPAD.md — 跨会话连续性记录

## 使用规则

1. 每次 AI 会话结束前，复制下方模板让 AI 填写
2. 新记录插入文件顶部（最新在上）
3. 超过 10 条时压缩最老 7 条为「历史摘要」
4. 关键决策必须填写「AI 推理链」

---

## 2026-04-08　运营 OS 一期 Phase A 完成 + Code Review 修复 + Phase B 规划　关联切片：headless/server + webapp/Reports + webapp/TaskCenter + feishu/server

### 本次完成了什么

**Phase A 执行收尾（延续上次会话）：**
- 完成 Plan 2 (异常处置流) Tasks 17-19 验收 + Task 20 集成验证
- 后端编译 BUILD SUCCESS (23/23 modules), 10/10 单元测试通过

**Code Review 修复（5 项 Important/Suggestion）：**
- `FixedReportServiceImpl.subscribe()` 补 `setTenantId(user.getTenantId())` — INSERT 不走 TenantSqlInterceptor
- `AlertRuleServiceImpl.transitionEvent()` 加 `@Transactional` — 修 read-then-write 无锁问题
- `Reports/index.tsx` 默认 viewFilter 改为 `'subscribed'`，空则回退 `''` — 对齐 spec §1.8
- `Reports/index.tsx` 域名筛选改用 `allDomains` 独立 state — 修 filtered data 导致选项丢失
- `AlertEventDrawer.tsx` 移除未使用的 `executionId` prop

**"查看历史"功能补齐：**
- 后端：`GET /api/v1/fixedReports/{datasetId}/executions` — 按 datasetId 跨 schedule 查分页执行记录
- 前端：`ReportHistoryDrawer` 组件 + 表格操作列"查看历史"按钮 + 详情抽屉 onViewHistory 接入
- 编译验证通过

**Phase B 计划编写：**
- Plan 3: `docs/superpowers/plans/2026-04-08-business-topics.md` (10 tasks) — 经营主题 CRUD + 关联对象管理 + 前端页面
- Plan 4: `docs/superpowers/plans/2026-04-08-feishu-alert-callback.md` (7 tasks) — 飞书卡片按钮回写异常状态

### 关键决策
| 决策内容 | 选择方案 | 原因摘要 |
|---------|---------|---------|
| 经营主题与子对象关联方式 | 泛型 join table `s2_business_topic_item(topic_id, item_type, item_id)` | 比在每个 DO 加 topicId 灵活，支持 N:M，不侵入现有表结构 |
| 飞书卡片按钮粒度 | 一卡多事件 + "确认全部/接手全部" | 一事件一卡会刷屏；批量操作更符合"轻消费"定位 |
| 查看历史接口设计 | `GET /fixedReports/{datasetId}/executions` 而非复用 schedule 级 API | 固定报表工作台视角是 dataset，不是 schedule；用户不关心具体哪个 schedule 产生的执行 |
| Code review 中 countPendingEventsByRule SQL 优化 | 跳过 | Phase A 规模不需要 GROUP BY 优化，标注 suggestion 待后续 |
| Code review 中"查看历史"no-op | 实现而非跳过 | spec §1.3 明确要求"查看历史"作为主列表操作，补齐后 spec 覆盖更完整 |

### AI 推理链（关键决策必填）
决策：经营主题关联方式
1. 我读取了 spec §5.2："每个主题至少挂载固定报表集合、异常处置流集合、相关定时报表任务"
2. 我注意到"集合"意味着 N:M（一个报表可属于多个主题）
3. 我考虑了 (A) 在 ReportScheduleDO/AlertRuleDO 加 topicId FK 和 (B) 独立 join table
4. (A) 的问题：1:N 限制（一个对象只能属于一个主题）；需要 ALTER 已有表；固定报表是虚拟对象（无自己的表），没地方加 FK
5. 因此选择 (B) `s2_business_topic_item`，用 `item_type` + `item_id` 泛型引用，不侵入现有 schema

### 放弃的方案
| 方案描述 | 放弃原因 |
|---------|---------|
| 为每个 DO 加 topicId 列 | 固定报表是虚拟对象无表可加；限制 1:N |
| 飞书一事件一卡片 | 告警可能产生几十个事件，会刷屏 |
| 复用现有 ExecutionList 组件做"查看历史" | ExecutionList 按 scheduleId 查询，固定报表需按 datasetId 聚合 |
| `@Transactional` + 乐观锁 version 列 | 需要 ALTER TABLE 加 version 列 + 全量改 update 逻辑，Phase A 先用 @Transactional 兜底 |

### 遗留问题（下次会话继续）
- [ ] **Phase B 执行**：Plan 3 (经营主题) + Plan 4 (飞书回写) 待选择执行方式并实施
- [ ] **Phase C 规划**：每日经营驾驶舱 (§5.1) + 可信度与责任账本 (§5.6)
- [ ] **transitionEvent 乐观锁**：当前仅 @Transactional，高并发场景仍有 last-write-wins 风险，后续考虑 version 列
- [ ] **countPendingEventsByRule SQL 优化**：用 GROUP BY 替代全量加载到内存
- [ ] **所有代码未提交**：Phase A + Code Review 修复 + 查看历史 均未 git commit，需要整理提交
- [ ] **"最近查看"视图过滤**：spec §1.3 列了"全部/我订阅的/最近查看"三个视图，目前只实现了前两个

### Spec 变更建议
- 文件：`docs/product/运营数据工作台产品设计说明书.md` §5.5 内容：异常处置流应补充"影响主题"字段的数据来源说明——当前异常通过 AlertRule 触发，AlertRule 与主题的关联通过 topic_item 表，但 AlertEvent 本身不直接携带主题信息，需要 join 查询

---

## 2026-03-18　NL 定时报表 Code Review + 修复　关联切片：chat/server + ReportScheduleQuery/Executor

### 本次完成了什么
Code Review 发现 15 个问题（6 P1 + 5 P2 + 4 P3），修复其中优先级 P1/P2/部分 P3：

**ReportScheduleExecutor.java（P1 全修）**：
- P1: 补 `queryConfig`（从 parseInfo 提取 QueryStructReq，拒绝 SQL 路径）
- P3: 成功消息改中文
- P3: `Collections.EMPTY_MAP` → `Collections.emptyMap()`

**ReportScheduleQuery.java（P1 + 部分 P2）**：
- P1: Pause/Resume/Trigger 加归属权校验（`schedule.getOwnerId().equals(currentUserId)`）
- P1: `PENDING_CONFIRMATIONS` 键从 `Long dataSetId` → `String "userId_dataSetId"`（防跨用户覆盖）
- P1: `buildQueryConfig` 移除 SQL 路径（防硬编码历史日期写入 cron 配置）
- P2: `extractIntent` TRIGGER 移至最后检查（防"现在有哪些"误识别）
- P2: `SCHEDULE_ID_PATTERN` 要求 `#` 前缀（防数字误提取）

### 遗留问题（下次会话继续）
- [ ] **PENDING_CONFIRMATIONS 集群失效**：进程内 Map，Quartz 集群多节点不共享；失效模式：节点 A 存确认，节点 B 收"确认"消息 → 优雅失败（ERROR_NO_PENDING）。低优先级，HTTP 层通常有 session 亲和性。若需修复：新建 `report_pending_confirmation` 表存储
- [x] **双路径冲突**：调查发现 `ReportScheduleExecutor` 无 `@Component` 且从未被 `new` 实例化，是 dead class，仅常量被 `TemplateDeployedEventListener` 引用——伪问题，不需修复
- [x] **resolveDeliveryConfigIds 静默推送所有渠道**：改为只取第一个 enabled config（默认渠道）
- [x] **executeCreate Map 强转 NPE**：改为 `instanceof Number` 模式匹配 + 空值返回错误响应
- [x] **handleList 只查当前 datasetId**：改为传 `null`（列出租户全部调度，TenantSqlInterceptor 自动过滤）

---

## 2026-03-18　快照审计 + 安全修复 bugfix　关联切片：headless/server + webapp/ReportSchedule

### 本次完成了什么
- 修复 `ReportExecutionController.buildResultPreview()` 编译错误（方法签名不匹配，返回 null）
- 修复前端 service URL：`/api/semantic/report-executions` → `/api/v1/report-executions`
- P1：快照接口缺跨用户授权检查 → 加 `checkDataSetViewPermission`，无权限返回 403
- P1：空 `authResList` 被当作"全访问" → fail-closed，空列表 = 返回空预览（AG-12）
- P2：`ExecutionSnapshot` 存的是模板 SQL 而非渲染后 SQL → 新增 `renderedSql` 字段，存 `queryResp.getSql()`
- P2：进程内 `Semaphore` 在 Quartz 集群下无效 → 改为 DB 计数 `SELECT COUNT(*) WHERE status='RUNNING'`
- Code Review 发现 8 个新问题（2 P1 + 6 P2），见遗留问题

### 关键决策

| 决策内容 | 选择方案 | 原因摘要 |
|---------|---------|---------|
| 空 authResList 语义 | 空 = 无权限（fail-closed） | DataSetAuthServiceImpl 对无 authGroup 用户返回空 ArrayList，不能误判为"全访问" |
| 列级权限 admin 旁路 | `isSuperAdmin \|\| checkDataSetAdminPermission` → return null | Admin 应能看全量，不能被自己的 authGroup 限制 |
| 集群并发限制 | DB COUNT(RUNNING) | Quartz `isClustered=true`，进程内 Semaphore 跨节点无效；DB 是共享状态的唯一可信来源 |
| 渲染 SQL 存储 | `ExecutionSnapshotData.renderedSql` 新字段 | `ctx.queryConfig` 是模板原文，`queryResp.getSql()` 是实际执行语句，语义不同不能混用 |
| 向后兼容构造器 | 保留 2-arg 构造器，去掉 `@AllArgsConstructor` | 已有代码大量使用 `new ExecutionSnapshotData(ctx, previewRows)`，不能破坏签名 |

### AI 推理链

```
决策：进程内 Semaphore → DB COUNT
1. 读取 application.yaml:49，确认 org.quartz.jobStore.isClustered: true
2. ReportScheduleDispatcher 原用 ConcurrentHashMap<Long, Semaphore>，JVM 作用域
3. 多节点：节点 A 和 B 各有独立 Semaphore(5)，同一租户可在两节点各跑 5 个 = 实际 10 个
4. 进程内方案完全失效
5. 选 DB COUNT：selectCount WHERE tenant_id=? AND status='RUNNING'
   - 共享 DB 是 Quartz 集群的基础设施，天然跨节点一致
   - Quartz Job 不在 HTTP 链路，TenantSqlInterceptor ThreadLocal 为空，需显式加 tenantId 条件

决策：空 authResList = fail-closed
1. 读取 DataSetAuthServiceImpl.queryAuthorizedResources()
   - AuthorizedResourceResp.authResList 初始化为 new ArrayList<>()（非 null）
   - getAuthGroupsForUser() 对无 authGroup 用户返回空 list → authResList 仍为空
2. 原代码：authResList.isEmpty() → return null → 调用方跳过过滤 → 所有列暴露
3. AG-12 要求快照结果预览必须经过列级权限过滤
4. 修复：空 = Set.of() → filterColumns 将所有列移除 → 返回空预览
5. Admin 旁路：先判断 isSuperAdmin || checkDataSetAdminPermission，是则 return null
```

### 放弃的方案

| 方案描述 | 放弃原因 |
|---------|---------|
| 保留进程内 Semaphore + 加 DB COUNT 双重保护 | 过度工程；DB COUNT 已足够，Semaphore 徒增复杂度 |
| 重放时重新执行 SQL 获取结果 | 违反"审计 = 当时发生了什么"原则；结果随数据变化，不是快照 |
| authResList 为空时返回全量（宽松模式） | 直接违反 AG-12，生产会数据泄露 |

### 遗留问题（下次会话继续）

- [x] **AG-08 P1**：`QueryConfigParser.parseForAlert` SqlTemplateConfig → 加 `injectAlertLimit()` 子查询包裹 `LIMIT 1000`（有 LIMIT 则跳过）
- [x] **alertKey 超长 P1**：`AlertEvaluator.toDimensionString()` 截断到 200 字符
- [x] **parseQueryConfig 重复实现 P2**：`ReportExecutionOrchestrator.parseQueryConfig()` 改为委托 `QueryConfigParser.parse()`，移除重复逻辑和冗余 import
- [x] **FeishuDeliveryChannel 日期时区 P2**：`LocalDate.now(ZoneId.of("Asia/Shanghai"))`
- [x] **DeliveryContext.scheduleId 负数约定 P2**：新增 `alertRuleId` 字段，`AlertCheckDispatcher` 改用 `.alertRuleId(rule.getId())`（移除 `-rule.getId()` hack）
- [ ] **全链路联调**：无真实执行记录，监控/审计/Runbook 无法验证

### Spec 变更建议

- 文件：`ai-dev/ai-spec/domain/intent.md` 内容：AG-08 补充"SqlTemplateConfig 路径必须包含 LIMIT 子查询包裹或在 createRule 时校验拒绝"

---

## 2026-03-18　模板报表 P0 上线收口 SPEC Discovery　关联切片：headless/server + report + monitoring

### 关键决策
1. 联调范围收窄：单模板 × MySQL × QueryStructReq/SqlTemplateConfig × 飞书 → 先跑通一条链路再扩展
2. 审计回放 = 快照展示，不重新执行 SQL → 审计需要的是"当时发生了什么"，重新执行可能结果已变
3. 快照结果预览必须权限过滤 → AG-12，历史数据不能绕过列级权限
4. Prometheus tag 禁止高基数 → AG-13，防止 templateId 导致时序膨胀 OOM
5. 调度并发租户级上限 5 → AG-14，防止单租户慢查询独占 Quartz 线程池
6. 告警规则全部带 `for: 5m` → 防止瞬时抖动触发告警风暴（B-09）
7. 压测一期只做功能级基准测试 → 在 CI 用 JUnit 验证行为正确性，性能级压测后置
8. 重放 UI 放在执行历史列表页加 Drawer → 复用现有页面，不做独立审计中心

### AI 推理链
- 用户确认 Phase 1-3 代码全部在主干（29 个组件逐项验证），瓶颈不是功能缺失而是上线收口
- 联调是所有后续工作的前提：没有执行记录 → 监控无数据、审计无快照、Runbook 无场景
- 破坏者模式发现 6 个高严重级风险（B-01/02/04/06/08/11/12），其中 B-11/12 直接导致新增 AG-11/12
- 快照 SQL 脱敏和结果权限过滤是审计功能的前提，不做会产生数据泄露路径
- 实施顺序：联调(W1-2) → 监控(W2-3) → 审计回放(W3) → Runbook+压测(W3-4)，总计 4 周

### 产出文件
- `ai-dev/ai-spec/domain/intent.md` — 新增 AG-11 ~ AG-14 + 5 条敏感级别
- `docs/details/report/P0-上线收口实施方案.md` — 5 个收口任务的完整实施方案
- `docs/details/README.md` — 索引新增 P0 方案引用

---

## 2026-03-17　告警订阅 SPEC Discovery　关联切片：headless/server + report delivery

### 关键决策
1. queryConfig 禁止 QuerySqlReq → 堵住 SQL 注入攻击面（AG-06）
2. Cron 最小间隔 5 分钟 → 防 Quartz 过载（AG-07）
3. 共用线程池 + 30 秒超时 → 告警快进快出，不拆独立池（避免过度工程）
4. 连续失败 5 次自动禁用 + 通知 owner → 复用推送渠道 consecutive_failures 模式
5. 事件 90 天自动清理 → 覆盖 3 个月末对账周期

### AI 推理链
- 用户场景：信贷系统多合作方批量推数据，跑批成功但数据不全是盲区
- 首版只监控 1-2 个钉子户 → 规则数少，不需要独立线程池
- 误报是杀手 → 静默期 24h + 首版只监控钉子户 + 提供 :test 试运行
- 权限模型：管理员 + 分析师（限 DataSet），复用现有 RBAC

### 产出文件
- `ai-dev/ai-spec/domain/intent.md` — 新增 AG-06 ~ AG-10
- `ai-dev/ai-spec/domain/entities.md` — 新增 AlertRule / AlertExecution / AlertEvent
- `ai-dev/ai-spec/domain/state-machines.yaml` — 新增 alert_rule / alert_execution / alert_event_delivery
- `docs/details/report/06-alert-subscription.md` — 补入 SPEC Discovery 约束表

### 下一步
使用并行 Agent 实现：任务 1-2（DDL + DO）/ 任务 3（Evaluator）/ 任务 5-6（共享工具 + 推送扩展）可并行

---

## 2026-03-12　安全加固（第一阶段）　关联切片：common / headless / chat / auth / feishu / launchers

### 关键决策

| 决策内容 | 选择方案 | 原因摘要 |
|---------|---------|---------|
| AES 密钥向后兼容 | warn + fallback 默认值 | 升级平滑，不炸现有环境，后续可规划数据迁移 |
| 配置分层方式 | Spring Profile（dev/prd yaml） | 主文件只放通用配置，环境凭据由 profile 文件提供，保持 `${ENV:default}` 模式 |
| Profile 加载机制 | `include: ${S2_ENV:dev}` | DB 类型和运行环境是独立维度，active 控制数据库，include 控制环境，不耦合 |
| 线程池上下文传播 | 增强 ThreadMdcUtil + ContextAwareThreadPoolExecutor | 复用已有 MDC 包装基础设施，最小改动覆盖全部 6 个线程池 |
| AES 注入方式 | `@ConfigurationProperties` | 与 FeishuProperties 保持同一模式 |

### AI 推理链

```
决策：配置分层方式
1. 我读取了 application.yaml（主文件含 ${FEISHU_APP_ID:} 和 s2.encryption.*）
2. 用户指出：环境相关的凭据不应在主文件定义，应由 profile 文件提供
3. 第一次错误：在 -dev.yaml 里直接硬编码值 → 用户纠正应保持 ${ENV:default} 模式
4. 第二次错误：在主文件保留 ${ENV:} 空默认值 → 用户指出这是重复定义，profile 会覆盖
5. 最终：主文件删除环境凭据行，dev.yaml 用 ${ENV:开发默认值}，prd.yaml 用 ${ENV}

决策：Profile 加载机制
1. 用户发现 spring.profiles.active: ${S2_DB_TYPE:h2} 只激活数据库 profile
2. dev/prd profile 永远不会被加载，凭据配置不生效
3. 方案 A（include 分离）：两个维度独立，本地可 mysql+dev
4. 方案 B（group 聚合）：DB 和环境绑死，不灵活
5. 选择方案 A
```

### 放弃的方案

| 方案描述 | 放弃原因 |
|---------|---------|
| AES `@Value` 注解内置默认值 | Java 代码里仍有硬编码密钥，与项目 ConfigurationProperties 模式不一致 |
| .env + gitignore 管理凭据 | .env 只在 Docker Compose 生效，IDE 本地开发不方便 |
| 主 yaml 保留 `${ENV:}` 空默认值 | 与 profile 文件重复定义 |
| AES 强制配置启动失败 | 会炸现有环境 |

### 遗留问题（下次会话继续）

- [ ] `ChatParseReq`/`ChatExecuteReq`/`DatabaseReq` 缺少字段级校验注解（@Valid 是空操作）
- [ ] `QueryRecommendProcessor`、`DefaultQueryCache` 的 CompletableFuture 未包装
- [ ] Feishu `ThreadPoolTaskExecutor` 未替换为 ContextAware 版本
- [ ] AES 数据迁移到新密钥（方案 C）作为后续规划
- [ ] P1/P2 控制器 @Valid 覆盖（剩余约 100 个方法）

### Spec 变更建议

- 文件：CLAUDE.md 内容：补充 Spring Profile 分层约定（`active` = DB 类型，`include` = 运行环境），新增环境配置只加 profile yaml 不改主文件

---

## 记录模板 v4

---
日期：____ 任务类型：____ 关联切片：____
使用的 Prompt 版本：[如 generate-api/v2.0]

### 本次完成了什么

### 关键决策

| 决策内容 | 选择方案 | 原因摘要 |
|---------|---------|---------|

### AI 推理链（关键决策必填）

格式要求：每个关键决策，让 AI 按以下结构输出推理过程

```
决策：[决策名称]
1. 我读取了 [文件/规则]
2. 我注意到 [关键约束，引用原文]
3. 我考虑了 [方案A] 和 [方案B]
4. 方案A 的问题：[具体原因]
5. 因此我选择了 [方案B]，因为 [与约束的对应关系]
```

### 放弃的方案

| 方案描述 | 放弃原因 |
|---------|---------|

### 遗留问题（下次会话继续）

- [ ]

### Spec 变更建议

- 文件：____ 内容：____

### Prompt 效果反馈

版本：____ 评价：[好用 / 有问题] 具体：____
如发现问题，在对应 PROMPT-VERSIONS/CHANGELOG.md 中记录

---
