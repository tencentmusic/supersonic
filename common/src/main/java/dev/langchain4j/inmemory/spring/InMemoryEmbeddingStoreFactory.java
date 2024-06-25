package dev.langchain4j.inmemory.spring;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreFactory;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class InMemoryEmbeddingStoreFactory implements EmbeddingStoreFactory {

    private static Map<String, InMemoryEmbeddingStore<TextSegment>> collectionNameToStore =
            new ConcurrentHashMap<>();
    private Properties properties;


    public InMemoryEmbeddingStoreFactory(Properties properties) {
        this.properties = properties;
    }

    @Override
    public synchronized EmbeddingStore create(String collectionName) {
        InMemoryEmbeddingStore<TextSegment> embeddingStore = collectionNameToStore.get(collectionName);
        if (Objects.nonNull(embeddingStore)) {
            return embeddingStore;
        }
        embeddingStore = new InMemoryEmbeddingStore();
        collectionNameToStore.putIfAbsent(collectionName, embeddingStore);
        return embeddingStore;

    }
}