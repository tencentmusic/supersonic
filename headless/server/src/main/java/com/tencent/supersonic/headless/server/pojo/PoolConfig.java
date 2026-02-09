package com.tencent.supersonic.headless.server.pojo;

import lombok.Data;

/**
 * Per-database pool configuration overrides. If a field is null, the default from PoolType is used.
 */
@Data
public class PoolConfig {

    /**
     * Override max active connections for INTERACTIVE pool.
     */
    private Integer interactiveMaxActive;

    /**
     * Override max active connections for REPORT pool.
     */
    private Integer reportMaxActive;

    /**
     * Override max active connections for EXPORT pool.
     */
    private Integer exportMaxActive;

    /**
     * Override max active connections for SYNC pool.
     */
    private Integer syncMaxActive;

    /**
     * Override max wait time in milliseconds.
     */
    private Integer maxWaitMs;

    /**
     * Override query timeout in milliseconds.
     */
    private Integer queryTimeoutMs;

    /**
     * Get effective max active connections for the given pool type, falling back to type defaults.
     */
    public int getEffectiveMaxActive(PoolType poolType) {
        Integer override = switch (poolType) {
            case INTERACTIVE -> interactiveMaxActive;
            case REPORT -> reportMaxActive;
            case EXPORT -> exportMaxActive;
            case SYNC -> syncMaxActive;
        };
        return override != null ? override : poolType.getMaxActive();
    }

    /**
     * Get effective max wait time, falling back to type defaults.
     */
    public int getEffectiveMaxWaitMs(PoolType poolType) {
        return maxWaitMs != null ? maxWaitMs : poolType.getMaxWaitMs();
    }

    /**
     * Get effective query timeout, falling back to type defaults.
     */
    public int getEffectiveQueryTimeoutMs(PoolType poolType) {
        return queryTimeoutMs != null ? queryTimeoutMs : poolType.getQueryTimeoutMs();
    }
}
