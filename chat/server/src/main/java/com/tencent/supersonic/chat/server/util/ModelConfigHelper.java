package com.tencent.supersonic.chat.server.util;

import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.service.ChatModelService;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.enums.ChatModelType;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.common.util.ContextUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.provider.ModelProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class ModelConfigHelper {
    public static boolean testConnection(ChatModelConfig modelConfig) {
        try {
            if (modelConfig == null || StringUtils.isBlank(modelConfig.getBaseUrl())) {
                return false;
            }
            ChatLanguageModel chatLanguageModel = ModelProvider.getChatModel(modelConfig);
            String response = chatLanguageModel.generate("Hi there");
            return StringUtils.isNotEmpty(response) ? true : false;
        } catch (Exception e) {
            log.warn("connect to llm failed:", e);
            throw new InvalidArgumentException(e.getMessage());
        }
    }

    public static ChatModelConfig getChatModelConfig(ChatApp chatApp) {
        ChatModelService chatModelService = ContextUtils.getBean(ChatModelService.class);
        ChatModelConfig chatModelConfig =
                chatModelService.getChatModel(chatApp.getChatModelId()).getConfig();
        return chatModelConfig;
    }
}
