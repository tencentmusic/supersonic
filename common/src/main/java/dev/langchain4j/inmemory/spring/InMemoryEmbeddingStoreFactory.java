package dev.langchain4j.inmemory.spring;

import dev.langchain4j.store.embedding.EmbeddingQuery;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreFactory;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InMemoryEmbeddingStoreFactory implements EmbeddingStoreFactory {

    private static Map<String, InMemoryEmbeddingStore<EmbeddingQuery>> collectionNameToStore =
            new ConcurrentHashMap<>();
    private Properties properties;


    public InMemoryEmbeddingStoreFactory(Properties properties) {
        this.properties = properties;
    }

    @Override
    public synchronized EmbeddingStore create(String collectionName) {
        InMemoryEmbeddingStore<EmbeddingQuery> embeddingStore = collectionNameToStore.get(collectionName);
        if (Objects.nonNull(embeddingStore)) {
            return embeddingStore;
        }
        if (Objects.isNull(embeddingStore)) {
            embeddingStore = new InMemoryEmbeddingStore();
            collectionNameToStore.putIfAbsent(collectionName, embeddingStore);
        }
        return embeddingStore;

    }
}