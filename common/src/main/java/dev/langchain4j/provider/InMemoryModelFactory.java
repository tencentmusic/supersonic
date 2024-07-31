package dev.langchain4j.provider;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.S2OnnxEmbeddingModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

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
        if (EmbeddingModelConstant.BGE_SMALL_ZH.equalsIgnoreCase(modelName)) {
            return EmbeddingModelConstant.BGE_SMALL_ZH_MODEL;
        }
        if (EmbeddingModelConstant.ALL_MINILM_L6_V2.equalsIgnoreCase(modelName)) {
            return EmbeddingModelConstant.ALL_MINI_LM_L6_V2_MODEL;
        }
        return EmbeddingModelConstant.BGE_SMALL_ZH_MODEL;
    }

    @Override
    public void afterPropertiesSet() {
        ModelProvider.add(PROVIDER, this);
    }
}
