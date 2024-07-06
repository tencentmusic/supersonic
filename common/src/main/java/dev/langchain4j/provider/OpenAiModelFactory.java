package dev.langchain4j.provider;

import com.tencent.supersonic.common.config.ChatModelConfig;
import com.tencent.supersonic.common.config.EmbeddingModelConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class OpenAiModelFactory implements ModelFactory, InitializingBean {
    @Override
    public ChatLanguageModel createChatModel(ChatModelConfig chatModel) {
        return OpenAiChatModel
                .builder()
                .baseUrl(chatModel.getBaseUrl())
                .modelName(chatModel.getModelName())
                .apiKey(chatModel.keyDecrypt())
                .temperature(chatModel.getTemperature())
                .timeout(Duration.ofSeconds(chatModel.getTimeOut()))
                .build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(EmbeddingModelConfig embeddingModel) {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(embeddingModel.getBaseUrl())
                .apiKey(embeddingModel.getApiKey())
                .modelName(embeddingModel.getModelName())
                .maxRetries(embeddingModel.getMaxRetries())
                .logRequests(embeddingModel.getLogRequests())
                .logResponses(embeddingModel.getLogResponses())
                .build();
    }

    @Override
    public void afterPropertiesSet() {
        ModelProvider.add(Provider.OPEN_AI, this);
    }
}
