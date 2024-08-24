package com.tencent.supersonic.provider;

import com.tencent.supersonic.BaseApplication;
import com.tencent.supersonic.common.config.ParameterConfig;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.provider.AzureModelFactory;
import dev.langchain4j.provider.DashscopeModelFactory;
import dev.langchain4j.provider.EmbeddingModelConstant;
import dev.langchain4j.provider.InMemoryModelFactory;
import dev.langchain4j.provider.ModelProvider;
import dev.langchain4j.provider.OpenAiModelFactory;
import dev.langchain4j.provider.QianfanModelFactory;
import dev.langchain4j.provider.ZhipuModelFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    public void test_qianfan_chat_model() {
        ChatModelConfig modelConfig = new ChatModelConfig();
        modelConfig.setProvider(QianfanModelFactory.PROVIDER);
        modelConfig.setModelName(QianfanModelFactory.DEFAULT_MODEL_NAME);
        modelConfig.setBaseUrl(QianfanModelFactory.DEFAULT_BASE_URL);
        modelConfig.setSecretKey(ParameterConfig.DEMO);
        modelConfig.setApiKey(ParameterConfig.DEMO);
        modelConfig.setEndpoint(QianfanModelFactory.DEFAULT_ENDPOINT);

        ChatLanguageModel chatModel = ModelProvider.getChatModel(modelConfig);
        assertThrows(RuntimeException.class, () -> {
            chatModel.generate("hi");
        });
    }

    @Test
    public void test_zhipu_chat_model() {
        ChatModelConfig modelConfig = new ChatModelConfig();
        modelConfig.setProvider(ZhipuModelFactory.PROVIDER);
        modelConfig.setModelName(ZhipuModelFactory.DEFAULT_MODEL_NAME);
        modelConfig.setBaseUrl(ZhipuModelFactory.DEFAULT_BASE_URL);
        modelConfig.setApiKey("e2724491714b3b2a0274e987905f1001.5JyHgf4vbZVJ7gC5");

        ChatLanguageModel chatModel = ModelProvider.getChatModel(modelConfig);
        assertThrows(RuntimeException.class, () -> {
            chatModel.generate("hi");
        });
    }

    @Test
    public void test_dashscope_chat_model() {
        ChatModelConfig modelConfig = new ChatModelConfig();
        modelConfig.setProvider(DashscopeModelFactory.PROVIDER);
        modelConfig.setModelName(DashscopeModelFactory.DEFAULT_MODEL_NAME);
        modelConfig.setBaseUrl(DashscopeModelFactory.DEFAULT_BASE_URL);
        modelConfig.setEnableSearch(true);
        modelConfig.setApiKey(ParameterConfig.DEMO);

        ChatLanguageModel chatModel = ModelProvider.getChatModel(modelConfig);
        assertThrows(RuntimeException.class, () -> {
            chatModel.generate("hi");
        });
    }

    @Test
    public void test_azure_chat_model() {
        ChatModelConfig modelConfig = new ChatModelConfig();
        modelConfig.setProvider(AzureModelFactory.PROVIDER);
        modelConfig.setModelName(AzureModelFactory.DEFAULT_MODEL_NAME);
        modelConfig.setBaseUrl(AzureModelFactory.DEFAULT_BASE_URL);
        modelConfig.setApiKey(ParameterConfig.DEMO);

        ChatLanguageModel chatModel = ModelProvider.getChatModel(modelConfig);
        assertThrows(RuntimeException.class, () -> {
            chatModel.generate("hi");
        });
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

    @Test
    public void test_azure_embedding_model() {
        EmbeddingModelConfig modelConfig = new EmbeddingModelConfig();
        modelConfig.setProvider(AzureModelFactory.PROVIDER);
        modelConfig.setModelName(AzureModelFactory.DEFAULT_EMBEDDING_MODEL_NAME);
        modelConfig.setBaseUrl(AzureModelFactory.DEFAULT_BASE_URL);
        modelConfig.setApiKey(ParameterConfig.DEMO);

        EmbeddingModel embeddingModel = ModelProvider.getEmbeddingModel(modelConfig);
        assertThrows(RuntimeException.class, () -> {
            embeddingModel.embed("hi");
        });
    }

    @Test
    public void test_dashscope_embedding_model() {
        EmbeddingModelConfig modelConfig = new EmbeddingModelConfig();
        modelConfig.setProvider(DashscopeModelFactory.PROVIDER);
        modelConfig.setModelName(DashscopeModelFactory.DEFAULT_EMBEDDING_MODEL_NAME);
        modelConfig.setBaseUrl(DashscopeModelFactory.DEFAULT_BASE_URL);
        modelConfig.setApiKey(ParameterConfig.DEMO);

        EmbeddingModel embeddingModel = ModelProvider.getEmbeddingModel(modelConfig);
        assertThrows(RuntimeException.class, () -> {
            embeddingModel.embed("hi");
        });
    }

    @Test
    public void test_qianfan_embedding_model() {
        EmbeddingModelConfig modelConfig = new EmbeddingModelConfig();
        modelConfig.setProvider(QianfanModelFactory.PROVIDER);
        modelConfig.setModelName(QianfanModelFactory.DEFAULT_EMBEDDING_MODEL_NAME);
        modelConfig.setBaseUrl(QianfanModelFactory.DEFAULT_BASE_URL);
        modelConfig.setApiKey(ParameterConfig.DEMO);
        modelConfig.setSecretKey(ParameterConfig.DEMO);

        EmbeddingModel embeddingModel = ModelProvider.getEmbeddingModel(modelConfig);
        assertThrows(RuntimeException.class, () -> {
            embeddingModel.embed("hi");
        });
    }

    @Test
    public void test_zhipu_embedding_model() {
        EmbeddingModelConfig modelConfig = new EmbeddingModelConfig();
        modelConfig.setProvider(ZhipuModelFactory.PROVIDER);
        modelConfig.setModelName(ZhipuModelFactory.DEFAULT_EMBEDDING_MODEL_NAME);
        modelConfig.setBaseUrl(ZhipuModelFactory.DEFAULT_BASE_URL);
        modelConfig.setApiKey("e2724491714b3b2a0274e987905f1001.5JyHgf4vbZVJ7gC5");

        EmbeddingModel embeddingModel = ModelProvider.getEmbeddingModel(modelConfig);
        assertThrows(RuntimeException.class, () -> {
            embeddingModel.embed("hi");
        });
    }
}
