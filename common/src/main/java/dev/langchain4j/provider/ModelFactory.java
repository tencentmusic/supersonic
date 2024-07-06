package dev.langchain4j.provider;

import com.tencent.supersonic.common.config.ChatModelConfig;
import com.tencent.supersonic.common.config.EmbeddingModelConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

public interface ModelFactory {
    ChatLanguageModel createChatModel(ChatModelConfig modelConfig);

    EmbeddingModel createEmbeddingModel(EmbeddingModelConfig embeddingModel);
}
