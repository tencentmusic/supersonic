package com.tencent.supersonic.chat.llm.vectordb;

import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EmbeddingStoreFactory {

    private static Map<String, EmbeddingStore> collectionNameToStore = new ConcurrentHashMap<>();


    public static EmbeddingStore create(String collectionName) {
        return collectionNameToStore.computeIfAbsent(collectionName, k -> new InMemoryEmbeddingStore());
    }


}
