package com.tencent.supersonic.chat.server.util;

import com.tencent.supersonic.common.config.LLMConfig;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.provider.ChatLanguageModelProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class LLMConnHelper {
    public static boolean testConnection(LLMConfig llmConfig) {
        try {
            if (llmConfig == null || StringUtils.isBlank(llmConfig.getBaseUrl())) {
                return false;
            }
            ChatLanguageModel chatLanguageModel = ChatLanguageModelProvider.provide(llmConfig);
            String response = chatLanguageModel.generate("Hi there");
            return StringUtils.isNotEmpty(response) ? true : false;
        } catch (Exception e) {
            log.warn("connect to llm failed:", e);
            throw new InvalidArgumentException(e.getMessage());
        }
    }
}
