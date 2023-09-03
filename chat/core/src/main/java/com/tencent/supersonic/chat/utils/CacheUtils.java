package com.tencent.supersonic.chat.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;

import java.util.concurrent.TimeUnit;

public class CacheUtils {
    private static final Cache<String, Object> cache = Caffeine.newBuilder()
            .expireAfterWrite(1200, TimeUnit.SECONDS)
            .expireAfterAccess(1200, TimeUnit.SECONDS)
            .maximumSize(1000)
            .build();

    public static void put(QueryContext queryContext, ChatContext chatCtx, Object v) {
        String key = chatCtx.getUser() + "_" + chatCtx.getChatId() + "_" + queryContext.getRequest().getQueryText();
        cache.put(key, v);
    }

    public static Object get(QueryContext queryContext, ChatContext chatCtx) {
        String key = chatCtx.getUser() + "_" + chatCtx.getChatId() + "_" + queryContext.getRequest().getQueryText();
        return cache.getIfPresent(key);
    }
}
