package com.tencent.supersonic.knowledge.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.api.pojo.ModelSchema;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.knowledge.utils.ComponentFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SchemaService {


    public static final String ALL_CACHE = "all";
    private static final Integer META_CACHE_TIME = 5;
    private SemanticLayer semanticLayer = ComponentFactory.getSemanticLayer();

    private LoadingCache<String, SemanticSchema> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(META_CACHE_TIME, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<String, SemanticSchema>() {
                        @Override
                        public SemanticSchema load(String key) {
                            log.info("load getDomainSchemaInfo cache [{}]", key);
                            return new SemanticSchema(semanticLayer.getModelSchema());
                        }
                    }
            );

    public ModelSchema getModelSchema(Long id) {
        return semanticLayer.getModelSchema(id, true);
    }

    public SemanticSchema getSemanticSchema() {
        return cache.getUnchecked(ALL_CACHE);
    }

    public LoadingCache<String, SemanticSchema> getCache() {
        return cache;
    }
}
