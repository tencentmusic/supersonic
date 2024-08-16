package dev.langchain4j.provider;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.model.zhipu.ZhipuAiEmbeddingModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

@Service
public class ZhipuModelFactory implements ModelFactory, InitializingBean {
    public static final String PROVIDER = "ZHIPU";
    public static final String DEFAULT_BASE_URL = "https://open.bigmodel.cn/api/paas/v4";
    public static final String DEFAULT_EMBEDDING_BASE_URL = "https://open.bigmodel.cn/";

    @Override
    public ChatLanguageModel createChatModel(ChatModelConfig modelConfig) {
        return ZhipuAiChatModel.builder()
                .baseUrl(modelConfig.getBaseUrl())
                .apiKey(modelConfig.getApiKey())
                .model(modelConfig.getModelName())
                .temperature(modelConfig.getTemperature())
                .topP(modelConfig.getTopP())
                .maxRetries(modelConfig.getMaxRetries())
                .logRequests(modelConfig.getLogRequests())
                .logResponses(modelConfig.getLogResponses())
                .build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(EmbeddingModelConfig embeddingModelConfig) {
        return ZhipuAiEmbeddingModel.builder()
                .baseUrl(embeddingModelConfig.getBaseUrl())
                .apiKey(embeddingModelConfig.getApiKey())
                .model(embeddingModelConfig.getModelName())
                .maxRetries(embeddingModelConfig.getMaxRetries())
                .logRequests(embeddingModelConfig.getLogRequests())
                .logResponses(embeddingModelConfig.getLogResponses())
                .build();
    }

    @Override
    public void afterPropertiesSet() {
        ModelProvider.add(PROVIDER, this);
    }
}
