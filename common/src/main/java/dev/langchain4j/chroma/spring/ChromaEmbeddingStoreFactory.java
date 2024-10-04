package dev.langchain4j.chroma.spring;

import com.tencent.supersonic.common.pojo.EmbeddingStoreConfig;
import dev.langchain4j.store.embedding.BaseEmbeddingStoreFactory;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.time.Duration;

@Slf4j
public class ChromaEmbeddingStoreFactory extends BaseEmbeddingStoreFactory {

    private EmbeddingStoreProperties storeProperties;

    public ChromaEmbeddingStoreFactory(EmbeddingStoreConfig storeConfig) {
        this(createPropertiesFromConfig(storeConfig));
    }

    public ChromaEmbeddingStoreFactory(EmbeddingStoreProperties storeProperties) {
        this.storeProperties = storeProperties;
    }

    @Override
    public EmbeddingStore createEmbeddingStore(String collectionName) {
        return ChromaEmbeddingStore.builder().baseUrl(storeProperties.getBaseUrl())
                .collectionName(collectionName).timeout(storeProperties.getTimeout()).build();
    }

    private static EmbeddingStoreProperties createPropertiesFromConfig(
            EmbeddingStoreConfig storeConfig) {
        EmbeddingStoreProperties embeddingStore = new EmbeddingStoreProperties();
        BeanUtils.copyProperties(storeConfig, embeddingStore);
        embeddingStore.setTimeout(Duration.ofSeconds(storeConfig.getTimeOut()));
        return embeddingStore;
    }
}
