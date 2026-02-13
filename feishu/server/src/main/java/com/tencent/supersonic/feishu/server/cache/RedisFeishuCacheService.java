package com.tencent.supersonic.feishu.server.cache;

import com.tencent.supersonic.feishu.api.cache.FeishuCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

@Slf4j
public class RedisFeishuCacheService implements FeishuCacheService {

    private static final String EVENT_KEY_PREFIX = "feishu:event:";
    private static final String CACHE_KEY_PREFIX = "feishu:cache:";
    private static final String COUNTER_KEY_PREFIX = "feishu:counter:";
    private static final String TOKEN_KEY = "feishu:tenant_access_token";
    private static final long EVENT_TTL_MINUTES = 5;
    private static final long TOKEN_TTL_MINUTES = 110;
    private static final long CACHE_TTL_MINUTES = 30;
    private static final long COUNTER_TTL_SECONDS = 60;

    private final StringRedisTemplate redisTemplate;

    public RedisFeishuCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        log.info("Initialized Redis-based FeishuCacheService");
    }

    @Override
    public boolean isDuplicateEvent(String eventId) {
        if (eventId == null) {
            return false;
        }
        String key = EVENT_KEY_PREFIX + eventId;
        Boolean wasAbsent = redisTemplate.opsForValue().setIfAbsent(key, "1", EVENT_TTL_MINUTES,
                TimeUnit.MINUTES);
        // setIfAbsent returns true if the key was set (not a duplicate)
        return wasAbsent == null || !wasAbsent;
    }

    @Override
    public String getToken() {
        return redisTemplate.opsForValue().get(TOKEN_KEY);
    }

    @Override
    public void putToken(String token) {
        redisTemplate.opsForValue().set(TOKEN_KEY, token, TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get(CACHE_KEY_PREFIX + key);
    }

    @Override
    public void put(String key, String value) {
        redisTemplate.opsForValue().set(CACHE_KEY_PREFIX + key, value, CACHE_TTL_MINUTES,
                TimeUnit.MINUTES);
    }

    @Override
    public void remove(String key) {
        redisTemplate.delete(CACHE_KEY_PREFIX + key);
    }

    @Override
    public long incrementCounter(String key) {
        String fullKey = COUNTER_KEY_PREFIX + key;
        Long count = redisTemplate.opsForValue().increment(fullKey);
        if (count != null && count == 1) {
            redisTemplate.expire(fullKey, COUNTER_TTL_SECONDS, TimeUnit.SECONDS);
        }
        return count != null ? count : 0;
    }
}
