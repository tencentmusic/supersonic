package com.tencent.supersonic.common.lock;

/**
 * Factory for obtaining {@link DistributedLock} instances keyed by a logical name.
 */
public interface DistributedLockProvider {

    /**
     * Obtain a lock handle for the given key. The returned lock is not yet acquired — call
     * {@link DistributedLock#tryLock} to attempt acquisition.
     *
     * @param lockKey logical name identifying the lock
     * @return a lock handle
     */
    DistributedLock obtain(String lockKey);
}
