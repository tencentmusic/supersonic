package dev.langchain4j.milvus.spring;

import com.tencent.supersonic.common.config.EmbeddingStoreConfig;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.BaseEmbeddingStoreFactory;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import org.springframework.beans.BeanUtils;

public class MilvusEmbeddingStoreFactory extends BaseEmbeddingStoreFactory {
    private final Properties properties;

    public MilvusEmbeddingStoreFactory(EmbeddingStoreConfig storeConfig) {
        this(createPropertiesFromConfig(storeConfig));
    }

    public MilvusEmbeddingStoreFactory(Properties properties) {
        this.properties = properties;
    }

    private static Properties createPropertiesFromConfig(EmbeddingStoreConfig storeConfig) {
        Properties properties = new Properties();
        EmbeddingStoreProperties embeddingStore = new EmbeddingStoreProperties();
        BeanUtils.copyProperties(storeConfig, embeddingStore);
        embeddingStore.setUri(storeConfig.getBaseUrl());
        properties.setEmbeddingStore(embeddingStore);
        return properties;
    }

    @Override
    public EmbeddingStore<TextSegment> createEmbeddingStore(String collectionName) {
        EmbeddingStoreProperties storeProperties = properties.getEmbeddingStore();
        return MilvusEmbeddingStore.builder()
                .host(storeProperties.getHost())
                .port(storeProperties.getPort())
                .collectionName(collectionName)
                .dimension(storeProperties.getDimension())
                .indexType(storeProperties.getIndexType())
                .metricType(storeProperties.getMetricType())
                .uri(storeProperties.getUri())
                .token(storeProperties.getToken())
                .username(storeProperties.getUsername())
                .password(storeProperties.getPassword())
                .consistencyLevel(storeProperties.getConsistencyLevel())
                .retrieveEmbeddingsOnSearch(storeProperties.getRetrieveEmbeddingsOnSearch())
                .autoFlushOnInsert(storeProperties.getAutoFlushOnInsert())
                .databaseName(storeProperties.getDatabaseName())
                .build();
    }
}