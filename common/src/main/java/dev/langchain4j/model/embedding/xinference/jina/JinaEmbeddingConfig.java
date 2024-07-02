package dev.langchain4j.model.embedding.xinference.jina;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class JinaEmbeddingConfig {

    @Value("${langchain4j.jina.embedding-model.base-url}")
    public String embeddingModelBaseUrl;

    @Value("${langchain4j.jina.embedding-model.model}")
    public String embeddingModel;

    @Bean
    public JinaXinferenceEmbeddingModel jinaXinferenceEmbeddingModel() {
        return JinaXinferenceEmbeddingModel.builder()
                .baseUrl(embeddingModelBaseUrl)
                .model(embeddingModel)
                .build();
    }
}
