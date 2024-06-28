package dev.langchain4j.model.provider;

import com.tencent.supersonic.common.config.LLMConfig;
import com.tencent.supersonic.common.util.ContextUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class ChatLanguageModelProvider {
    private static final Map<String, ChatLanguageModelFactory> factories = new HashMap<>();

    static {
        factories.put(ModelProvider.OPEN_AI.name(), new OpenAiChatModelFactory());
        factories.put(ModelProvider.LOCAL_AI.name(), new LocalAiChatModelFactory());
        factories.put(ModelProvider.OLLAMA.name(), new OllamaChatModelFactory());
    }

    public static ChatLanguageModel provide(LLMConfig llmConfig) {
        if (llmConfig == null || StringUtils.isBlank(llmConfig.getProvider())
                || StringUtils.isBlank(llmConfig.getBaseUrl())) {
            return ContextUtils.getBean(ChatLanguageModel.class);
        }

        ChatLanguageModelFactory factory = factories.get(llmConfig.getProvider().toUpperCase());
        if (factory != null) {
            return factory.create(llmConfig);
        }

        throw new RuntimeException("Unsupported provider: " + llmConfig.getProvider());
    }
}