package com.tencent.supersonic.provider;

import com.tencent.supersonic.BaseApplication;
import com.tencent.supersonic.common.config.ParameterConfig;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.provider.EmbeddingModelConstant;
import dev.langchain4j.provider.InMemoryModelFactory;
import dev.langchain4j.provider.ModelProvider;
import dev.langchain4j.provider.OpenAiModelFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.Assert.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
public class ModelProviderTest extends BaseApplication {

    @Test
    public void test_openai_chat_model_with_openapi() {
        ChatModelConfig modelConfig = new ChatModelConfig();
        modelConfig.setProvider(OpenAiModelFactory.PROVIDER);
        modelConfig.setModelName(OpenAiModelFactory.DEFAULT_MODEL_NAME);
        modelConfig.setBaseUrl(OpenAiModelFactory.DEFAULT_BASE_URL);
        modelConfig.setApiKey(ParameterConfig.DEMO);

        ChatLanguageModel chatModel = ModelProvider.getChatModel(modelConfig);
        String response = chatModel.generate("hi");
        assertNotNull(response);
    }

    @Test
    public void test_in_memory_embedding_model() {
        EmbeddingModelConfig modelConfig = new EmbeddingModelConfig();
        modelConfig.setProvider(InMemoryModelFactory.PROVIDER);
        modelConfig.setModelName(EmbeddingModelConstant.BGE_SMALL_ZH);

        EmbeddingModel embeddingModel = ModelProvider.getEmbeddingModel(modelConfig);
        Response<Embedding> embed = embeddingModel.embed("hi");
        assertNotNull(embed);
    }

    @Test
    public void test_openai_embedding_model() {
        EmbeddingModelConfig modelConfig = new EmbeddingModelConfig();
        modelConfig.setProvider(OpenAiModelFactory.PROVIDER);
        modelConfig.setModelName(OpenAiModelFactory.DEFAULT_EMBEDDING_MODEL_NAME);
        modelConfig.setBaseUrl(OpenAiModelFactory.DEFAULT_BASE_URL);
        modelConfig.setApiKey(ParameterConfig.DEMO);

        EmbeddingModel embeddingModel = ModelProvider.getEmbeddingModel(modelConfig);
        Response<Embedding> embed = embeddingModel.embed("hi");
        assertNotNull(embed);
    }
}
