package dev.langchain4j.provider;

import com.tencent.supersonic.common.config.ChatModel;
import com.tencent.supersonic.common.config.EmbeddingModelParameterConfig;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.service.ChatModelService;
import com.tencent.supersonic.common.config.SystemConfig;
import com.tencent.supersonic.common.service.SystemConfigService;
import com.tencent.supersonic.common.config.AliasGenerateParameterConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class ModelProvider {

    public static final ChatModelConfig DEMO_CHAT_MODEL =
            ChatModelConfig.builder().provider("OPEN_AI").baseUrl("https://api.openai.com/v1")
                    .apiKey("demo").modelName("gpt-4o-mini").temperature(0.0).timeOut(60L).build();

    private static final Map<String, ModelFactory> factories = new HashMap<>();

    public static void add(String provider, ModelFactory modelFactory) {
        factories.put(provider, modelFactory);
    }

    public static ChatLanguageModel getChatModel() {
        return getChatModel(null);
    }

    public static ChatLanguageModel getChatModel(ChatModelConfig modelConfig) {
        if (modelConfig == null || StringUtils.isBlank(modelConfig.getProvider())
                || StringUtils.isBlank(modelConfig.getBaseUrl())) {
            SystemConfigService systemConfigService = ContextUtils.getBean(SystemConfigService.class);
            SystemConfig systemConfig = systemConfigService.getSystemConfig();
            String modelId = systemConfig.getParameterByName(
                    AliasGenerateParameterConfig.LLM_ALIAS_GENERATION_MODEL.getName());
            if (StringUtils.isNotBlank(modelId)) {
                ChatModelService chatModelService = ContextUtils.getBean(ChatModelService.class);
                ChatModel chatModel = chatModelService.getChatModel(Integer.parseInt(modelId));
                if (chatModel != null) {
                    modelConfig = chatModel.getConfig();
                } else {
                    modelConfig = DEMO_CHAT_MODEL;
                }
            } else {
                modelConfig = DEMO_CHAT_MODEL;
            }
        }
        if (modelConfig == null) {
            modelConfig = DEMO_CHAT_MODEL;
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
