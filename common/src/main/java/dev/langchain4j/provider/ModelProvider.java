package dev.langchain4j.provider;

import com.tencent.supersonic.common.config.ChatModelParameterConfig;
import com.tencent.supersonic.common.config.EmbeddingModelParameterConfig;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import com.tencent.supersonic.common.util.ContextUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class ModelProvider {

    private static final Map<String, ModelFactory> factories = new HashMap<>();

    public static void add(String provider, ModelFactory modelFactory) {
        factories.put(provider, modelFactory);
    }

    public static ChatLanguageModel getChatModel() {
        return getChatModel(null);
    }

    public static ChatLanguageModel getChatModel(ChatModelConfig modelConfig) {
        if (modelConfig == null
                || StringUtils.isBlank(modelConfig.getProvider())
                || StringUtils.isBlank(modelConfig.getBaseUrl())) {
            ChatModelParameterConfig parameterConfig =
                    ContextUtils.getBean(ChatModelParameterConfig.class);
            modelConfig = parameterConfig.convert();
        }
        ModelFactory modelFactory = factories.get(modelConfig.getProvider().toUpperCase());
        if (modelFactory != null) {
            return modelFactory.createChatModel(modelConfig);
        }

        throw new RuntimeException(
                "Unsupported ChatLanguageModel provider: " + modelConfig.getProvider());
    }

    public static EmbeddingModel getEmbeddingModel() {
        return getEmbeddingModel(null);
    }

    public static EmbeddingModel getEmbeddingModel(EmbeddingModelConfig embeddingModel) {
        if (embeddingModel == null || StringUtils.isBlank(embeddingModel.getProvider())) {
            EmbeddingModelParameterConfig parameterConfig =
                    ContextUtils.getBean(EmbeddingModelParameterConfig.class);
            embeddingModel = parameterConfig.convert();
        }
        ModelFactory modelFactory = factories.get(embeddingModel.getProvider().toUpperCase());
        if (modelFactory != null) {
            return modelFactory.createEmbeddingModel(embeddingModel);
        }

        throw new RuntimeException(
                "Unsupported EmbeddingModel provider: " + embeddingModel.getProvider());
    }
}
