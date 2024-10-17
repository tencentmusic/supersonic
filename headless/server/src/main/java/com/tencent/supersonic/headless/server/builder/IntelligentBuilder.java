package com.tencent.supersonic.headless.server.builder;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.provider.ModelProvider;

public abstract class IntelligentBuilder {

    protected ChatLanguageModel getChatModel() {
        return ModelProvider.getChatModel();
    }
}
