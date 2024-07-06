package dev.langchain4j.provider;

import com.tencent.supersonic.common.config.ChatModelConfig;
import com.tencent.supersonic.common.config.EmbeddingModelConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.embedding.BgeSmallZhEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.S2OnnxEmbeddingModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import static dev.langchain4j.inmemory.spring.InMemoryAutoConfig.ALL_MINILM_L6_V2;
import static dev.langchain4j.inmemory.spring.InMemoryAutoConfig.BGE_SMALL_ZH;

@Service
public class InMemoryModelFactory implements ModelFactory, InitializingBean {
    public static final String PROVIDER = "IN_MEMORY";

    @Override
    public ChatLanguageModel createChatModel(ChatModelConfig modelConfig) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public EmbeddingModel createEmbeddingModel(EmbeddingModelConfig embeddingModel) {
        String modelPath = embeddingModel.getModelPath();
        String vocabularyPath = embeddingModel.getVocabularyPath();
        if (StringUtils.isNotBlank(modelPath) && StringUtils.isNotBlank(vocabularyPath)) {
            return new S2OnnxEmbeddingModel(modelPath, vocabularyPath);
        }
        String modelName = embeddingModel.getModelName();
        if (BGE_SMALL_ZH.equalsIgnoreCase(modelName)) {
            return new BgeSmallZhEmbeddingModel();
        }
        if (ALL_MINILM_L6_V2.equalsIgnoreCase(modelName)) {
            return new AllMiniLmL6V2QuantizedEmbeddingModel();
        }
        return new BgeSmallZhEmbeddingModel();
    }

    @Override
    public void afterPropertiesSet() {
        ModelProvider.add(PROVIDER, this);
    }
}
