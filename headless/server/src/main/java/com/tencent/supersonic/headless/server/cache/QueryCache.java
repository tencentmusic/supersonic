package com.tencent.supersonic.headless.server.cache;


import com.tencent.supersonic.headless.api.pojo.Cache;
import com.tencent.supersonic.headless.api.request.SemanticQueryReq;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class QueryCache {

    @Value("${query.cache.enable:true}")
    private Boolean cacheEnable;
    @Autowired
    private CacheManager cacheManager;

    public Object query(SemanticQueryReq semanticQueryReq) {
        String cacheKey = getCacheKey(semanticQueryReq);
        handleGlobalCacheDisable(semanticQueryReq);
        boolean isCache = isCache(semanticQueryReq);
        if (isCache) {
            Object result = cacheManager.get(cacheKey);
            log.info("queryFromCache, key:{}, semanticQueryReq:{}", cacheKey, semanticQueryReq);
            return result;
        }
        return null;
    }

    public Boolean put(SemanticQueryReq semanticQueryReq, Object value) {
        if (cacheEnable && Objects.nonNull(value)) {
            String key = getCacheKey(semanticQueryReq);
            CompletableFuture.supplyAsync(() -> cacheManager.put(key, value))
                    .exceptionally(exception -> {
                        log.warn("exception:", exception);
                        return null;
                    });
            log.info("add record to cache, key:{}", key);
            return true;
        }
        return false;
    }

    public String getCacheKey(SemanticQueryReq semanticQueryReq) {
        String commandMd5 = semanticQueryReq.generateCommandMd5();
        String keyByModelIds = getKeyByModelIds(semanticQueryReq.getModelIds());
        return cacheManager.generateCacheKey(keyByModelIds, commandMd5);
    }

    private void handleGlobalCacheDisable(SemanticQueryReq semanticQueryReq) {
        if (!cacheEnable) {
            Cache cacheInfo = new Cache();
            cacheInfo.setCache(false);
            semanticQueryReq.setCacheInfo(cacheInfo);
        }
    }

    private String getKeyByModelIds(List<Long> modelIds) {
        return String.join(",", modelIds.stream().map(Object::toString).collect(Collectors.toList()));
    }

    private boolean isCache(SemanticQueryReq semanticQueryReq) {
        if (!cacheEnable) {
            return false;
        }
        if (semanticQueryReq.getCacheInfo() != null) {
            return semanticQueryReq.getCacheInfo().getCache();
        }
        return false;
    }

}
