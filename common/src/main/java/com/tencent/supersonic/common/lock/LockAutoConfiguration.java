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
 */
@Slf4j
@Configuration
public class LockAutoConfiguration {

    @Bean
    @ConditionalOnClass(name = "org.redisson.api.RedissonClient")
    @ConditionalOnMissingBean(DistributedLockProvider.class)
    public DistributedLockProvider redisLockProvider(
            org.redisson.api.RedissonClient redissonClient) {
        log.info("Distributed lock: using Redis");
        return new RedisLockProvider(redissonClient);
    }

    @Bean
    @ConditionalOnMissingBean(DistributedLockProvider.class)
    public DistributedLockProvider localLockProvider() {
        log.info("Distributed lock: using local JVM locks (single-instance mode)");
        return new LocalLockProvider();
    }
}
