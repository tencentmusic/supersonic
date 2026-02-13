package com.tencent.supersonic.common.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for the lock infrastructure.
 *
 * <p>
 * When {@code redisson-spring-boot-starter} is on the classpath, the starter creates the
 * {@code RedissonClient} bean (handling single/sentinel/cluster from {@code spring.data.redis.*}).
 * This class wraps it as a {@link RedisLockProvider} for distributed locking.
 *
 * <p>
 * When Redisson is not on the classpath, uses {@link LocalLockProvider} with Caffeine +
 * {@code ReentrantLock} for JVM-level mutual exclusion (single-instance only).
 *
 * <p>
 * The Redis bean is in a separate inner {@code @Configuration} class so that the
 * {@code org.redisson.api.RedissonClient} type reference is never loaded when Redisson is absent
 * from the classpath (prevents {@code NoClassDefFoundError} during CGLIB proxy generation).
 */
@Slf4j
@Configuration
public class LockAutoConfiguration {

    @Configuration
    @ConditionalOnClass(name = "org.redisson.api.RedissonClient")
    static class RedisLockConfig {
        @Bean
        public DistributedLockProvider redisLockProvider(
                org.redisson.api.RedissonClient redissonClient) {
            if (log.isDebugEnabled()) {
                log.debug("Distributed lock: using Redis");
            }
            return new RedisLockProvider(redissonClient);
        }
    }

    @Bean
    @ConditionalOnMissingBean(DistributedLockProvider.class)
    public DistributedLockProvider localLockProvider() {
        if (log.isDebugEnabled()) {
            log.debug("Distributed lock: using local JVM locks (single-instance mode)");
        }
        return new LocalLockProvider();
    }
}
