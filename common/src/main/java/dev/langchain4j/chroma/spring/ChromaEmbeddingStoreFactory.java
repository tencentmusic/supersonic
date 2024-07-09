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

    private Properties properties;

    public ChromaEmbeddingStoreFactory(EmbeddingStoreConfig storeConfig) {
        this(createPropertiesFromConfig(storeConfig));
    }

    public ChromaEmbeddingStoreFactory(Properties properties) {
        this.properties = properties;
    }

    @Override
    public EmbeddingStore createEmbeddingStore(String collectionName) {
        EmbeddingStoreProperties storeProperties = properties.getEmbeddingStore();
        return ChromaEmbeddingStore.builder()
                .baseUrl(storeProperties.getBaseUrl())
                .collectionName(collectionName)
                .timeout(storeProperties.getTimeout())
                .build();
    }

    private static Properties createPropertiesFromConfig(EmbeddingStoreConfig storeConfig) {
        Properties properties = new Properties();
        EmbeddingStoreProperties embeddingStore = new EmbeddingStoreProperties();
        BeanUtils.copyProperties(storeConfig, embeddingStore);
        embeddingStore.setTimeout(Duration.ofSeconds(storeConfig.getTimeOut()));
        properties.setEmbeddingStore(embeddingStore);
        return properties;
    }
}