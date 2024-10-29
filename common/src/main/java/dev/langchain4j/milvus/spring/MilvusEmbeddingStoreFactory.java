package dev.langchain4j.milvus.spring;

import com.tencent.supersonic.common.pojo.EmbeddingStoreConfig;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.BaseEmbeddingStoreFactory;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import org.springframework.beans.BeanUtils;

public class MilvusEmbeddingStoreFactory extends BaseEmbeddingStoreFactory {
    private final EmbeddingStoreProperties storeProperties;

    public MilvusEmbeddingStoreFactory(EmbeddingStoreConfig storeConfig) {
        this(createPropertiesFromConfig(storeConfig));
    }

    public MilvusEmbeddingStoreFactory(EmbeddingStoreProperties storeProperties) {
        this.storeProperties = storeProperties;
    }

    private static EmbeddingStoreProperties createPropertiesFromConfig(
            EmbeddingStoreConfig storeConfig) {
        EmbeddingStoreProperties embeddingStore = new EmbeddingStoreProperties();
        BeanUtils.copyProperties(storeConfig, embeddingStore);
        embeddingStore.setUri(storeConfig.getBaseUrl());
        embeddingStore.setToken(storeConfig.getApiKey());
        return embeddingStore;
    }

    @Override
    public EmbeddingStore<TextSegment> createEmbeddingStore(String collectionName) {
        return MilvusEmbeddingStore.builder().host(storeProperties.getHost())
                .port(storeProperties.getPort()).collectionName(collectionName)
                .dimension(storeProperties.getDimension()).indexType(storeProperties.getIndexType())
                .metricType(storeProperties.getMetricType()).uri(storeProperties.getUri())
                .token(storeProperties.getToken()).username(storeProperties.getUser())
                .password(storeProperties.getPassword())
                .consistencyLevel(storeProperties.getConsistencyLevel())
                .retrieveEmbeddingsOnSearch(storeProperties.getRetrieveEmbeddingsOnSearch())
                .autoFlushOnInsert(storeProperties.getAutoFlushOnInsert())
                .databaseName(storeProperties.getDatabaseName()).build();
    }
}
