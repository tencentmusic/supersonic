package dev.langchain4j.opensearch.spring;

import dev.langchain4j.store.embedding.EmbeddingStoreFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static dev.langchain4j.opensearch.spring.Properties.PREFIX;

@Configuration
@EnableConfigurationProperties(dev.langchain4j.opensearch.spring.Properties.class)
public class OpenSearchAutoConfig {

    @Bean
    @ConditionalOnProperty(PREFIX + ".embedding-store.uri")
    EmbeddingStoreFactory milvusChatModel(Properties properties) {
        return new OpenSearchEmbeddingStoreFactory(properties.getEmbeddingStore());
    }
}
