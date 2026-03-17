---
status: implemented
module: feishu/server
key-files:
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/config/FeishuAsyncConfig.java
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/cache/CaffeineFeishuCacheService.java
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/cache/RedisFeishuCacheService.java
  - feishu/api/src/main/java/com/tencent/supersonic/feishu/api/cache/FeishuCacheService.java
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/metrics/FeishuMeterBinder.java
  - feishu/server/src/main/java/com/tencent/supersonic/feishu/server/metrics/FeishuMetricsAspect.java
depends-on: []
---

# 06 基础设施（缓存、限流、异步、监控）

## 目标

为飞书机器人模块提供稳定的缓存抽象、用户级限流、专用异步线程池和 Micrometer 可观测性，支持单机和分布式两种部署形态。

## 当前状态

- [x] `FeishuCacheService` 接口 + Caffeine/Redis 双实现已实现
- [x] 事件去重缓存（TTL 5min）已实现
- [x] Token 缓存（TTL 110min）已实现
- [x] chatId 缓存（TTL 30min，maxSize 5000）已实现
- [x] 用户级频率限制（20 次/分，固定窗口计数器）已实现
- [x] 专用线程池 `feishuExecutor`（`FeishuAsyncConfig`）已实现
- [x] 优雅拒绝（队列满时回复"系统繁忙"卡片）已实现
- [x] Micrometer 指标（`FeishuMeterBinder` + `FeishuMetricsAspect`）已实现
- [x] `chatIdCache` 无界增长修复（复用 `FeishuCacheService`，有界 + TTL）已实现
- [x] `queryTimeoutMs` 超时实际生效（`SimpleClientHttpRequestFactory`）已实现
- [x] 网络重试（`exchangeWithRetry()`，`ResourceAccessException` 重试 1 次）已实现
- [ ] 飞书 API 出站频率控制（令牌桶，P1 待开发）
- [ ] Redis Stream 持久化缓冲（多实例水平扩展，可选）

## 设计决策

**为什么用 `FeishuCacheService` 接口抽象？**
统一缓存策略（事件去重、Token、chatId、计数器），支持单机用 Caffeine、分布式用 Redis，无需修改业务代码。通过 `s2.feishu.cache.type` 配置切换。

**固定窗口 vs 滑动窗口限流？**
当前实现使用**固定窗口计数器**（更简单）。边界处理：两个窗口交界处短时间可能达到 2x 上限，对于 20 次/分的场景可接受。如需精确滑动窗口可改用 Redis Sorted Set。

**为什么从 `@Async` 改为手动提交？**
`@Async` 注解在队列满时会静默丢弃任务，无法感知拒绝并给用户反馈。改为手动调用 `feishuExecutor.execute()`，捕获 `RejectedExecutionException` 后提取 `messageId` 回复"系统繁忙"错误卡片。

**Micrometer AOP 分层**：
- `FeishuMeterBinder`：注册 Gauge（PENDING 映射数量）和 Counter（限流命中数、线程池拒绝数），从 DB 或 FeishuBotService 聚合数据
- `FeishuMetricsAspect`：AOP 切面，自动记录 Handler 耗时（Timer）、消息发送成功/失败（Counter）、API 调用（Counter + status 标签）

## 接口契约

### FeishuCacheService 接口

```java
public interface FeishuCacheService {
    // 事件去重
    boolean isDuplicate(String eventId);
    void markProcessed(String eventId);         // TTL 5min

    // 通用 KV 缓存（chatId 等）
    Optional<String> get(String key);
    void put(String key, String value);          // TTL 30min
    void remove(String key);                     // 缓存失效

    // 用户限流计数器
    long incrementCounter(String key);           // 返回递增后的计数
    // Caffeine: asMap().merge(key, 1L, Long::sum)（60s TTL）
    // Redis: INCR + 条件 EXPIRE 60s

    // Token 缓存（TTL 110min）
    Optional<String> getToken(String key);
    void putToken(String key, String value);
}
```

### 频率限制（FeishuBotService）

```java
// 限流检查位置：身份映射成功后、路由分发前
long count = cacheService.incrementCounter("rate:" + openId);
if (count > properties.getRateLimit().getMaxPerMinute()) {
    messageSender.replyText(messageId, "查询过于频繁，请稍后再试（每分钟最多 20 次）");
    return;
}
```

### 专用线程池配置

```java
// FeishuAsyncConfig
@Bean("feishuExecutor")
public ThreadPoolTaskExecutor feishuExecutor(FeishuProperties properties) {
    FeishuProperties.AsyncConfig async = properties.getAsync();
    executor.setCorePoolSize(async.getCorePoolSize());       // 默认 4
    executor.setMaxPoolSize(async.getMaxPoolSize());         // 默认 8
    executor.setQueueCapacity(async.getQueueCapacity());     // 默认 100
    executor.setRejectedExecutionHandler(new AbortPolicy());
    executor.setThreadNamePrefix("feishu-bot-");
}
```

