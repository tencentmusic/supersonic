---
status: implemented
module: feishu/server
key-files:
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/render/FeishuCardRenderer.java
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/render/FeishuCardTemplate.java
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/service/FeishuMessageSender.java
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/service/FeishuTokenManager.java
depends-on:
  - details/feishu/04-api-client.md
---

# 05 卡片渲染与消息发送

## 目标

将 SuperSonic `QueryResult` 转换为飞书消息卡片 JSON，并通过 `FeishuMessageSender` 回复用户。支持多种卡片类型以覆盖查询结果、错误、引导等场景。

## 当前状态

- [x] `FeishuCardRenderer`（Java 代码生成，非模板文件）已实现
- [x] `FeishuCardTemplate`（卡片元素构建工具类）已实现
- [x] 查询结果表格卡片（≤20 行 + 截断提示）已实现
- [x] 单值指标卡片已实现
- [x] 错误/权限不足/引导绑定卡片已实现
- [x] SQL 预览卡片（`renderSqlPreviewCard()`）已实现
- [x] Agent 列表卡片（`renderAgentListCard()`）已实现
- [x] 列名中文翻译（`QueryColumn.name` 用作显示名）已实现
- [x] 追问提示行（结果卡片底部，`FollowUpHintGenerator` 提供）已实现
- [x] `FeishuMessageSender`（IM API 封装）已实现
- [x] `FeishuTokenManager`（tenant_access_token 管理，TTL 110min，指数退避重试）已实现
- [ ] 敏感字段脱敏（查询结果渲染前检查 `QueryColumn` 脱敏标记，P2）
- [ ] 固定结构卡片模板外部化（Mustache + JSON 模板文件，P3）

## 设计决策

**为什么使用代码生成而非 JSON 模板文件？**
查询结果卡片（表格卡片）的结构是动态的——列数、列名、行数完全取决于 NL2SQL 返回的 `QueryResult`，无法用静态 JSON 模板表达。飞书卡片的嵌套结构（`column_set → column → markdown`）深度随列数变化，代码生成比模板引擎更易维护。

**固定结构卡片（帮助、错误、引导绑定）的改进方向**：
存为 JSON 模板文件（`resources/feishu/templates/*.json`），通过 Mustache 填充变量，非开发人员可通过飞书消息卡片搭建工具设计后导入，支持热更新。这是 P3 改进项。

**为什么动态卡片样式参数外部化？**
将颜色、字号、截断行数等参数提取到 `FeishuProperties`，修改样式无需改代码。`maxTableRows`（默认 20）、卡片颜色模板等均通过配置控制。

**为什么 FeishuMessageSender 与 FeishuDeliveryChannel 独立？**
`FeishuDeliveryChannel`（`headless/server`）用于定时报表推送（Webhook 出站），`FeishuMessageSender` 用于机器人交互回复（IM API 入站-回复模式）。两者调用方式不同，解耦避免相互影响。

**tenant_access_token 刷新策略**：
Caffeine 缓存 Token，TTL 设为 110 分钟（有效期 2 小时，提前 10 分钟刷新）。获取失败时指数退避重试（最多 3 次）。

## 接口契约

### FeishuCardRenderer 核心方法

```java
// 查询结果 → 表格/摘要/单值卡片
Map<String, Object> renderQueryResult(QueryResult result, List<String> followUpHints);

// SQL 预览卡片
Map<String, Object> renderSqlPreviewCard(String queryText, SemanticParseInfo parse);

// Agent 列表卡片（含频次排序 + 当前标记）
Map<String, Object> renderAgentListCard(List<AgentInfo> agents, int currentAgentId);

// 错误卡片
Map<String, Object> renderErrorCard(String message);

// 动态帮助卡片
Map<String, Object> renderHelpCard(String agentName, List<String> examples);
```

### 卡片类型选择逻辑

| 查询结果行数 | 卡片类型 | 展示内容 |
|------------|---------|---------|
| 1 行 1 列 | 指标卡片 | 大字号展示核心指标 |
| ≤ 20 行 | 表格卡片 | 完整数据表格 + 查询摘要 + 追问提示 |
| > 20 行 | 表格卡片（截断） | 前 20 行 + "共 N 行，点击导出完整数据" + 追问提示 |
| 空结果 | 提示卡片 | "查询完成，未找到匹配数据" |
| 查询失败 | 错误卡片 | 错误原因 + 建议 |
| 无权限 | 提示卡片 | 权限不足说明 |

