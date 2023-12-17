package com.tencent.supersonic.common.util.embedding;

import com.tencent.supersonic.common.util.embedding.InMemoryS2EmbeddingStore.InMemoryEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;

public interface InMemoryEmbeddingStoreJsonCodec {
    InMemoryEmbeddingStore<TextSegment> fromJson(String json);

    String toJson(InMemoryEmbeddingStore<?> store);
}
