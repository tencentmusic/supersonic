package com.tencent.supersonic.headless.server.pojo;

/**
 * Connection pool type for different use cases. Each type has default settings optimized for its
 * typical workload.
 */
public enum PoolType {

    /**
     * Interactive queries from UI/API. Low latency, moderate concurrency.
     */
    INTERACTIVE(10, 30_000, 30_000),

    /**
     * Report generation. Medium concurrency, longer timeouts.
     */
    REPORT(3, 300_000, 600_000),

    /**
     * Data export tasks. Low concurrency, long timeouts.
     */
    EXPORT(2, 600_000, 1800_000),

    /**
     * Data synchronization jobs. Low concurrency, very long timeouts.
     */
    SYNC(2, 600_000, 3600_000);

    private final int maxActive;
    private final int maxWaitMs;
    private final int queryTimeoutMs;

    PoolType(int maxActive, int maxWaitMs, int queryTimeoutMs) {
        this.maxActive = maxActive;
        this.maxWaitMs = maxWaitMs;
        this.queryTimeoutMs = queryTimeoutMs;
    }

    public int getMaxActive() {
        return maxActive;
    }

    public int getMaxWaitMs() {
        return maxWaitMs;
    }

    public int getQueryTimeoutMs() {
        return queryTimeoutMs;
    }
}
