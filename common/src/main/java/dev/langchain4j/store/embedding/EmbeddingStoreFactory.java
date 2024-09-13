package dev.langchain4j.store.embedding;

import dev.langchain4j.data.segment.TextSegment;

public interface EmbeddingStoreFactory {

    EmbeddingStore<TextSegment> create(String collectionName);
}
