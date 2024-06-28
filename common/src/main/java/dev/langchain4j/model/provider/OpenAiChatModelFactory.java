package dev.langchain4j.model.provider;

import com.tencent.supersonic.common.config.LLMConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.time.Duration;

public class OpenAiChatModelFactory implements ChatLanguageModelFactory {
    @Override
    public ChatLanguageModel create(LLMConfig llmConfig) {
        return OpenAiChatModel
                .builder()
                .baseUrl(llmConfig.getBaseUrl())
                .modelName(llmConfig.getModelName())
                .apiKey(llmConfig.keyDecrypt())
                .temperature(llmConfig.getTemperature())
                .timeout(Duration.ofSeconds(llmConfig.getTimeOut()))
                .build();
    }
}