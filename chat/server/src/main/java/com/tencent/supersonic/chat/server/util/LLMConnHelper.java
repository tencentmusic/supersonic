package com.tencent.supersonic.chat.server.util;

import com.tencent.supersonic.common.config.LLMConfig;
import com.tencent.supersonic.common.util.S2ChatModelProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.apache.commons.lang3.StringUtils;

public class LLMConnHelper {
    public static boolean testConnection(LLMConfig llmConfig) {
        try {
            ChatLanguageModel chatLanguageModel = S2ChatModelProvider.provide(llmConfig);
            String response = chatLanguageModel.generate("Hi there");
            return StringUtils.isNotEmpty(response) ? true : false;
        } catch (Exception e) {
            return false;
        }
    }
}
