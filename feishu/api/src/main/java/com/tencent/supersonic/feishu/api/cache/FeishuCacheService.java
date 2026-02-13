package com.tencent.supersonic.feishu.api.cache;

/**
 * Abstraction for Feishu caching: event deduplication, token caching, and general-purpose KV cache.
 * Implementations: Caffeine (single-node) or Redis (distributed).
 */
public interface FeishuCacheService {

    /**
     * Check if an event has already been processed. If not seen before, marks it as processed and
     * returns false. If already seen, returns true (duplicate).
     */
    boolean isDuplicateEvent(String eventId);

    /**
     * Get cached tenant_access_token.
     *
     * @return the token, or null if not cached
     */
    String getToken();

    /**
     * Cache tenant_access_token.
     */
    void putToken(String token);

    /**
     * Get a value from the general-purpose cache.
     *
     * @param key cache key
     * @return the cached value, or null if not present
     */
    String get(String key);

    /**
     * Put a value into the general-purpose cache with default TTL.
     *
     * @param key cache key
     * @param value cache value
     */
    void put(String key, String value);

    /**
     * Remove a value from the general-purpose cache.
     *
     * @param key cache key
     */
    void remove(String key);

    /**
     * Atomically increment a counter and return the new value. The counter auto-expires after ~60
     * seconds (fixed window). Used for rate limiting.
     *
     * @param key counter key
     * @return the count after incrementing
     */
    long incrementCounter(String key);
}
