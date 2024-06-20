package dev.langchain4j.model.embedding;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingModelConfig {

    @Bean
    @ConditionalOnMissingBean
    public EmbeddingModel embeddingModel() {
        return new BgeSmallZhEmbeddingModel();
    }
}