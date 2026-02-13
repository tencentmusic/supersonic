package com.tencent.supersonic.feishu.server.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tencent.supersonic.feishu.api.cache.FeishuCacheService;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
public class CaffeineFeishuCacheService implements FeishuCacheService {

    private final Cache<String, Boolean> eventDedup;
    private final Cache<String, String> tokenCache;
    private final Cache<String, String> generalCache;
    private final Cache<String, Long> counterCache;

    private static final String TOKEN_KEY = "tenant_access_token";

    public CaffeineFeishuCacheService() {
        this.eventDedup = Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(10000).build();
        this.tokenCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(110))
                .maximumSize(1).build();
        this.generalCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(30))
                .maximumSize(5000).build();
        this.counterCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(60))
                .maximumSize(10000).build();
        log.info("Initialized Caffeine-based FeishuCacheService");
    }

    @Override
    public boolean isDuplicateEvent(String eventId) {
        if (eventId == null) {
            return false;
        }
        if (eventDedup.getIfPresent(eventId) != null) {
            return true;
        }
        eventDedup.put(eventId, Boolean.TRUE);
        return false;
    }

    @Override
    public String getToken() {
        return tokenCache.getIfPresent(TOKEN_KEY);
    }

    @Override
    public void putToken(String token) {
        tokenCache.put(TOKEN_KEY, token);
    }

    @Override
    public String get(String key) {
        return generalCache.getIfPresent(key);
    }

    @Override
    public void put(String key, String value) {
        generalCache.put(key, value);
    }

    @Override
    public void remove(String key) {
        generalCache.invalidate(key);
    }

    @Override
    public long incrementCounter(String key) {
        return counterCache.asMap().merge(key, 1L, Long::sum);
    }
}
