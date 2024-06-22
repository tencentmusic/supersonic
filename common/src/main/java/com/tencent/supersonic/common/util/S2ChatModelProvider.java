package com.tencent.supersonic.common.util;

import com.tencent.supersonic.common.config.LLMConfig;
import com.tencent.supersonic.common.pojo.enums.S2ModelProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.localai.LocalAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;

public class S2ChatModelProvider {

    public static ChatLanguageModel provide(LLMConfig llmConfig) {
        ChatLanguageModel chatLanguageModel = ContextUtils.getBean(ChatLanguageModel.class);
        if (llmConfig == null || StringUtils.isBlank(llmConfig.getProvider())
                || StringUtils.isBlank(llmConfig.getBaseUrl())) {
            return chatLanguageModel;
        }
        if (S2ModelProvider.OPEN_AI.name().equalsIgnoreCase(llmConfig.getProvider())) {
            return OpenAiChatModel
                    .builder()
                    .baseUrl(llmConfig.getBaseUrl())
                    .modelName(llmConfig.getModelName())
                    .apiKey(llmConfig.getApiKey())
                    .temperature(llmConfig.getTemperature())
                    .timeout(Duration.ofSeconds(llmConfig.getTimeOut()))
                    .build();
        } else if (S2ModelProvider.LOCAL_AI.name().equalsIgnoreCase(llmConfig.getProvider())) {
            return LocalAiChatModel
                    .builder()
                    .baseUrl(llmConfig.getBaseUrl())
                    .modelName(llmConfig.getModelName())
                    .temperature(llmConfig.getTemperature())
                    .timeout(Duration.ofSeconds(llmConfig.getTimeOut()))
                    .build();
        }
        throw new RuntimeException("unsupported provider: " + llmConfig.getProvider());
    }

}
