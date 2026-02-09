package com.tencent.supersonic.headless.server.service.delivery;

/**
 * Exception thrown when report delivery fails.
 */
public class DeliveryException extends RuntimeException {

    private final boolean retryable;

    public DeliveryException(String message) {
        super(message);
        this.retryable = true;
    }

    public DeliveryException(String message, Throwable cause) {
        super(message, cause);
        this.retryable = true;
    }

    public DeliveryException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public DeliveryException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
