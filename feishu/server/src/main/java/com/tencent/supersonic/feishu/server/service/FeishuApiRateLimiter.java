package com.tencent.supersonic.feishu.server.service;

import com.tencent.supersonic.feishu.api.cache.FeishuCacheService;
import com.tencent.supersonic.feishu.api.config.FeishuProperties;
import com.tencent.supersonic.feishu.api.config.FeishuProperties.RateLimitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(name = "s2.feishu.enabled", havingValue = "true")
@Component
@RequiredArgsConstructor
@Slf4j
public class FeishuApiRateLimiter {

    private static final String API_RATE_PREFIX = "apiRate:";

    public enum ApiType {
        MESSAGE, CONTACT
    }

    private final FeishuCacheService cacheService;
    private final FeishuProperties properties;

    /**
     * Check if an API call is rate limited.
     *
     * @return true if rate limited (should reject), false if allowed
     */
    public boolean isRateLimited(ApiType apiType) {
        RateLimitConfig config = properties.getRateLimit();
        if (!config.isEnabled()) {
            return false;
        }
        int qpsLimit = getQpsLimit(apiType, config);
        if (qpsLimit <= 0) {
            return false;
        }
        long epochSecond = System.currentTimeMillis() / 1000;
        String key = API_RATE_PREFIX + apiType.name() + ":" + epochSecond;
        long count = cacheService.incrementCounter(key);
        if (count > qpsLimit) {
            log.warn("Feishu API rate limited: type={}, count={}/s, limit={}/s", apiType, count,
                    qpsLimit);
            return true;
        }
        return false;
    }

    private int getQpsLimit(ApiType apiType, RateLimitConfig config) {
        return switch (apiType) {
            case MESSAGE -> config.getMessageQps();
            case CONTACT -> config.getContactQps();
        };
    }

    public static class FeishuApiRateLimitedException extends RuntimeException {
        public FeishuApiRateLimitedException(String message) {
            super(message);
        }
    }
}
