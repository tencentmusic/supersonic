package dev.langchain4j.provider;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

public interface ModelFactory {
    ChatLanguageModel createChatModel(ChatModelConfig modelConfig);

    EmbeddingModel createEmbeddingModel(EmbeddingModelConfig embeddingModel);
}
