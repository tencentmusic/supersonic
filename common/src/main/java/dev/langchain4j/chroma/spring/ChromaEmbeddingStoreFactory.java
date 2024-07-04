package dev.langchain4j.chroma.spring;

import dev.langchain4j.store.embedding.BaseEmbeddingStoreFactory;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChromaEmbeddingStoreFactory extends BaseEmbeddingStoreFactory {

    private Properties properties;

    public ChromaEmbeddingStoreFactory(Properties properties) {
        this.properties = properties;
    }

    @Override
    public EmbeddingStore createEmbeddingStore(String collectionName) {
        EmbeddingStoreProperties storeProperties = properties.getEmbeddingStore();
        return ChromaEmbeddingStore.builder()
                .baseUrl(storeProperties.getBaseUrl())
                .collectionName(collectionName)
                .timeout(storeProperties.getTimeout())
                .build();
    }
}