package dev.langchain4j.model.provider;

import com.tencent.supersonic.common.config.LLMConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;

public interface ChatLanguageModelFactory {
    ChatLanguageModel create(LLMConfig llmConfig);
}
