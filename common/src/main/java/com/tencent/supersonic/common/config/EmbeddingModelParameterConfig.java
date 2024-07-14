package com.tencent.supersonic.common.config;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import com.tencent.supersonic.common.pojo.Parameter;
import dev.langchain4j.provider.AzureModelFactory;
import dev.langchain4j.provider.DashscopeModelFactory;
import dev.langchain4j.provider.InMemoryModelFactory;
import dev.langchain4j.provider.OllamaModelFactory;
import dev.langchain4j.provider.OpenAiModelFactory;
import dev.langchain4j.provider.QianfanModelFactory;
import dev.langchain4j.provider.ZhipuModelFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("EmbeddingModelParameterConfig")
@Slf4j
public class EmbeddingModelParameterConfig extends ParameterConfig {

    public static final Parameter EMBEDDING_MODEL_PROVIDER =
            new Parameter("s2.embedding.model.provider", InMemoryModelFactory.PROVIDER,
                    "接口协议", "",
                    "string", "向量模型配置",
                    Lists.newArrayList(InMemoryModelFactory.PROVIDER,
                            OpenAiModelFactory.PROVIDER,
                            OllamaModelFactory.PROVIDER,
                            AzureModelFactory.PROVIDER,
                            DashscopeModelFactory.PROVIDER,
                            QianfanModelFactory.PROVIDER,
                            ZhipuModelFactory.PROVIDER));

    public static final Parameter EMBEDDING_MODEL_BASE_URL =
            new Parameter("s2.embedding.model.base.url", "",
                    "BaseUrl", "",
                    "string", "向量模型配置");

    public static final Parameter EMBEDDING_MODEL_API_KEY =
            new Parameter("s2.embedding.model.api.key", "",
                    "ApiKey", "",
                    "string", "向量模型配置");


    public static final Parameter EMBEDDING_MODEL_NAME =
            new Parameter("s2.embedding.model.name", "",
                    "ModelName", "",
                    "string", "向量模型配置");

    public static final Parameter EMBEDDING_MODEL_PATH =
            new Parameter("s2.embedding.model.path", "",
                    "模型路径", "",
                    "string", "向量模型配置");

    public static final Parameter EMBEDDING_MODEL_VOCABULARY_PATH =
            new Parameter("s2.embedding.model.vocabulary.path", "",
                    "词汇表路径", "",
                    "string", "向量模型配置");

    @Override
    public List<Parameter> getSysParameters() {
        return Lists.newArrayList(
                EMBEDDING_MODEL_PROVIDER, EMBEDDING_MODEL_BASE_URL, EMBEDDING_MODEL_API_KEY,
                EMBEDDING_MODEL_NAME, EMBEDDING_MODEL_PATH, EMBEDDING_MODEL_VOCABULARY_PATH
        );
    }

    public EmbeddingModelConfig convert() {
        String provider = getParameterValue(EMBEDDING_MODEL_PROVIDER);
        String baseUrl = getParameterValue(EMBEDDING_MODEL_BASE_URL);
        String apiKey = getParameterValue(EMBEDDING_MODEL_API_KEY);
        String modelName = getParameterValue(EMBEDDING_MODEL_NAME);
        String modelPath = getParameterValue(EMBEDDING_MODEL_PATH);
        String vocabularyPath = getParameterValue(EMBEDDING_MODEL_VOCABULARY_PATH);

        return EmbeddingModelConfig.builder()
                .provider(provider)
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .modelPath(modelPath)
                .vocabularyPath(vocabularyPath)
                .build();
    }

}
