package com.tencent.supersonic.headless.core.cache;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DefaultQueryCache implements QueryCache {

    public Object query(SemanticQueryReq semanticQueryReq, String cacheKey) {
        CacheManager cacheManager = ContextUtils.getBean(CacheManager.class);
        if (isCache(semanticQueryReq)) {
            Object result = cacheManager.get(cacheKey);
            if (Objects.nonNull(result)) {
                log.debug("query from cache, key:{},result:{}", cacheKey,
                        StringUtils.normalizeSpace(result.toString()));
            }
            return result;
        }
        return null;
    }

    public Boolean put(String cacheKey, Object value) {
        CacheManager cacheManager = ContextUtils.getBean(CacheManager.class);
        CacheCommonConfig cacheCommonConfig = ContextUtils.getBean(CacheCommonConfig.class);
        if (cacheCommonConfig.getCacheEnable() && Objects.nonNull(value)) {
            CompletableFuture.supplyAsync(() -> cacheManager.put(cacheKey, value))
                    .exceptionally(exception -> {
                        log.warn("exception:", exception);
                        return null;
                    });
            log.debug("put to cache, key: {}", cacheKey);
            return true;
        }
        return false;
    }

    public String getCacheKey(SemanticQueryReq semanticQueryReq) {
        CacheManager cacheManager = ContextUtils.getBean(CacheManager.class);
        String commandMd5 = semanticQueryReq.generateCommandMd5();
        String keyByModelIds = getKeyByModelIds(semanticQueryReq.getModelIds());
        return cacheManager.generateCacheKey(keyByModelIds, commandMd5);
    }

    private String getKeyByModelIds(List<Long> modelIds) {
        return String.join(",",
                modelIds.stream().map(Object::toString).collect(Collectors.toList()));
    }

    private boolean isCache(SemanticQueryReq semanticQueryReq) {
        CacheCommonConfig cacheCommonConfig = ContextUtils.getBean(CacheCommonConfig.class);
        if (!cacheCommonConfig.getCacheEnable()) {
            return false;
        }
        if (semanticQueryReq.getCacheInfo() != null) {
            return semanticQueryReq.getCacheInfo().getCache();
        }
        return false;
    }
}