```java
// FeishuBotService 手动提交
try {
    feishuExecutor.execute(() -> processMessage(event));
} catch (RejectedExecutionException e) {
    replyBusy(event);  // 从 event 提取 messageId，发送系统繁忙卡片
}
```

### Micrometer 指标

| 指标名 | 类型 | Tags | 实现位置 |
|--------|------|------|----------|
| `feishu.query.duration` | Timer | `handler`, `status=success/error` | `FeishuMetricsAspect` @Around MessageHandler |
| `feishu.mapping.pending` | Gauge | — | `FeishuMeterBinder`（DB 查询 PENDING 数） |
| `feishu.message.sent` | Counter | `type=text/card/file` | `FeishuMetricsAspect` @AfterReturning |
| `feishu.message.send.errors` | Counter | `type=text/card/file` | `FeishuMetricsAspect` @AfterThrowing |
| `feishu.api.call` | Counter | `api=query/token/contact`, `status` | `FeishuMetricsAspect` @Around SuperSonicApiClient/TokenManager/ContactService |
| `feishu.rate_limit.hits` | Counter | — | `FeishuMeterBinder`（由 FeishuBotService 触发计数） |
| `feishu.executor.rejections` | Counter | — | `FeishuMeterBinder`（线程池拒绝数） |

暴露路径：Spring Boot Actuator `/actuator/prometheus`，可接入 Prometheus + Grafana。

## 数据模型

无独立表。缓存数据：
- Caffeine 实现：JVM 内存，单机隔离
- Redis 实现：key 前缀 `feishu:cache:`、`feishu:rate:`、`feishu:token:`

## 实现要点

**缓存配置切换**：

```yaml
s2:
  feishu:
    cache:
      type: caffeine    # caffeine（默认，单机）| redis（分布式）
    rate-limit:
      max-per-minute: 20
    async:
      core-pool-size: 4
      max-pool-size: 8
      queue-capacity: 100
```

**Caffeine 各缓存配置**：
- `eventDedup`：TTL 5min，maxSize 10000（事件去重）
- `tokenCache`：TTL 110min，maxSize 10（Token，数量极少）
- `generalCache`：TTL 30min，maxSize 5000（chatId 等通用缓存）
- `counterCache`：expireAfterAccess 2min，maxSize 10000（频率计数器）

**Redis INCR 限流原子性**：

```java
// Redis 固定窗口计数器
Long count = redisTemplate.opsForValue().increment(key);
if (count != null && count == 1) {
    redisTemplate.expire(key, 60, TimeUnit.SECONDS); // 首次请求设 TTL
}
return count != null && count > maxPerMinute;
// 注意: INCR + EXPIRE 非原子操作，极端情况下可能不设 TTL
// 对于 20 次/分的场景影响极小，可接受
```

**飞书 API 出站频率控制**（P1 待开发）：
飞书开放平台对各 API 有 QPS 限制（IM 消息发送 50 QPS），当前无客户端限流。
- 在 `FeishuMessageSender` / `FeishuTokenManager` 添加令牌桶限流
- 可选：Guava `RateLimiter` 或 Resilience4j `RateLimiter` + `CircuitBreaker`
- 配置：`s2.feishu.rate-limit.message-qps=40`（留 20% 余量）

**优雅关闭**：`feishuExecutor` 使用 `ThreadPoolTaskExecutor`，Spring 容器关闭时等待队列任务完成（`setWaitForTasksToCompleteOnShutdown(true)`，超时 `setAwaitTerminationSeconds(30)`）。

## 监控告警建议

| 监控项 | 指标 | 告警阈值 |
|--------|------|---------|
| 查询成功率 | `feishu_query_duration{status=success}` | < 95% |
| 查询平均延迟 | `feishu_query_duration` p95 | > 10s |
| 飞书 API 调用失败率 | `feishu_api_call{status=error}` | > 5% |
| Token 刷新失败 | `feishu_api_call{api=token, status=error}` | 连续 2 次 |
| PENDING 映射积压 | `feishu_mapping_pending` | > 10 |

## 待办

- [ ] 飞书 API 出站频率控制（令牌桶，P1）：`FeishuMessageSender` + `FeishuTokenManager` 添加 Guava RateLimiter
- [ ] 查询审计日志增强（P2）：`sqlText`、`parseInfo`、`feedback` 字段补全写入
- [ ] 批量映射操作（P3）：批量导入 CSV、按部门同步
- [ ] PENDING 映射自动过期（P3）：30 天定时清理 + EXPIRED 状态
- [ ] Redis Stream 持久化缓冲（可选，多实例水平扩展）
