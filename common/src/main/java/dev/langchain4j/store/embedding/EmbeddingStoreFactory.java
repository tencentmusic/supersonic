package dev.langchain4j.store.embedding;

public interface EmbeddingStoreFactory {

    EmbeddingStore create(String collectionName);
}