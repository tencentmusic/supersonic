package dev.langchain4j.provider;

import com.tencent.supersonic.common.config.ChatModelConfig;
import com.tencent.supersonic.common.config.EmbeddingModelConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

@Service
public class DashscopeModelFactory implements ModelFactory, InitializingBean {
    @Override
    public ChatLanguageModel createChatModel(ChatModelConfig chatModel) {
        return QwenChatModel.builder()
                .baseUrl(chatModel.getBaseUrl())
                .apiKey(chatModel.getApiKey())
                .modelName(chatModel.getModelName())
                .temperature(chatModel.getTemperature() == null ? 0L :
                        chatModel.getTemperature().floatValue())
                .build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(EmbeddingModelConfig embeddingModelConfig) {
        return QwenEmbeddingModel.builder()
                .apiKey(embeddingModelConfig.getApiKey())
                .modelName(embeddingModelConfig.getModelName())
                .build();
    }

    @Override
    public void afterPropertiesSet() {
        ModelProvider.add(Provider.DASHSCOPE, this);
    }
}
