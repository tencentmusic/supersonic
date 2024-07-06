package dev.langchain4j.provider;

import com.tencent.supersonic.common.config.ChatModelConfig;
import com.tencent.supersonic.common.config.EmbeddingModelConfig;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AzureModelFactory implements ModelFactory, InitializingBean {

    @Override
    public ChatLanguageModel createChatModel(ChatModelConfig chatModel) {
        AzureOpenAiChatModel.Builder builder = AzureOpenAiChatModel.builder()
                .endpoint(chatModel.getBaseUrl())
                .apiKey(chatModel.getApiKey())
                .deploymentName(chatModel.getModelName())
                .temperature(chatModel.getTemperature())
                .timeout(Duration.ofSeconds(chatModel.getTimeOut() == null ? 0L : chatModel.getTimeOut()));
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
        ModelProvider.add(Provider.AZURE, this);
    }
}
