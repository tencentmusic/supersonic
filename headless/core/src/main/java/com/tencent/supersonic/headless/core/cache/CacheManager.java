package com.tencent.supersonic.headless.core.cache;


public interface CacheManager {

    Boolean put(String key, Object value);

    Object get(String key);

    String generateCacheKey(String prefix, String body);

    Boolean removeCache(String key);

}
