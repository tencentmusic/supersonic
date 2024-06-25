package dev.langchain4j.milvus.spring;

import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreFactory;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;

public class MilvusEmbeddingStoreFactory implements EmbeddingStoreFactory {
    private Properties properties;

    public MilvusEmbeddingStoreFactory(Properties properties) {
        this.properties = properties;
    }

    @Override
    public EmbeddingStore create(String collectionName) {
        EmbeddingStoreProperties embeddingStore = properties.getEmbeddingStore();
        return MilvusEmbeddingStore.builder()
                .host(embeddingStore.getHost())
                .port(embeddingStore.getPort())
                .collectionName(collectionName)
                .dimension(embeddingStore.getDimension())
                .indexType(embeddingStore.getIndexType())
                .metricType(embeddingStore.getMetricType())
                .uri(embeddingStore.getUri())
                .token(embeddingStore.getToken())
                .username(embeddingStore.getUsername())
                .password(embeddingStore.getPassword())
                .consistencyLevel(embeddingStore.getConsistencyLevel())
                .retrieveEmbeddingsOnSearch(embeddingStore.getRetrieveEmbeddingsOnSearch())
                .databaseName(embeddingStore.getDatabaseName())
                .build();
    }
}