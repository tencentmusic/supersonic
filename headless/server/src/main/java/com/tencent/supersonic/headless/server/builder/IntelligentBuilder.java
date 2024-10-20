package com.tencent.supersonic.headless.server.builder;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.provider.ModelProvider;

public abstract class IntelligentBuilder {

    protected ChatLanguageModel getChatModel(ChatModelConfig chatModelConfig) {
        return ModelProvider.getChatModel(chatModelConfig);
    }
}
