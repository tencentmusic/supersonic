package com.tencent.supersonic.headless.core.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CaffeineCacheConfig {

    @Autowired
    private CacheCommonConfig cacheCommonConfig;

    @Value("${s2.caffeine.initial.capacity:500}")
    private Integer caffeineInitialCapacity;

    @Value("${s2.caffeine.max.size:5000}")
    private Integer caffeineMaximumSize;

    @Bean(name = "caffeineCache")
    public Cache<String, Object> caffeineCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(cacheCommonConfig.getCacheCommonExpireAfterWrite(),
                        TimeUnit.MINUTES)
                .initialCapacity(caffeineInitialCapacity).maximumSize(caffeineMaximumSize).build();
    }

    @Bean(name = "searchCaffeineCache")
    public Cache<Long, Object> searchCaffeineCache() {
        return Caffeine.newBuilder().expireAfterWrite(10000, TimeUnit.MINUTES)
                .initialCapacity(caffeineInitialCapacity).maximumSize(caffeineMaximumSize).build();
    }
}
