package dev.langchain4j.chroma.spring;


import static dev.langchain4j.chroma.spring.Properties.PREFIX;

import dev.langchain4j.store.embedding.EmbeddingStoreFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(Properties.class)
public class ChromaAutoConfig {

    @Bean
    @ConditionalOnProperty(PREFIX + ".embedding-store.base-url")
    EmbeddingStoreFactory milvusChatModel(Properties properties) {
        return new ChromaEmbeddingStoreFactory(properties);
    }
}