package com.tencent.supersonic.headless.core.cache;


import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
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
public class DefaultQueryCache implements QueryCache {

    @Value("${query.cache.enable:true}")
    private Boolean cacheEnable;
    @Autowired
    private CacheManager cacheManager;

    public Object query(SemanticQueryReq semanticQueryReq, String cacheKey) {
        if (isCache(semanticQueryReq)) {
            Object result = cacheManager.get(cacheKey);
            log.info("queryFromCache, key:{}, semanticQueryReq:{}", cacheKey, semanticQueryReq);
            return result;
        }
        return null;
    }

    public Boolean put(String cacheKey, Object value) {
        if (cacheEnable && Objects.nonNull(value)) {
            CompletableFuture.supplyAsync(() -> cacheManager.put(cacheKey, value))
                    .exceptionally(exception -> {
                        log.warn("exception:", exception);
                        return null;
                    });
            log.info("add record to cache, key:{}", cacheKey);
            return true;
        }
        return false;
    }

    public String getCacheKey(SemanticQueryReq semanticQueryReq) {
        String commandMd5 = semanticQueryReq.generateCommandMd5();
        String keyByModelIds = getKeyByModelIds(semanticQueryReq.getModelIds());
        return cacheManager.generateCacheKey(keyByModelIds, commandMd5);
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
