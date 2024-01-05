package com.tencent.supersonic.common.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class EmbeddingConfig {

    @Value("${embedding.url:}")
    private String url;

    @Value("${embedding.recognize.path:/preset_query_retrival}")
    private String recognizePath;

    @Value("${embedding.preset.collection:preset_query_collection}")
    private String presetCollection;

    @Value("${embedding.meta.collection:meta_collection}")
    private String metaCollectionName;

    @Value("${embedding.nResult:1}")
    private int nResult;

    @Value("${embedding.solved.query.collection:solved_query_collection}")
    private String solvedQueryCollection;

    @Value("${embedding.solved.query.nResult:5}")
    private int solvedQueryResultNum;

    @Value("${embedding.metric.analyzeQuery.collection:solved_query_collection}")
    private String metricAnalyzeQueryCollection;

    @Value("${embedding.metric.analyzeQuery.nResult:5}")
    private int metricAnalyzeQueryResultNum;

    @Value("${inMemoryEmbeddingStore.persistent.path:/tmp}")
    private String embeddingStorePersistentPath;

}
