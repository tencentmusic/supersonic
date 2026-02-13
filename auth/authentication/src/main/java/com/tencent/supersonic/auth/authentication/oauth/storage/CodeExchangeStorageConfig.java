package com.tencent.supersonic.auth.authentication.oauth.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Auto-configuration for OAuth exchange code storage.
 *
 * <p>
 * Redis inner class is evaluated first — when {@code s2.oauth.storage.type=redis} and
 * {@code StringRedisTemplate} is on the classpath, the Redis bean is created. Otherwise Caffeine
 * acts as the default fallback via {@code @ConditionalOnMissingBean}.
 */
@Slf4j
@Configuration
public class CodeExchangeStorageConfig {

    @Configuration
    @ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
    @ConditionalOnProperty(name = "s2.oauth.storage.type", havingValue = "redis")
    static class RedisCacheConfig {
        @Bean
        public CodeExchangeStorage redisCodeExchangeStorage(StringRedisTemplate redisTemplate) {
            log.info("Using Redis-based CodeExchangeStorage for OAuth exchange codes");
            return new RedisCodeExchangeStorage(redisTemplate);
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "s2.oauth.storage.type", havingValue = "caffeine",
            matchIfMissing = true)
    static class CaffeineCacheConfig {
        @Bean
        public CodeExchangeStorage caffeineCodeExchangeStorage() {
            log.info("Using Caffeine-based CodeExchangeStorage for OAuth exchange codes");
            return new CaffeineCodeExchangeStorage();
        }
    }
}
