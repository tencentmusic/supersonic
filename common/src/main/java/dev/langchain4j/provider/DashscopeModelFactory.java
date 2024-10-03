package dev.langchain4j.provider;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.dashscope.QwenModelName;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

@Service
public class DashscopeModelFactory implements ModelFactory, InitializingBean {
    public static final String PROVIDER = "DASHSCOPE";
    public static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/api/v1";
    public static final String DEFAULT_MODEL_NAME = QwenModelName.QWEN_PLUS;
    public static final String DEFAULT_EMBEDDING_MODEL_NAME = "text-embedding-v2";

    @Override
    public ChatLanguageModel createChatModel(ChatModelConfig modelConfig) {
        return QwenChatModel.builder().baseUrl(modelConfig.getBaseUrl())
                .apiKey(modelConfig.getApiKey()).modelName(modelConfig.getModelName())
                .temperature(modelConfig.getTemperature() == null ? 0L
                        : modelConfig.getTemperature().floatValue())
                .topP(modelConfig.getTopP()).enableSearch(modelConfig.getEnableSearch()).build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(EmbeddingModelConfig embeddingModelConfig) {
        return QwenEmbeddingModel.builder().apiKey(embeddingModelConfig.getApiKey())
                .modelName(embeddingModelConfig.getModelName()).build();
    }

    @Override
    public void afterPropertiesSet() {
        ModelProvider.add(PROVIDER, this);
    }
}
