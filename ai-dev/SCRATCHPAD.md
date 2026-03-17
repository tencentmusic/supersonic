# SCRATCHPAD.md — 跨会话连续性记录

## 使用规则

1. 每次 AI 会话结束前，复制下方模板让 AI 填写
2. 新记录插入文件顶部（最新在上）
3. 超过 10 条时压缩最老 7 条为「历史摘要」
4. 关键决策必须填写「AI 推理链」

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
