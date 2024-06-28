package dev.langchain4j.model.provider;

import com.tencent.supersonic.common.config.LLMConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.localai.LocalAiChatModel;

import java.time.Duration;

public class LocalAiChatModelFactory implements ChatLanguageModelFactory {
    @Override
    public ChatLanguageModel create(LLMConfig llmConfig) {
        return LocalAiChatModel
                .builder()
                .baseUrl(llmConfig.getBaseUrl())
                .modelName(llmConfig.getModelName())
                .temperature(llmConfig.getTemperature())
                .timeout(Duration.ofSeconds(llmConfig.getTimeOut()))
                .build();
    }
}