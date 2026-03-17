---
status: implemented
module: feishu/server
key-files:
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/handler/FeishuMessageRouter.java
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/handler/QueryMessageHandler.java
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/handler/ExportMessageHandler.java
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/handler/HelpMessageHandler.java
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/handler/TemplateListHandler.java
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/handler/HistoryMessageHandler.java
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/handler/PreviewMessageHandler.java
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/handler/UseAgentHandler.java
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/service/FeishuBotService.java
depends-on:
  - details/feishu/02-identity-mapping.md
  - details/feishu/04-api-client.md
---

# 03 消息处理链

## 目标

将飞书用户的文本消息路由到正确的处理器，执行查询/导出/帮助等操作，并将结果以卡片或文件形式回复。

## 当前状态

- [x] `FeishuMessageRouter`（7 个 Handler 路由）已实现
- [x] `QueryMessageHandler`（NL2SQL 查询 + 追问上下文）已实现
- [x] `ExportMessageHandler`（SQL 查询 → CSV → 飞书文件上传）已实现
- [x] `HelpMessageHandler`（动态帮助卡片）已实现
- [x] `TemplateListHandler`（Agent 列表 + 使用频次排序）已实现
- [x] `HistoryMessageHandler`（最近 10 次查询记录）已实现
- [x] `PreviewMessageHandler`（`/sql` SQL 预览，不执行）已实现
- [x] `UseAgentHandler`（`/use <agentId>` 切换 Agent）已实现
- [x] 群聊 @机器人 过滤（提取 @后文本）已实现
- [x] 追问上下文（chatId 共享 + dataSetId 传递 + `FollowUpHintGenerator`）已实现
- [ ] 卡片内筛选/下钻/翻页（阶段 3，待开发）

## 设计决策

**为什么消息路由基于文本前缀而非 NLU？**
消息路由采用简单规则匹配（`/` 前缀指令 + 关键词），而不是 LLM 意图识别。原因：延迟低（无需额外 LLM 调用）、可预测（规则明确）、错误率低（指令型交互规则清晰）。复杂意图路由是阶段 3 能力（引入 `DifyRoutingHandler`）。

**导出为什么不走 ExportTaskService？**
当前 `ExportMessageHandler` 使用轻量旁路：`queryBySql() → CSV → 飞书文件上传`，适合小数据量即时导出场景。模板报表子系统的 `ReportExecutionOrchestrator / ExportTaskService` 是统一主链路，后续应逐步收敛。详见 `04-api-client.md` 和 `05-card-rendering.md`。

**追问能力如何实现？**
三层机制：
1. `chatId` 共享（同一 user+agent 复用同一 SuperSonic chat session，NL2SQL 自动加载多轮上下文）
2. `dataSetId` 传递（`QueryMessageHandler` 缓存上次成功查询的 `dataSetId`，下次查询通过 `ChatParseReq.dataSetId` 传入）
3. `FollowUpHintGenerator` 动态生成追问提示（基于 `SemanticParseInfo` 的 5 类规则）

**帮助卡片为什么动态化？**
`HelpMessageHandler` 通过 `apiClient.getAgentList()` 获取当前 Agent 的 `examples` 字段，动态生成示例问题，不再硬编码，保证与 Agent 配置同步。

## 接口契约

### MessageHandler 接口

```java
public interface MessageHandler {
    boolean matches(String messageText);   // 路由匹配
    void handle(FeishuMessage msg, User user);  // 处理逻辑
}
```

### FeishuMessageRouter 路由规则（按优先级）

| 消息模式 | 示例 | 处理器 | 说明 |
|---------|------|--------|------|
| `/sql <文本>` | `/sql 查昨天GMV` | `PreviewMessageHandler` | 仅解析生成 SQL，不执行 |
| `/use <id>` | `/use 3` | `UseAgentHandler` | 切换默认 Agent |
| `/help` / `帮助` | `/help` | `HelpMessageHandler` | 返回动态帮助卡片 |
| `/export` / `导出` / `下载` | `导出` | `ExportMessageHandler` | 导出最近查询结果 |
| `/template` / `模板` | `/template` | `TemplateListHandler` | 列出可用 Agent 列表 |
| `/history` | `/history` | `HistoryMessageHandler` | 最近 10 次查询记录 |
| 其他 | `查昨天北京GMV` | `QueryMessageHandler` | 默认，调用 NL2SQL |

### FeishuBotService 核心流程

```
FeishuWsClient / FeishuEventController（事件接收）
  → FeishuBotService.handleEventAsync()     // 手动提交到 feishuExecutor 线程池
    → FeishuUserMappingService.resolveUser(openId)  // 身份映射
    → 限流检查 cacheService.isRateLimited()
    → TenantContext.setTenantId(tenantId)
    → FeishuMessageRouter.route(messageText)
    → handler.handle(msg, user)
```

### QueryMessageHandler

```java
void handle(FeishuMessage msg, User user) {
    // 1. getOrCreateChat(agentId, user) → chatId
    // 2. 从缓存读取 lastDataSetId
    // 3. apiClient.query(queryText, agentId, chatId, dataSetId, user)
    // 4. 保存 dataSetId + agentId 到 FeishuCacheService
    // 5. 写 s2_feishu_query_session
    // 6. 生成追问提示 FollowUpHintGenerator.generate(parseInfo)
    // 7. FeishuCardRenderer.renderQueryResult(result, hints)
    // 8. messageSender.replyCard(messageId, card)
}
```

