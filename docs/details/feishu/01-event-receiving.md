---
status: implemented
module: feishu/server
key-files:
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/rest/FeishuEventController.java
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/cache/CaffeineFeishuCacheService.java
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/cache/RedisFeishuCacheService.java
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/service/FeishuBotService.java
depends-on: []
---

# 01 事件接收与验签

## 目标

安全、幂等地接收飞书开放平台推送的事件，并将合法事件分发给 `FeishuBotService` 异步处理。

## 当前状态

- [x] Webhook 模式（`FeishuEventController`）已实现
- [x] WebSocket 长连接模式（`FeishuWsClient`，基于 `oapi-sdk:2.5.3`）已实现
- [x] URL 验证挑战（challenge）已实现
- [x] `verification-token` 基础验证已实现
- [x] HMAC-SHA256 签名验证（encrypt-key 配置后强制校验）已实现
- [x] 事件去重（Caffeine / Redis 双实现，TTL 5min）已实现
- [ ] 联调验证（依赖飞书应用创建与配置）

## 设计决策

**为什么支持两种接收模式？**
- Webhook 模式需要公网可达的回调 URL，适合生产环境。
- WebSocket 模式（`connection-mode: ws`）由 `FeishuWsClient` 主动连接飞书服务器，无需暴露公网端点，适合开发调试和内网部署。SDK 内部处理重连、心跳、加解密、签名校验。

**为什么 HMAC-SHA256 签名是可选的？**
- `verification-token` 已提供基础来源验证，`encrypt-key` 为空时跳过签名验证（向后兼容）。
- 生产环境建议同时配置两项（双重验证）。

**为什么入站签名算法与出站 Webhook 签名不同？**
- 入站事件验签算法：`SHA256(timestamp + nonce + encryptKey + body)`（纯 SHA256 摘要）。
- 出站 Webhook 签名（`FeishuDeliveryChannel.generateSign()`）：`HMAC-SHA256(timestamp + "\n" + secret, secret)`。
- 两者都由飞书协议定义，不可互换。

**为什么使用 timing-safe 比较？**
- 防止基于时序的侧信道攻击，签名比较必须是恒定时间比较。

## 接口契约

### Webhook 入口

```
POST /api/feishu/webhook
@AuthenticationIgnore
参数: rawBody (String), headers (Map<String, String>)
返回: {"challenge": "..."} (URL验证) | {} (正常处理)
```

### 签名算法

```
签名输入 = timestamp + nonce + encrypt_key + request_body
签名结果 = SHA256(签名输入)

timestamp = HTTP Header "X-Lark-Request-Timestamp"
nonce     = HTTP Header "X-Lark-Request-Nonce"
signature = HTTP Header "X-Lark-Signature"
```

### 事件去重接口

```java
// FeishuCacheService
boolean isDuplicate(String eventId);        // 检查 event_id 是否已处理
void markProcessed(String eventId);         // 标记已处理（TTL 5min）
```

### FeishuCacheService 扩展方法

```java
boolean isRateLimited(String openId, int maxPerMinute);
// Caffeine 实现: counterCache.asMap().merge(key, 1L, Long::sum)（60s TTL）
// Redis 实现: INCR + 条件 EXPIRE 60s
```

## 数据模型

无独立表，依赖 `FeishuCacheService` 缓存 event_id（TTL 5 分钟）。

事件结构（飞书推送的核心字段）：

| 字段 | 说明 |
|------|------|
| `header.event_id` | 全局唯一，用于去重 |
| `header.event_type` | `im.message.receive_v1` / `card.action.trigger` |
| `event.message.message_id` | 消息 ID，用于回复 |
| `event.sender.sender_id.open_id` | 发送者飞书 open_id |
| `event.message.chat_type` | `p2p`（单聊）/ `group`（群聊） |
| `event.message.content` | 消息内容（JSON 字符串） |

## 实现要点

**Controller 方法签名**：接收 `@RequestBody String rawBody`（而非 `Map`），以便先做签名验证再反序列化。

```java
@PostMapping
@AuthenticationIgnore
public Object handleEvent(@RequestBody String rawBody,
                           @RequestHeader Map<String, String> headers) {
    // 1. URL 验证挑战
    // 2. HMAC-SHA256 签名验证（encryptKey 非空时）
    // 3. verification-token 验证
    // 4. event_id 去重
    // 5. 异步分发 → FeishuBotService.handleEventAsync()
}
```

**WebSocket 模式**：`FeishuWsClient` 基于 `oapi-sdk:2.5.3`，SDK 内部处理重连、心跳、签名校验，开发者只注册事件处理回调即可。

**配置项**：

```yaml
s2:
  feishu:
    connection-mode: ws           # webhook | ws
    verification-token: ${FEISHU_VERIFICATION_TOKEN:}
    encrypt-key: ${FEISHU_ENCRYPT_KEY:}   # 空时跳过签名验证
```

**飞书开放平台事件订阅配置**：

| 事件 | 事件标识 | 说明 |
|------|---------|------|
| 接收消息 | `im.message.receive_v1` | 用户发送消息时触发 |
| 消息卡片回调 | `card.action.trigger` | 卡片按钮交互 |

**飞书应用所需权限**：

| 权限标识 | 用途 |
|---------|------|
| `im:message` | 接收用户消息、发送回复 |
| `im:message.group_at_msg` | 群聊中 @机器人 消息 |
| `im:resource` | 导出文件发送 |
| `im:message:send_as_bot` | 以机器人身份发消息 |
| `im:message.group_msg` | 接收群聊中所有消息事件 |

## 待办

- [ ] 创建飞书企业自建应用，完成权限申请与事件订阅配置
- [ ] 联调 Webhook 模式端到端（Webhook 验证 + 真实事件推送）
- [ ] 联调 WebSocket 模式内网部署场景
