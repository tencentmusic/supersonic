package com.tencent.supersonic.common.lock;

/**
 * Thrown when a distributed lock cannot be acquired within the specified wait time.
 */
public class LockAcquisitionException extends RuntimeException {

    public LockAcquisitionException(String message) {
        super(message);
    }

    public LockAcquisitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
