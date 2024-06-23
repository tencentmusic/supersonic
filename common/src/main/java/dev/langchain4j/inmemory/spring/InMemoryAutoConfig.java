package dev.langchain4j.inmemory.spring;


import static dev.langchain4j.inmemory.spring.Properties.PREFIX;

import dev.langchain4j.store.embedding.EmbeddingStoreFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(Properties.class)
public class InMemoryAutoConfig {

    @Bean
    @ConditionalOnProperty(PREFIX + ".embedding-store.file-path")
    EmbeddingStoreFactory milvusChatModel(Properties properties) {
        return new InMemoryEmbeddingStoreFactory(properties);
    }
}