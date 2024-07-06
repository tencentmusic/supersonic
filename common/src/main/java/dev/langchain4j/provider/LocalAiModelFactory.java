package dev.langchain4j.provider;

import com.tencent.supersonic.common.config.ChatModelConfig;
import com.tencent.supersonic.common.config.EmbeddingModelConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.localai.LocalAiChatModel;
import dev.langchain4j.model.localai.LocalAiEmbeddingModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class LocalAiModelFactory implements ModelFactory, InitializingBean {
    @Override
    public ChatLanguageModel createChatModel(ChatModelConfig chatModel) {
        return LocalAiChatModel
                .builder()
                .baseUrl(chatModel.getBaseUrl())
                .modelName(chatModel.getModelName())
                .temperature(chatModel.getTemperature())
                .timeout(Duration.ofSeconds(chatModel.getTimeOut()))
                .build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(EmbeddingModelConfig embeddingModel) {
        return LocalAiEmbeddingModel.builder()
                .baseUrl(embeddingModel.getBaseUrl())
                .modelName(embeddingModel.getModelName())
                .build();
    }

    @Override
    public void afterPropertiesSet() {
        ModelProvider.add(Provider.LOCAL_AI, this);
    }
}