package com.tencent.supersonic.common.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class EmbeddingConfig {

    @Value("${s2.embedding.memory.collection.prefix:memory_}")
    private String memoryCollectionPrefix;

    @Value("${s2.embedding.preset.collection:preset_query_collection}")
    private String presetCollection;

    @Value("${s2.embedding.meta.collection:meta_collection}")
    private String metaCollectionName;

    @Value("${s2.embedding.nResult:1}")
    private int nResult;

    @Value("${s2.embedding.metric.analyzeQuery.collection:solved_query_collection}")
    private String metricAnalyzeQueryCollection;

    @Value("${text2sql.collection.name:text2dsl_agent_collection}")
    private String text2sqlCollectionName;

    @Value("${s2.embedding.metric.analyzeQuery.nResult:5}")
    private int metricAnalyzeQueryResultNum;

    public String getMemoryCollectionName(Integer agentId) {
        return memoryCollectionPrefix + agentId;
    }

}
