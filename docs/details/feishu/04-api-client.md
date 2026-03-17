---
status: implemented
module: feishu/server
key-files:
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/service/SuperSonicApiClient.java
  - launchers/standalone/src/main/java/com/tencent/supersonic/launcher/feishu/DirectSuperSonicApiClient.java
  - feishu/api/src/main/java/com/tencent/supersonic/feishu/api/config/FeishuProperties.java
depends-on: []
---

# 04 内部 API 客户端

## 目标

提供飞书模块调用 SuperSonic 核心服务的统一接口，在 standalone 单体部署时走直联（零序列化开销），在微服务部署时走 HTTP 回环调用，同时保持权限、租户和审计链路一致。

## 当前状态

- [x] `SuperSonicApiClient` 接口定义已实现
- [x] `HttpSuperSonicApiClient`（`feishu-server`，`@ConditionalOnMissingBean` 兜底）已实现
- [x] `DirectSuperSonicApiClient`（`launchers/standalone`，via `spring.factories`，优先生效）已实现
- [x] `ResultData` 解包已实现
- [x] `Accept: application/json` header 已处理（避免 XML 响应）
- [x] chatId 缓存（`FeishuCacheService`，TTL 30min，maxSize 5000）已实现
- [x] chatId 缓存失效 + 自动重建（`evictAndRefreshChat()`）已实现
- [x] 网络重试（`exchangeWithRetry()`，仅对 `ResourceAccessException` 重试 1 次）已实现
- [x] `queryTimeoutMs` 超时设置（`SimpleClientHttpRequestFactory`）已实现
- [x] `buildInternalHeaders(tenantId)` 内部认证已实现

## 设计决策

**为什么抽取为接口 + 两种实现？**
Standalone 部署时，`DirectSuperSonicApiClient` 直接调用 Service 层，消除 HTTP 序列化/反序列化开销、`ResultData` 解包和 JWT 认证。微服务独立部署时，`HttpSuperSonicApiClient` 通过 HTTP 回环调用 REST API。切换通过 SPI 自动完成（`spring.factories`），无需配置项。

**为什么内部调用需要认证头而不是跳过认证？**
飞书查询需要以真实用户身份进入权限链路（`S2DataPermissionAspect`、行级权限、字段脱敏）。`buildInternalHeaders(tenantId)` 构建最小 User（`feishu-internal`）触发认证过滤器，再在 Handler 内部将实际的飞书用户身份写入上下文。这不是 admin 身份，也不绕过权限。

**chatId 隔离策略**：
- 每个 `(user, agentId)` 组合对应一个 SuperSonic chat session（`chatName=飞书助手`）
- 飞书会话与 Web 会话完全隔离，各自独立的多轮上下文
- chatId 缓存在 `FeishuCacheService`（TTL 30min），过期自动重建

**为什么只对 ResourceAccessException 重试？**
业务错误（权限不足、解析失败等）重试无意义。只有网络层错误（连接超时/拒绝、读超时）才值得重试 1 次（间隔 1s）。与 chatId 重试分层：网络重试是内层，chatId 重建是外层。

## 接口契约

### SuperSonicApiClient 接口

```java
public interface SuperSonicApiClient {
    // 查询（NL2SQL 解析 + 执行）
    QueryResult query(String queryText, int agentId, Integer chatId,
                      Long dataSetId, User user);

    // 仅解析，不执行
    ChatParseResp parse(String queryText, int agentId, User user);

    // 按 SQL 全量查询（用于导出）
    List<Map<String, Object>> queryBySql(String sql, Long dataSetId, User user);

    // 查找/创建飞书专属会话
    Integer getOrCreateChat(int agentId, User user);

    // 清除 chatId 缓存并重建
    void evictAndRefreshChat(int agentId, User user);

    // 获取 Agent 列表
    List<Map<String, Object>> getAgentList(User user);
}
```

### HTTP 模式关键 API 端点

