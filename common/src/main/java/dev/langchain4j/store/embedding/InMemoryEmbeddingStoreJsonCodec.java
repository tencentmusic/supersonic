package dev.langchain4j.store.embedding;

import dev.langchain4j.store.embedding.InMemoryS2EmbeddingStore.InMemoryEmbeddingStore;

public interface InMemoryEmbeddingStoreJsonCodec {

    InMemoryEmbeddingStore<EmbeddingQuery> fromJson(String json);

    String toJson(InMemoryEmbeddingStore<?> store);
}
