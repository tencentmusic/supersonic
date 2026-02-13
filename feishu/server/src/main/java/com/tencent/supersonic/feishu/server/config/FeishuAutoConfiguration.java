package com.tencent.supersonic.feishu.server.config;

import com.tencent.supersonic.feishu.api.cache.FeishuCacheService;
import com.tencent.supersonic.feishu.api.config.FeishuProperties;
import com.tencent.supersonic.feishu.server.cache.CaffeineFeishuCacheService;
import com.tencent.supersonic.feishu.server.cache.RedisFeishuCacheService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@ConditionalOnProperty(prefix = "s2.feishu", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(FeishuProperties.class)
@ComponentScan(basePackages = "com.tencent.supersonic.feishu.server")
public class FeishuAutoConfiguration {

    @Configuration
    @ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
    @ConditionalOnProperty(name = "s2.feishu.cache.type", havingValue = "redis")
    static class RedisCacheConfig {
        @Bean
        public FeishuCacheService feishuCacheService(StringRedisTemplate redisTemplate) {
            return new RedisFeishuCacheService(redisTemplate);
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "s2.feishu.cache.type", havingValue = "caffeine",
            matchIfMissing = true)
    static class CaffeineCacheConfig {
        @Bean
        public FeishuCacheService feishuCacheService() {
            return new CaffeineFeishuCacheService();
        }
    }
}
