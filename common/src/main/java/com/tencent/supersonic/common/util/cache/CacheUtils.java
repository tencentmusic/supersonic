package com.tencent.supersonic.common.util.cache;


public interface CacheUtils {

    Boolean put(String key, Object value);

    Object get(String key);

    String generateCacheKey(String prefix, String body);

    Boolean removeCache(String key);

}
