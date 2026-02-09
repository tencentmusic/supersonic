package com.tencent.supersonic.common.lock;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Local JVM lock provider backed by {@link ReentrantLock}. Provides real mutual exclusion within a
 * single process. Lock keys are managed by a Caffeine cache to prevent memory leaks from
 * accumulated stale entries.
 *
 * <p>
 * Used as the default when no distributed lock backend (e.g., Redis) is available. Suitable for
 * single-instance deployments only.
 */
public class LocalLockProvider implements DistributedLockProvider {

    private final Cache<String, ReentrantLock> locks = Caffeine.newBuilder().maximumSize(10_000)
            .expireAfterAccess(30, TimeUnit.MINUTES).build();

    @Override
    public DistributedLock obtain(String lockKey) {
        ReentrantLock lock = locks.get(lockKey, k -> new ReentrantLock());
        return new LocalLock(lock);
    }

    private static class LocalLock implements DistributedLock {

        private final ReentrantLock lock;

        LocalLock(ReentrantLock lock) {
            this.lock = lock;
        }

        @Override
        public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) {
            try {
                return lock.tryLock(waitTime, unit);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        @Override
        public void unlock() {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
