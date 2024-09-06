package dev.langchain4j.inmemory.spring;

import dev.langchain4j.model.embedding.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.embedding.BgeSmallZhEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.S2OnnxEmbeddingModel;
import dev.langchain4j.provider.EmbeddingModelConstant;
import dev.langchain4j.store.embedding.EmbeddingStoreFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static dev.langchain4j.inmemory.spring.Properties.PREFIX;

@Configuration
@EnableConfigurationProperties(Properties.class)
public class InMemoryAutoConfig {
    @Bean
    @ConditionalOnProperty(PREFIX + ".embedding-store.persist-path")
    EmbeddingStoreFactory inMemoryChatModel(Properties properties) {
        return new InMemoryEmbeddingStoreFactory(properties.getEmbeddingStore());
    }

    @Bean
    @ConditionalOnProperty(PREFIX + ".embedding-model.model-name")
    EmbeddingModel inMemoryEmbeddingModel(Properties properties) {
        EmbeddingModelProperties embeddingModelProperties = properties.getEmbeddingModel();
        String modelPath = embeddingModelProperties.getModelPath();
        String vocabularyPath = embeddingModelProperties.getVocabularyPath();
        if (StringUtils.isNotBlank(modelPath) && StringUtils.isNotBlank(vocabularyPath)) {
            return new S2OnnxEmbeddingModel(modelPath, vocabularyPath);
        }
        String modelName = embeddingModelProperties.getModelName();
        if (EmbeddingModelConstant.BGE_SMALL_ZH.equalsIgnoreCase(modelName)) {
            return new BgeSmallZhEmbeddingModel();
        }
        if (EmbeddingModelConstant.ALL_MINILM_L6_V2.equalsIgnoreCase(modelName)) {
            return new AllMiniLmL6V2QuantizedEmbeddingModel();
        }
        return new BgeSmallZhEmbeddingModel();
    }
}
