package dev.langchain4j.provider;

import com.tencent.supersonic.common.config.ChatModelConfig;
import com.tencent.supersonic.common.config.EmbeddingModelConfig;
import com.tencent.supersonic.common.config.ModelConfig;
import com.tencent.supersonic.common.util.ContextUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ModelProvider {
    private static final Map<String, ModelFactory> factories = new HashMap<>();

    public static void add(String provider, ModelFactory modelFactory) {
        factories.put(provider, modelFactory);
    }

    public static ChatLanguageModel getChatModel(ChatModelConfig modelConfig) {
        if (modelConfig == null
                || StringUtils.isBlank(modelConfig.getProvider())
                || StringUtils.isBlank(modelConfig.getBaseUrl())) {
            return ContextUtils.getBean(ChatLanguageModel.class);
        }
        ModelFactory modelFactory = factories.get(modelConfig.getProvider().toUpperCase());
        if (modelFactory != null) {
            return modelFactory.createChatModel(modelConfig);
        }

        throw new RuntimeException("Unsupported ChatLanguageModel provider: " + modelConfig.getProvider());
    }

    public static EmbeddingModel getEmbeddingModel(ModelConfig modelConfig) {
        if (modelConfig == null || Objects.isNull(modelConfig.getEmbeddingModel())
                || StringUtils.isBlank(modelConfig.getEmbeddingModel().getBaseUrl())
                || StringUtils.isBlank(modelConfig.getEmbeddingModel().getProvider())) {
            return ContextUtils.getBean(EmbeddingModel.class);
        }
        EmbeddingModelConfig embeddingModel = modelConfig.getEmbeddingModel();

        ModelFactory modelFactory = factories.get(embeddingModel.getProvider().toUpperCase());
        if (modelFactory != null) {
            return modelFactory.createEmbeddingModel(embeddingModel);
        }

        throw new RuntimeException("Unsupported EmbeddingModel provider: " + embeddingModel.getProvider());
    }
}