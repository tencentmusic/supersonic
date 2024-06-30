package dev.langchain4j.chroma.spring;

import dev.langchain4j.inmemory.spring.InMemoryEmbeddingStoreFactory;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreFactory;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChromaEmbeddingStoreFactory implements EmbeddingStoreFactory {

    private Properties properties;

    public ChromaEmbeddingStoreFactory(Properties properties) {
        this.properties = properties;
    }

    @Override
    public EmbeddingStore create(String collectionName) {
        EmbeddingStoreProperties storeProperties = properties.getEmbeddingStore();
        EmbeddingStore embeddingStore = null;
        try {
            embeddingStore = ChromaEmbeddingStore.builder()
                    .baseUrl(storeProperties.getBaseUrl())
                    .collectionName(collectionName)
                    .timeout(storeProperties.getTimeout())
                    .build();
        } catch (Exception e) {
            log.error("Failed to create ChromaEmbeddingStore,collectionName:{}"
                            + ", fallback to the default InMemoryEmbeddingStore method。",
                    collectionName, e.getMessage());
        }
        if (Objects.isNull(embeddingStore)) {
            embeddingStore = createInMemoryEmbeddingStore(collectionName);
        }
        return embeddingStore;
    }

    private EmbeddingStore createInMemoryEmbeddingStore(String collectionName) {
        dev.langchain4j.inmemory.spring.Properties properties = new dev.langchain4j.inmemory.spring.Properties();
        dev.langchain4j.inmemory.spring.EmbeddingStoreProperties embeddingStoreProperties =
                new dev.langchain4j.inmemory.spring.EmbeddingStoreProperties();
        properties.setEmbeddingStore(embeddingStoreProperties);
        return new InMemoryEmbeddingStoreFactory(properties).create(collectionName);
    }
}