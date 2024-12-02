package dev.langchain4j.pgvector.spring;

import com.tencent.supersonic.common.pojo.EmbeddingStoreConfig;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.BaseEmbeddingStoreFactory;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.BeanUtils;

public class PgvectorEmbeddingStoreFactory extends BaseEmbeddingStoreFactory {
    private final EmbeddingStoreProperties storeProperties;

    public PgvectorEmbeddingStoreFactory(EmbeddingStoreConfig storeConfig) {
        this(createPropertiesFromConfig(storeConfig));
    }

    public PgvectorEmbeddingStoreFactory(EmbeddingStoreProperties storeProperties) {
        this.storeProperties = storeProperties;
    }

    private static EmbeddingStoreProperties createPropertiesFromConfig(
            EmbeddingStoreConfig storeConfig) {
        EmbeddingStoreProperties embeddingStore = new EmbeddingStoreProperties();
        BeanUtils.copyProperties(storeConfig, embeddingStore);
        embeddingStore.setHost(storeConfig.getBaseUrl());
        embeddingStore.setPort(storeConfig.getPost());
        embeddingStore.setDatabase(storeConfig.getDatabaseName());
        embeddingStore.setUser(storeConfig.getUser());
        embeddingStore.setPassword(storeConfig.getPassword());
        return embeddingStore;
    }

    @Override
    public EmbeddingStore<TextSegment> createEmbeddingStore(String collectionName) {
        return PgVectorEmbeddingStore.builder().host(storeProperties.getHost())
                .port(storeProperties.getPort()).database(storeProperties.getDatabase())
                .user(storeProperties.getUser()).password(storeProperties.getPassword())
                .table(collectionName).dimension(storeProperties.getDimension()).build();
    }

}