| 方法 | 端点 | 说明 |
|------|------|------|
| `POST` | `/api/chat/query/` | NL2SQL 查询（`ChatParseReq`） |
| `POST` | `/api/chat/query/parse` | 仅解析，返回 `ChatParseResp` |
| `POST` | `/api/semantic/query/sql` | 按 SQL 查询语义层（limit=0 全量） |
| `GET` | `/api/chat/agent/getAgentList` | 获取 Agent 列表 |
| `GET` | `/api/chat/manage/getOrCreateChat` | 查找/创建 chat session |
| `GET` | `/api/auth/user/*` | 用户信息查询（身份映射使用） |

### 内部认证头

```java
// buildInternalHeaders(tenantId)
// 构建最小 User（feishu-internal），触发认证过滤器
// 非 admin 身份，不绕过权限
HttpHeaders headers = new HttpHeaders();
headers.set("X-S2-Tenant-Id", String.valueOf(tenantId));
headers.set("X-S2-User-Name", "feishu-internal");
headers.setAccept(List.of(MediaType.APPLICATION_JSON)); // 避免 XML 响应
```

### ResponseAdvice 解包

```java
// 所有 API 响应格式: ResultData<T>
// extractData() 从 ResultData.data 中取出实际数据
private <T> T extractData(ResponseEntity<ResultData<T>> response) {
    if (response.getBody() == null || response.getBody().getData() == null) {
        throw new RuntimeException("API returned empty data");
    }
    return response.getBody().getData();
}
```

### 网络重试

```java
// exchangeWithRetry() — 仅对 ResourceAccessException 重试 1 次
// 应用于: doQuery(), doParse(), queryBySql()
private <T> ResponseEntity<T> exchangeWithRetry(String url, ...) {
    try {
        return restTemplate.exchange(url, ...);
    } catch (ResourceAccessException e) {
        Thread.sleep(1000);
        return restTemplate.exchange(url, ...); // 第二次尝试
    }
}
```

## 数据模型

无独立表。依赖：
- `FeishuCacheService.generalCache`：chatId 缓存（key=`chat:{userId}:{agentId}`，TTL 30min）
- `s2_feishu_query_session`：查询会话记录（详见 `03-message-handlers.md`）

## 实现要点

**SPI 注册**（`DirectSuperSonicApiClient` 优先于 `HttpSuperSonicApiClient`）：

```
# launchers/standalone/src/main/resources/META-INF/spring.factories
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  com.tencent.supersonic.launcher.feishu.DirectSuperSonicApiClientAutoConfiguration
```

`DirectSuperSonicApiClientAutoConfiguration` 注册 `DirectSuperSonicApiClient` Bean，`feishu-server` 中的 `HttpSuperSonicApiClient` 标注 `@ConditionalOnMissingBean(SuperSonicApiClient.class)` 作为兜底。

**直联模式收益**（standalone 部署）：
- 消除 HTTP 序列化/反序列化开销
- 无需处理 `ResponseAdvice` 的 `ResultData` 包装
- 无需 `Accept: application/json` header
- 无需内部 JWT 认证

**超时配置**：

```java
// SuperSonicApiClient 构造函数
SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
factory.setConnectTimeout((int) properties.getQueryTimeoutMs()); // 默认 30s
factory.setReadTimeout((int) properties.getQueryTimeoutMs());
restTemplate = new RestTemplate(factory);
```

**chatId 失效处理**：
管理员在 Web 端删除 chat session 后，缓存中的 chatId 变为无效（返回 404）。`query()` 和 `parse()` 首次调用失败时，自动 `evictAndRefreshChat()`，用新 chatId 重试一次（仅当新旧 chatId 不同时重试）。

**ChatParseReq 构建**：

```java
ChatParseReq req = new ChatParseReq();
req.setQueryText(queryText);
req.setAgentId(agentId);
req.setChatId(chatId);                // 多轮上下文
req.setDataSetId(lastDataSetId);      // 追问上下文（可为 null）
req.setUser(user);                    // 实际飞书用户（权限链路使用）
```

## 待办

- [ ] 联调 HTTP 模式（微服务独立部署场景）
- [ ] 联调 Direct 模式（standalone 场景，验证权限链路）
- [ ] 验证 chatId 失效场景（管理员删除 session 后自动恢复）
