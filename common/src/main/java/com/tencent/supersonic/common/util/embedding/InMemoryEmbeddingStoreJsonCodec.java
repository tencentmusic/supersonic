package com.tencent.supersonic.common.util.embedding;

import com.tencent.supersonic.common.util.embedding.InMemoryS2EmbeddingStore.InMemoryEmbeddingStore;

public interface InMemoryEmbeddingStoreJsonCodec {

    InMemoryEmbeddingStore<EmbeddingQuery> fromJson(String json);

    String toJson(InMemoryEmbeddingStore<?> store);
}
