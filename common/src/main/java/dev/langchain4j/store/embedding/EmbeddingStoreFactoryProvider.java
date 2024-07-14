package dev.langchain4j.store.embedding;

import com.tencent.supersonic.common.pojo.EmbeddingStoreConfig;
import com.tencent.supersonic.common.util.ContextUtils;
import dev.langchain4j.chroma.spring.ChromaEmbeddingStoreFactory;
import dev.langchain4j.inmemory.spring.InMemoryEmbeddingStoreFactory;
import dev.langchain4j.milvus.spring.MilvusEmbeddingStoreFactory;
import org.apache.commons.lang3.StringUtils;

public class EmbeddingStoreFactoryProvider {
    public static EmbeddingStoreFactory getFactory(EmbeddingStoreConfig storeConfig) {
        if (storeConfig == null || StringUtils.isBlank(storeConfig.getProvider())) {
            return ContextUtils.getBean(EmbeddingStoreFactory.class);
        }
        if (EmbeddingStoreType.CHROMA.name().equalsIgnoreCase(storeConfig.getProvider())) {
            return new ChromaEmbeddingStoreFactory(storeConfig);
        }
        if (EmbeddingStoreType.MILVUS.name().equalsIgnoreCase(storeConfig.getProvider())) {
            return new MilvusEmbeddingStoreFactory(storeConfig);
        }
        if (EmbeddingStoreType.IN_MEMORY.name().equalsIgnoreCase(storeConfig.getProvider())) {
            return new InMemoryEmbeddingStoreFactory(storeConfig);
        }
        throw new RuntimeException("Unsupported EmbeddingStore provider: " + storeConfig.getProvider());
    }
}