package com.tencent.supersonic.headless.server.service.delivery;

import com.google.common.util.concurrent.RateLimiter;
import com.tencent.supersonic.headless.server.pojo.DeliveryType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter for delivery channels to prevent overwhelming external services.
 *
 * Each channel type has its own rate limiter with configurable permits per second.
 */
@Component
@Slf4j
public class DeliveryRateLimiter {

    @Value("${supersonic.delivery.rate-limit.email:10}")
    private double emailPermitsPerSecond;

    @Value("${supersonic.delivery.rate-limit.webhook:20}")
    private double webhookPermitsPerSecond;

    @Value("${supersonic.delivery.rate-limit.feishu:5}")
    private double feishuPermitsPerSecond;

    @Value("${supersonic.delivery.rate-limit.dingtalk:5}")
    private double dingtalkPermitsPerSecond;

    @Value("${supersonic.delivery.rate-limit.wechat-work:5}")
    private double wechatWorkPermitsPerSecond;

    @Value("${supersonic.delivery.rate-limit.default:10}")
    private double defaultPermitsPerSecond;

    private final Map<DeliveryType, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        rateLimiters.put(DeliveryType.EMAIL, RateLimiter.create(emailPermitsPerSecond));
        rateLimiters.put(DeliveryType.WEBHOOK, RateLimiter.create(webhookPermitsPerSecond));
        rateLimiters.put(DeliveryType.FEISHU, RateLimiter.create(feishuPermitsPerSecond));
        rateLimiters.put(DeliveryType.DINGTALK, RateLimiter.create(dingtalkPermitsPerSecond));
        rateLimiters.put(DeliveryType.WECHAT_WORK, RateLimiter.create(wechatWorkPermitsPerSecond));

        log.info(
                "Delivery rate limiters initialized: EMAIL={}/s, WEBHOOK={}/s, FEISHU={}/s, DINGTALK={}/s, WECHAT_WORK={}/s",
                emailPermitsPerSecond, webhookPermitsPerSecond, feishuPermitsPerSecond,
                dingtalkPermitsPerSecond, wechatWorkPermitsPerSecond);
    }

    /**
     * Acquire a permit for the specified delivery type. This method blocks until a permit is
     * available.
     *
     * @param type the delivery type
     * @return the time spent sleeping to enforce rate limit, in seconds
     */
    public double acquire(DeliveryType type) {
        RateLimiter limiter = rateLimiters.get(type);
        if (limiter == null) {
            limiter = rateLimiters.computeIfAbsent(type,
                    t -> RateLimiter.create(defaultPermitsPerSecond));
        }
        return limiter.acquire();
    }

    /**
     * Try to acquire a permit without blocking.
     *
     * @param type the delivery type
     * @return true if permit was acquired, false otherwise
     */
    public boolean tryAcquire(DeliveryType type) {
        RateLimiter limiter = rateLimiters.get(type);
        if (limiter == null) {
            limiter = rateLimiters.computeIfAbsent(type,
                    t -> RateLimiter.create(defaultPermitsPerSecond));
        }
        return limiter.tryAcquire();
    }

    /**
     * Try to acquire a permit with timeout.
     *
     * @param type the delivery type
     * @param timeoutMs timeout in milliseconds
     * @return true if permit was acquired within timeout, false otherwise
     */
    public boolean tryAcquire(DeliveryType type, long timeoutMs) {
        RateLimiter limiter = rateLimiters.get(type);
        if (limiter == null) {
            limiter = rateLimiters.computeIfAbsent(type,
                    t -> RateLimiter.create(defaultPermitsPerSecond));
        }
        return limiter.tryAcquire(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Get current rate for a delivery type.
     *
     * @param type the delivery type
     * @return permits per second
     */
    public double getRate(DeliveryType type) {
        RateLimiter limiter = rateLimiters.get(type);
        return limiter != null ? limiter.getRate() : defaultPermitsPerSecond;
    }
}
