package dev.langchain4j.provider;

import com.tencent.supersonic.common.pojo.EmbeddingStoreConfig;
import dev.langchain4j.store.embedding.EmbeddingStore;

public interface EmbeddingStoreFactory {
    EmbeddingStore createEmbeddingStore(EmbeddingStoreConfig config);
}
