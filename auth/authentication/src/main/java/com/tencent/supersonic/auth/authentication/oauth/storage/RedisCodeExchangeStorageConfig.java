package com.tencent.supersonic.auth.authentication.oauth.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Configuration for Redis-based OAuth exchange code storage. This configuration is only loaded when
 * Redis is on the classpath and the storage type is explicitly set to "redis".
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
@ConditionalOnProperty(name = "s2.oauth.storage.type", havingValue = "redis",
        matchIfMissing = false)
public class RedisCodeExchangeStorageConfig {

    /**
     * Redis-based storage (primary when Redis is available and enabled).
     */
    @Bean
    @Primary
    public CodeExchangeStorage redisCodeExchangeStorage(StringRedisTemplate redisTemplate) {
        log.info("Using Redis-based CodeExchangeStorage for OAuth exchange codes");
        return new RedisCodeExchangeStorage(redisTemplate);
    }
}
