package dev.langchain4j.provider;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class OpenAiModelFactory implements ModelFactory, InitializingBean {
    public static final String PROVIDER = "OPEN_AI";

    @Override
    public ChatLanguageModel createChatModel(ChatModelConfig modelConfig) {
        return OpenAiChatModel
                .builder()
                .baseUrl(modelConfig.getBaseUrl())
                .modelName(modelConfig.getModelName())
                .apiKey(modelConfig.keyDecrypt())
                .temperature(modelConfig.getTemperature())
                .timeout(Duration.ofSeconds(modelConfig.getTimeOut()))
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
        ModelProvider.add(PROVIDER, this);
    }
}