### ExportMessageHandler

```java
void handle(FeishuMessage msg, User user) {
    // 1. 查最近 SUCCESS session → sqlText + datasetId
    // 2. apiClient.queryBySql(sql, datasetId, user) → 全量数据
    // 3. 写 CSV（含 UTF-8 BOM，Excel 兼容）
    // 4. 校验文件大小 < 30MB
    // 5. messageSender.uploadFile(filePath) → fileKey
    // 6. messageSender.sendFile(openId/chatId, fileKey)
}
```

### PreviewMessageHandler

```java
void handle(FeishuMessage msg, User user) {
    String queryText = msg.getContent().replaceFirst("(?i)/sql\\s+", "");
    ChatParseResp resp = apiClient.parse(queryText, msg.getAgentId(), user);
    SemanticParseInfo topParse = resp.getSelectedParses().get(0);
    // SQL 优先级: correctedS2SQL > parsedS2SQL > querySQL
    Map<String, Object> card = cardRenderer.renderSqlPreviewCard(queryText, topParse);
    messageSender.replyCard(msg.getMessageId(), card);
}
```

### TemplateListHandler

```java
void handle(FeishuMessage msg, User user) {
    // 1. apiClient.getAgentList(user) → 过滤 status=1
    // 2. 统计 s2_feishu_query_session.agent_id（SUCCESS 记录）使用频次
    // 3. 按频次降序排列（新用户按 API 原始顺序）
    // 4. cardRenderer.renderAgentListCard(agents, currentAgentId)
}
```

### UseAgentHandler

```java
void handle(FeishuMessage msg, User user) {
    int agentId = parseAgentId(msg.getContent()); // "/use 3" → 3
    // 验证 Agent 存在且在线
    userMappingService.updateDefaultAgent(openId, agentId);
    messageSender.replyText(msg.getMessageId(), "已切换到 [AgentName] 数据域");
}
```

### FollowUpHintGenerator（headless-api 公共工具类）

```java
// 基于 SemanticParseInfo 生成最多 3 条追问提示
List<String> hints = FollowUpHintGenerator.generate(parseInfo, rowCount, maxDisplayRows);
```

5 类规则按优先级：
1. 维度 drill-down（`dimensions + elementMatches`）
2. 筛选变更（`dimensionFilters`）
3. 指标切换（`metrics` 多指标时）
4. 日期变更（`dateInfo.mode/unit/period`）
5. 结果精炼（行数 + limit）

## 数据模型

### s2_feishu_query_session 表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 主键 |
| `feishu_open_id` | VARCHAR(64) | 飞书用户 Open ID |
| `feishu_message_id` | VARCHAR(64) | 飞书消息 ID |
| `query_text` | TEXT | 原始查询文本 |
| `query_result_id` | BIGINT | 关联的 s2_chat_query.id |
| `sql_text` | TEXT | 生成的 SQL（从 QueryResult 提取） |
| `dataset_id` | BIGINT | 数据集 ID（V22 新增，用于导出和追问） |
| `agent_id` | INT | 使用的 Agent ID（V22 新增，用于频次统计） |
| `row_count` | INT | 结果行数 |
| `status` | VARCHAR(20) | `PENDING` / `SUCCESS` / `FAILED` |
| `error_message` | TEXT | 错误信息 |
| `created_at` | DATETIME | 创建时间 |

索引：`KEY idx_open_id_created (feishu_open_id, created_at DESC)`

## 实现要点

**群聊场景**：
- `FeishuBotService` 检测 `chat_type=group`，过滤非 @机器人 消息
- 提取 @机器人 后的文本作为查询内容
- 回复消息时 @提问者，避免群内信息混淆
- 含敏感字段的结果仅回单聊

**异常处理**：

| 异常场景 | 处理策略 | 用户反馈 |
|---------|---------|---------|
| NL2SQL 解析失败 | 返回错误卡片 + 建议 | "未能理解您的查询，请尝试..." |
| 查询超时 | `queryTimeoutMs` 超时中断 | "查询超时，请缩小数据范围后重试" |
| 无数据权限 | `S2DataPermissionAspect` 拦截 | "您没有访问该数据集的权限" |
| 查询结果为空 | 正常返回空结果卡片 | "查询完成，未找到匹配数据" |
| 导出文件过大 | 超 30MB 提示 | "数据量超出限制，请缩小范围" |

**V22 迁移**：`s2_feishu_query_session` 新增 `dataset_id` 和 `agent_id` 列；`QueryMessageHandler` 在创建 session 时保存这两个字段。

## 待办

- [ ] 联调 QueryMessageHandler 端到端（NL2SQL + 卡片回复）
- [ ] 联调 ExportMessageHandler（CSV 文件上传 + 发送）
- [ ] 完善 HistoryMessageHandler（最近 10 条查询记录展示格式）
- [ ] 阶段 3：引入 `DifyRoutingHandler` 复杂意图路由
- [ ] 群聊场景增强：查询结果可见性控制、群粒度 Agent 绑定（P3）
