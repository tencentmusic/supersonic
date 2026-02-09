package com.tencent.supersonic.common.lock;

import java.util.concurrent.TimeUnit;

/**
 * Handle to a distributed lock. Obtain instances via
 * {@link DistributedLockProvider#obtain(String)}.
 */
public interface DistributedLock {

    /**
     * Try to acquire the lock within the given wait time.
     *
     * @param waitTime maximum time to wait for the lock
     * @param leaseTime how long the lock is held before auto-expiry
     * @param unit time unit for both parameters
     * @return true if the lock was acquired
     */
    boolean tryLock(long waitTime, long leaseTime, TimeUnit unit);

    /**
     * Release the lock. Only the owner that acquired it can release it.
     */
    void unlock();
}