### 表格卡片结构（示例）

```json
{
  "config": { "wide_screen_mode": true },
  "header": {
    "title": { "tag": "plain_text", "content": "查询结果" },
    "template": "blue"
  },
  "elements": [
    {
      "tag": "markdown",
      "content": "**查询**：查昨天北京的GMV\n**数据时间**：2025-01-14"
    },
    {
      "tag": "table",
      "columns": [
        { "name": "city", "display_name": "城市" },
        { "name": "gmv", "display_name": "GMV" }
      ],
      "rows": [...]
    },
    {
      "tag": "action",
      "actions": [
        {
          "tag": "button",
          "text": { "tag": "plain_text", "content": "导出 Excel" },
          "type": "primary",
          "value": { "action": "export", "format": "xlsx" }
        }
      ]
    },
    {
      "tag": "markdown",
      "content": "可继续追问，如「各城市的呢」、「看看最近7天的」"
    }
  ]
}
```

### FeishuMessageSender API

| 方法 | 飞书 API | 用途 |
|------|---------|------|
| `sendText(openId, text)` | `POST /im/v1/messages` | 发送纯文本消息 |
| `sendCard(openId, card)` | `POST /im/v1/messages` (interactive) | 发送消息卡片 |
| `sendFile(openId, fileKey)` | `POST /im/v1/messages` (file) | 发送文件 |
| `uploadFile(filePath)` | `POST /im/v1/files` | 上传文件获取 file_key |
| `replyText(messageId, text)` | `POST /im/v1/messages/{id}/reply` | 回复指定消息（文本） |
| `replyCard(messageId, card)` | `POST /im/v1/messages/{id}/reply` | 回复指定消息（卡片） |
| `sendToGroup(chatId, card)` | `POST /im/v1/messages` (chat_id) | 群聊发送 |

### FeishuTokenManager

```java
String getAccessToken();   // 获取有效 Token（优先缓存，TTL 110min）
String refreshToken();     // 强制刷新 Token
// 获取失败: 指数退避重试，最多 3 次
```

## 数据模型

无独立表。卡片 JSON 在内存中构建后直接通过飞书 IM API 发送。

**QueryColumn 字段脱敏**（P2 待实现）：
`QueryResult.columns[].sensitive` 标记字段是否敏感。`FeishuCardRenderer` 渲染前检查此标记，对敏感字段显示 `***`，并在群聊场景提示"含敏感数据，完整数据已发至私信"。

## 实现要点

**列名中文翻译**：使用 `QueryColumn.name`（业务显示名）而非 `bizName`（数据库列名）。SuperSonic 在返回 `QueryResult` 时已完成翻译。

**SQL 优先级**（`renderSqlPreviewCard()`）：`correctedS2SQL > parsedS2SQL > querySQL`，取第一个非空值展示。

**Agent 列表卡片**：每个 Agent 一段，含序号 + 名称（当前 Agent 标记 `*(当前)*`）+ 描述（截断 80 字符）+ 示例问题（`agent.examples` 取前 2 个）+ 非当前 Agent 显示 `` `/use <id>` `` 切换提示；Agent 间用分割线隔开。

**导出文件约束**：
- CSV 文件含 UTF-8 BOM（Excel 兼容）
- 文件大小校验 < 30MB
- `s2.feishu.export.max-rows`（默认 100000）控制最大导出行数

**飞书应用所需权限**（消息发送相关）：

| 权限标识 | 用途 |
|---------|------|
| `im:resource` | 文件上传（`uploadFile`） |
| `im:message:send_as_bot` | 以机器人身份发消息 |

## 待办

- [ ] 联调卡片渲染（真实 QueryResult → 卡片展示效果）
- [ ] 敏感字段脱敏：渲染前检查 `QueryColumn.sensitive` 标记（P2）
- [ ] 群聊场景敏感数据降级为单聊（P2）
- [ ] 固定结构卡片（帮助/错误/引导）提取为 Mustache JSON 模板（P3，支持热更新）
- [ ] 动态卡片样式参数（颜色、截断行数）完全外部化到 `FeishuProperties`（P3）
