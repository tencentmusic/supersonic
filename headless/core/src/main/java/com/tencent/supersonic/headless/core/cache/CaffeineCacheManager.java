package com.tencent.supersonic.headless.core.cache;


import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CaffeineCacheManager implements CacheManager {

    @Autowired
    private CacheCommonConfig cacheCommonConfig;

    @Autowired
    @Qualifier("caffeineCache")
    private Cache<String, Object> caffeineCache;

    @Override
    public Boolean put(String key, Object value) {
        log.debug("[put caffeineCache] key:{}, value:{}", key, value);
        caffeineCache.put(key, value);
        return true;
    }

    @Override
    public Object get(String key) {
        Object value = caffeineCache.asMap().get(key);
        log.debug("[get caffeineCache] key:{}, value:{}", key, value);
        return value;
    }

    @Override
    public String generateCacheKey(String prefix, String body) {
        if (StringUtils.isEmpty(prefix)) {
            prefix = "-1";
        }
        return Joiner.on(":").join(cacheCommonConfig.getCacheCommonApp(), cacheCommonConfig.getCacheCommonEnv(),
                cacheCommonConfig.getCacheCommonVersion(), prefix, body);
    }

    @Override
    public Boolean removeCache(String key) {
        caffeineCache.asMap().remove(key);
        return true;
    }
}
