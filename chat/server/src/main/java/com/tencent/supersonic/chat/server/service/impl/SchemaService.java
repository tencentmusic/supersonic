package com.tencent.supersonic.chat.server.service.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.DataSetSchema;
import com.tencent.supersonic.chat.core.query.semantic.SemanticInterpreter;
import com.tencent.supersonic.chat.core.utils.ComponentFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SchemaService {


    public static final String ALL_CACHE = "all";
    private static final Integer META_CACHE_TIME = 30;
    private SemanticInterpreter semanticInterpreter = ComponentFactory.getSemanticLayer();

    private LoadingCache<String, SemanticSchema> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(META_CACHE_TIME, TimeUnit.SECONDS)
            .build(
                    new CacheLoader<String, SemanticSchema>() {
                        @Override
                        public SemanticSchema load(String key) {
                            log.info("load getDomainSchemaInfo cache [{}]", key);
                            return new SemanticSchema(semanticInterpreter.getDataSetSchema());
                        }
                    }
            );

    public DataSetSchema getDataSetSchema(Long id) {
        return semanticInterpreter.getDataSetSchema(id, true);
    }

    public SemanticSchema getSemanticSchema() {
        return cache.getUnchecked(ALL_CACHE);
    }

    public LoadingCache<String, SemanticSchema> getCache() {
        return cache;
    }
}
