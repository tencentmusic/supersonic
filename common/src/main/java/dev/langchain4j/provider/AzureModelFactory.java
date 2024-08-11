package dev.langchain4j.provider;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AzureModelFactory implements ModelFactory, InitializingBean {
    public static final String PROVIDER = "AZURE";
    public static final String DEFAULT_BASE_URL = "https://xxxx.openai.azure.com/";

    @Override
    public ChatLanguageModel createChatModel(ChatModelConfig modelConfig) {
        AzureOpenAiChatModel.Builder builder = AzureOpenAiChatModel.builder()
                .endpoint(modelConfig.getEndpoint())
                .apiKey(modelConfig.getApiKey())
                .deploymentName(modelConfig.getModelName())
                .temperature(modelConfig.getTemperature())
                .maxRetries(modelConfig.getMaxRetries())
                .topP(modelConfig.getTopP())
                .timeout(Duration.ofSeconds(modelConfig.getTimeOut() == null ? 0L : modelConfig.getTimeOut()))
                .logRequestsAndResponses(modelConfig.getLogRequests() != null && modelConfig.getLogResponses());
        return builder.build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(EmbeddingModelConfig embeddingModelConfig) {
        AzureOpenAiEmbeddingModel.Builder builder = AzureOpenAiEmbeddingModel.builder()
                .endpoint(embeddingModelConfig.getBaseUrl())
                .apiKey(embeddingModelConfig.getApiKey())
                .deploymentName(embeddingModelConfig.getModelName())
                .maxRetries(embeddingModelConfig.getMaxRetries())
                .logRequestsAndResponses(embeddingModelConfig.getLogRequests() != null
                        && embeddingModelConfig.getLogResponses());
        return builder.build();
    }

    @Override
    public void afterPropertiesSet() {
        ModelProvider.add(PROVIDER, this);
    }
}
