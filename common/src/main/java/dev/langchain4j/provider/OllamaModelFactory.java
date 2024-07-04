package dev.langchain4j.provider;

import com.tencent.supersonic.common.config.ChatModelConfig;
import com.tencent.supersonic.common.config.EmbeddingModelConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class OllamaModelFactory implements ModelFactory, InitializingBean {
    @Override
    public ChatLanguageModel createChatModel(ChatModelConfig chatModel) {
        return OllamaChatModel
                .builder()
                .baseUrl(chatModel.getBaseUrl())
                .modelName(chatModel.getModelName())
                .temperature(chatModel.getTemperature())
                .timeout(Duration.ofSeconds(chatModel.getTimeOut()))
                .build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(EmbeddingModelConfig embeddingModelConfig) {
        return OllamaEmbeddingModel.builder()
                .baseUrl(embeddingModelConfig.getBaseUrl())
                .modelName(embeddingModelConfig.getModelName())
                .maxRetries(embeddingModelConfig.getMaxRetries())
                .logRequests(embeddingModelConfig.getLogRequests())
                .logResponses(embeddingModelConfig.getLogResponses())
                .build();
    }

    @Override
    public void afterPropertiesSet() {
        ModelProvider.add(Provider.OLLAMA, this);
    }
}
