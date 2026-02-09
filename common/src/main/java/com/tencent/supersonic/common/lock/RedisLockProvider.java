package com.tencent.supersonic.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * Redis-based distributed lock provider. Delegates to Redisson's {@link RLock} which provides
 * reentrant locking, automatic watchdog lease renewal, and proper Redis cleanup on unlock.
 */
@Slf4j
@RequiredArgsConstructor
public class RedisLockProvider implements DistributedLockProvider {

    private static final String KEY_PREFIX = "s2:lock:";

    private final RedissonClient redissonClient;

    @Override
    public DistributedLock obtain(String lockKey) {
        return new RedisLock(redissonClient.getLock(KEY_PREFIX + lockKey));
    }

    private static class RedisLock implements DistributedLock {

        private final RLock rLock;

        RedisLock(RLock rLock) {
            this.rLock = rLock;
        }

        @Override
        public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) {
            try {
                return rLock.tryLock(waitTime, leaseTime, unit);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        @Override
        public void unlock() {
            if (rLock.isHeldByCurrentThread()) {
                rLock.unlock();
            }
        }
    }
}
