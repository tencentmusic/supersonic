package dev.langchain4j.model.provider;

import com.tencent.supersonic.common.config.LLMConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;

import java.time.Duration;

public class OllamaChatModelFactory implements ChatLanguageModelFactory {
    @Override
    public ChatLanguageModel create(LLMConfig llmConfig) {
        return OllamaChatModel
                .builder()
                .baseUrl(llmConfig.getBaseUrl())
                .modelName(llmConfig.getModelName())
                .temperature(llmConfig.getTemperature())
                .timeout(Duration.ofSeconds(llmConfig.getTimeOut()))
                .build();
    }
}