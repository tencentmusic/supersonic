---
status: planned
module: feishu/server
key-files: []
depends-on: []
---

# 飞书机器人 Backlog

本文收录**尚未实现**的改进项（P1–P3），已完成项不在此列。

## P1 — 高优先级

### 飞书 API 出站频率控制

**问题**：飞书开放平台对各 API 有 QPS 限制（IM 消息发送 50 QPS），当前 `FeishuMessageSender` / `FeishuTokenManager` 无客户端限流，并发高时可能触发飞书 429 错误。

**方案**：
- 在 `FeishuMessageSender` 和 `FeishuTokenManager` 添加基于令牌桶的本地限流
- 可选：Guava `RateLimiter` 或 Resilience4j `RateLimiter` + `CircuitBreaker`
- 配置项：`s2.feishu.rate-limit.message-qps=40`（留 20% 余量于飞书上限 50 QPS）

**工作量**：1d
**依赖**：无

---

## P2 — 中优先级

### 查询审计日志增强

**问题**：`FeishuQuerySessionDO` 部分字段未填充，影响审计追溯和 NL2SQL 质量回流。

| 字段 | 现状 | 改进 |
|------|------|------|
| `sqlText` | 存在但未填充 | 从 `QueryResult.sqlInfo.sql` 提取并保存 |
| `parseInfo` | 无 | 记录命中模板/解析模式（`RULE/LLM`） |
| `feedback` | 无 | 新增正确/错误标记字段，回流至 chat memory 改善 NL2SQL |

**工作量**：1d
**依赖**：无

---

### 敏感数据脱敏

**问题**：查询结果可能包含手机号、身份证、金额等敏感字段，飞书消息一旦发出不可撤回，尤其群聊场景风险高。

**方案**：
- 在 `FeishuCardRenderer` 渲染前检查 `QueryColumn.sensitive` 脱敏标记（SuperSonic 已有字段级脱敏能力）
- 对超过 N 行且包含敏感列的数据，自动隐藏具体值并引导走导出流程
- 群聊场景下含敏感字段的结果仅回单聊，并提示用户"含敏感数据，完整数据已发至私信"

**工作量**：1.5d
**依赖**：SuperSonicApiClient 返回的 QueryResult 需携带 `QueryColumn.sensitive` 标记

---

## P3 — 低优先级

### 群聊场景增强

**现状**：群聊仅在 @bot 时响应，缺少权限控制和群粒度配置。

| # | 改进 | 说明 |
|---|------|------|
| 1 | 查询结果可见性控制 | 群聊内查询含敏感数据时引导私聊 |
| 2 | 群粒度 Agent 绑定 | 不同群绑定不同数据域（如 #销售群 → 销售 Agent） |
| 3 | 群聊 help 提示 | `/help` 在群聊中显示"请 @我 提问" |
| 4 | 群内协作 | 支持 @同事 共享查询结果 |

**工作量**：2d
**依赖**：无

---

### 批量映射操作

**现状**：`FeishuUserMappingController` 只支持单条 CRUD，用户多时管理效率低。

**改进项**：
- 批量导入映射（CSV/Excel 上传）
- 批量启用/禁用（`POST /api/v1/feishu/userMappings:batchEnable`）
- 按部门自动同步：通过飞书通讯录 API `GET /open-apis/contact/v3/department/{id}/children` 拉取部门成员，批量创建映射

**工作量**：2d
**依赖**：无

---

### PENDING 映射自动过期

**问题**：PENDING 状态映射如果长期未处理，飞书用户每次发消息都会查询该记录，且管理员可能遗忘。

**方案**：
- PENDING 记录超过 30 天自动标记为 `EXPIRED`（定时任务清理）
- 管理后台增加 PENDING 数量告警（> 10 条时通知管理员），可复用 `feishu.mapping.pending` Gauge 指标
- EXPIRED 用户再次发消息时重新创建 PENDING 记录

**工作量**：0.5d
**依赖**：无

---

### 卡片样式外部化

**现状**：`FeishuCardTemplate` 通过 Java HashMap 手动拼装 JSON，修改固定结构卡片的样式（帮助卡片、错误卡片、引导绑定卡片）需改 Java 代码并重新编译。

**方案（分类处理）**：
- **固定结构卡片**（帮助、错误、引导绑定、单值指标）→ 存为 JSON 模板文件（`resources/feishu/templates/*.json`），通过 Mustache 填充变量，非开发人员可通过[飞书消息卡片搭建工具](https://open.feishu.cn/cardkit)设计后导入，支持热更新
- **动态表格卡片**（查询结果）→ 保持 `FeishuCardRenderer` 代码生成，将样式参数（颜色、字号、间距、截断行数）提取到 `FeishuProperties` 配置项，修改样式无需改代码

**工作量**：2d
**依赖**：无

---

## 阶段 2/3 规划（非 backlog，对应设计文档章节）

以下功能已在主文档有明确方案，但属于后续阶段，不在当前 sprint 范围：

| 功能 | 对应文档 | 阶段 |
|------|---------|------|
| 企业通讯录同步 | `02-identity-mapping.md` §企业通讯录同步 | 阶段 2（可选） |
| OAuth 自助绑定 H5 | `02-identity-mapping.md` §OAuth 自助绑定 | 阶段 2（可选兜底） |
| Dify 复杂意图路由 | 主文档 §5.1 | 阶段 3 |
| 卡片内筛选/下钻/翻页 | 主文档 §5.2 | 阶段 3 |
| 语音查询 | 主文档 §5.2 | 阶段 3 |
| 阈值告警通知 | 主文档 §4.1 | 待排期 |
