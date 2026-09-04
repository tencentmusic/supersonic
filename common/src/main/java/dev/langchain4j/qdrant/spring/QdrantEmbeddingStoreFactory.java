package dev.langchain4j.qdrant.spring;

import com.tencent.supersonic.common.pojo.EmbeddingStoreConfig;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.BaseEmbeddingStoreFactory;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;

public class QdrantEmbeddingStoreFactory extends BaseEmbeddingStoreFactory {

    private final EmbeddingStoreConfig config;

    public QdrantEmbeddingStoreFactory(EmbeddingStoreConfig config) {
        this.config = config;
    }

    @Override
    public EmbeddingStore<TextSegment> createEmbeddingStore(String collectionName) {
        return new QdrantEmbeddingStore(collectionName, config.getBaseUrl(), config.getPost(),
                Boolean.TRUE.equals(config.getUseTls()), config.getApiKey(), config.getDimension());
    }
}
