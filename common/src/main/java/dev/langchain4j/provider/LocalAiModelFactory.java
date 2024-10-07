package dev.langchain4j.provider;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.localai.LocalAiChatModel;
import dev.langchain4j.model.localai.LocalAiEmbeddingModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class LocalAiModelFactory implements ModelFactory, InitializingBean {
    public static final String PROVIDER = "LOCAL_AI";
    public static final String DEFAULT_BASE_URL = "http://localhost:8080";
    public static final String DEFAULT_MODEL_NAME = "ggml-gpt4all-j";

    @Override
    public ChatLanguageModel createChatModel(ChatModelConfig modelConfig) {
        return LocalAiChatModel.builder().baseUrl(modelConfig.getBaseUrl())
                .modelName(modelConfig.getModelName()).temperature(modelConfig.getTemperature())
                .timeout(Duration.ofSeconds(modelConfig.getTimeOut())).topP(modelConfig.getTopP())
                .logRequests(modelConfig.getLogRequests())
                .logResponses(modelConfig.getLogResponses()).maxRetries(modelConfig.getMaxRetries())
                .build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(EmbeddingModelConfig embeddingModel) {
        return LocalAiEmbeddingModel.builder().baseUrl(embeddingModel.getBaseUrl())
                .modelName(embeddingModel.getModelName()).maxRetries(embeddingModel.getMaxRetries())
                .logRequests(embeddingModel.getLogRequests())
                .logResponses(embeddingModel.getLogResponses()).build();
    }

    @Override
    public void afterPropertiesSet() {
        ModelProvider.add(PROVIDER, this);
    }
}
