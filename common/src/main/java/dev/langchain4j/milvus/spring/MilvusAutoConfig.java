package dev.langchain4j.milvus.spring;

import dev.langchain4j.store.embedding.EmbeddingStoreFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static dev.langchain4j.milvus.spring.Properties.PREFIX;

@Configuration
@EnableConfigurationProperties(Properties.class)
public class MilvusAutoConfig {

    @Bean
    @ConditionalOnProperty(PREFIX + ".embedding-store.uri")
    EmbeddingStoreFactory milvusChatModel(Properties properties) {
        return new MilvusEmbeddingStoreFactory(properties.getEmbeddingStore());
    }
}
