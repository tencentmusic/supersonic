package dev.langchain4j.chroma.spring;

import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreFactory;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;

public class ChromaEmbeddingStoreFactory implements EmbeddingStoreFactory {

    private Properties properties;

    public ChromaEmbeddingStoreFactory(Properties properties) {
        this.properties = properties;
    }

    @Override
    public EmbeddingStore create(String collectionName) {
        EmbeddingStoreProperties embeddingStore = properties.getEmbeddingStore();
        return ChromaEmbeddingStore.builder()
                .baseUrl(embeddingStore.getBaseUrl())
                .collectionName(collectionName)
                .timeout(embeddingStore.getTimeout())
                .build();
    }
}