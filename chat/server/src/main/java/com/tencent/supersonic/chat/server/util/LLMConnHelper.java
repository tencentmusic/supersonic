package com.tencent.supersonic.chat.server.util;

import com.tencent.supersonic.common.config.ChatModelConfig;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.provider.ModelProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class LLMConnHelper {
    public static boolean testConnection(ChatModelConfig chatModel) {
        try {
            if (chatModel == null || StringUtils.isBlank(chatModel.getBaseUrl())) {
                return false;
            }
            ChatLanguageModel chatLanguageModel = ModelProvider.provideChatModel(chatModel);
            String response = chatLanguageModel.generate("Hi there");
            return StringUtils.isNotEmpty(response) ? true : false;
        } catch (Exception e) {
            log.warn("connect to llm failed:", e);
            throw new InvalidArgumentException(e.getMessage());
        }
    }
}
