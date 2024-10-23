package dev.langchain4j.pgvector.spring;

import dev.langchain4j.store.embedding.EmbeddingStoreFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static dev.langchain4j.pgvector.spring.Properties.PREFIX;

@Configuration
@EnableConfigurationProperties(Properties.class)
public class PgvectorAutoConfig {

    @Bean
    @ConditionalOnProperty(PREFIX + ".embedding-store.host")
    EmbeddingStoreFactory pgvectorChatModel(Properties properties) {
        return new PgvectorEmbeddingStoreFactory(properties.getEmbeddingStore());
    }
}
