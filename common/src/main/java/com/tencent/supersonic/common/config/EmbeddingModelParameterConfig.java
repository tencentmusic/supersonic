package com.tencent.supersonic.common.config;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import com.tencent.supersonic.common.pojo.Parameter;
import dev.langchain4j.provider.AzureModelFactory;
import dev.langchain4j.provider.DashscopeModelFactory;
import dev.langchain4j.provider.EmbeddingModelConstant;
import dev.langchain4j.provider.InMemoryModelFactory;
import dev.langchain4j.provider.OllamaModelFactory;
import dev.langchain4j.provider.OpenAiModelFactory;
import dev.langchain4j.provider.QianfanModelFactory;
import dev.langchain4j.provider.ZhipuModelFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("EmbeddingModelParameterConfig")
@Slf4j
public class EmbeddingModelParameterConfig extends ParameterConfig {
    private static final String MODULE_NAME = "嵌入模型配置";

    public static final Parameter EMBEDDING_MODEL_PROVIDER =
            new Parameter("s2.embedding.model.provider", InMemoryModelFactory.PROVIDER, "接口协议", "",
                    "list", MODULE_NAME, getCandidateValues());
    public static final Parameter EMBEDDING_MODEL_BASE_URL =
            new Parameter("s2.embedding.model.base.url", "", "BaseUrl", "", "string", MODULE_NAME,
                    null, getBaseUrlDependency());

    public static final Parameter EMBEDDING_MODEL_API_KEY =
            new Parameter("s2.embedding.model.api.key", "", "ApiKey", "", "password", MODULE_NAME,
                    null, getApiKeyDependency());

    public static final Parameter EMBEDDING_MODEL_SECRET_KEY =
            new Parameter("s2.embedding.model.secretKey", "demo", "SecretKey", "", "password",
                    MODULE_NAME, null, getSecretKeyDependency());

    public static final Parameter EMBEDDING_MODEL_NAME =
            new Parameter("s2.embedding.model.name", EmbeddingModelConstant.BGE_SMALL_ZH,
                    "ModelName", "", "string", MODULE_NAME, null, getModelNameDependency());

    public static final Parameter EMBEDDING_MODEL_PATH = new Parameter("s2.embedding.model.path",
            "", "模型路径", "", "string", MODULE_NAME, null, getModelPathDependency());
    public static final Parameter EMBEDDING_MODEL_VOCABULARY_PATH =
            new Parameter("s2.embedding.model.vocabulary.path", "", "词汇表路径", "", "string",
                    MODULE_NAME, null, getModelPathDependency());

    @Override
    public List<Parameter> getSysParameters() {
        return Lists.newArrayList(EMBEDDING_MODEL_PROVIDER, EMBEDDING_MODEL_BASE_URL,
                EMBEDDING_MODEL_API_KEY, EMBEDDING_MODEL_SECRET_KEY, EMBEDDING_MODEL_NAME,
                EMBEDDING_MODEL_PATH, EMBEDDING_MODEL_VOCABULARY_PATH);
    }

    public EmbeddingModelConfig convert() {
        String provider = getParameterValue(EMBEDDING_MODEL_PROVIDER);
        String baseUrl = getParameterValue(EMBEDDING_MODEL_BASE_URL);
        String apiKey = getParameterValue(EMBEDDING_MODEL_API_KEY);
        String modelName = getParameterValue(EMBEDDING_MODEL_NAME);
        String modelPath = getParameterValue(EMBEDDING_MODEL_PATH);
        String vocabularyPath = getParameterValue(EMBEDDING_MODEL_VOCABULARY_PATH);
        String secretKey = getParameterValue(EMBEDDING_MODEL_SECRET_KEY);
        return EmbeddingModelConfig.builder().provider(provider).baseUrl(baseUrl).apiKey(apiKey)
                .secretKey(secretKey).modelName(modelName).modelPath(modelPath)
                .vocabularyPath(vocabularyPath).build();
    }

    private static ArrayList<String> getCandidateValues() {
        return Lists.newArrayList(InMemoryModelFactory.PROVIDER, OpenAiModelFactory.PROVIDER,
                OllamaModelFactory.PROVIDER, DashscopeModelFactory.PROVIDER,
                QianfanModelFactory.PROVIDER, ZhipuModelFactory.PROVIDER,
                AzureModelFactory.PROVIDER);
    }

    private static List<Parameter.Dependency> getBaseUrlDependency() {
        return getDependency(EMBEDDING_MODEL_PROVIDER.getName(),
                Lists.newArrayList(OpenAiModelFactory.PROVIDER, OllamaModelFactory.PROVIDER,
                        AzureModelFactory.PROVIDER, DashscopeModelFactory.PROVIDER,
                        QianfanModelFactory.PROVIDER, ZhipuModelFactory.PROVIDER),
                ImmutableMap.of(OpenAiModelFactory.PROVIDER, OpenAiModelFactory.DEFAULT_BASE_URL,
                        OllamaModelFactory.PROVIDER, OllamaModelFactory.DEFAULT_BASE_URL,
                        AzureModelFactory.PROVIDER, AzureModelFactory.DEFAULT_BASE_URL,
                        DashscopeModelFactory.PROVIDER, DashscopeModelFactory.DEFAULT_BASE_URL,
                        QianfanModelFactory.PROVIDER, QianfanModelFactory.DEFAULT_BASE_URL,
                        ZhipuModelFactory.PROVIDER, ZhipuModelFactory.DEFAULT_BASE_URL));
    }

    private static List<Parameter.Dependency> getApiKeyDependency() {
        return getDependency(EMBEDDING_MODEL_PROVIDER.getName(),
                Lists.newArrayList(OpenAiModelFactory.PROVIDER, AzureModelFactory.PROVIDER,
                        DashscopeModelFactory.PROVIDER, QianfanModelFactory.PROVIDER,
                        ZhipuModelFactory.PROVIDER),
                ImmutableMap.of(OpenAiModelFactory.PROVIDER, DEMO, AzureModelFactory.PROVIDER, DEMO,
                        DashscopeModelFactory.PROVIDER, DEMO, QianfanModelFactory.PROVIDER, DEMO,
                        ZhipuModelFactory.PROVIDER, DEMO));
    }

    private static List<Parameter.Dependency> getModelNameDependency() {
        return getDependency(EMBEDDING_MODEL_PROVIDER.getName(),
                Lists.newArrayList(InMemoryModelFactory.PROVIDER, OpenAiModelFactory.PROVIDER,
                        OllamaModelFactory.PROVIDER, AzureModelFactory.PROVIDER,
                        DashscopeModelFactory.PROVIDER, QianfanModelFactory.PROVIDER,
                        ZhipuModelFactory.PROVIDER),
                ImmutableMap.of(InMemoryModelFactory.PROVIDER, EmbeddingModelConstant.BGE_SMALL_ZH,
                        OpenAiModelFactory.PROVIDER,
                        OpenAiModelFactory.DEFAULT_EMBEDDING_MODEL_NAME,
                        OllamaModelFactory.PROVIDER,
                        OllamaModelFactory.DEFAULT_EMBEDDING_MODEL_NAME, AzureModelFactory.PROVIDER,
                        AzureModelFactory.DEFAULT_EMBEDDING_MODEL_NAME,
                        DashscopeModelFactory.PROVIDER,
                        DashscopeModelFactory.DEFAULT_EMBEDDING_MODEL_NAME,
                        QianfanModelFactory.PROVIDER,
                        QianfanModelFactory.DEFAULT_EMBEDDING_MODEL_NAME,
                        ZhipuModelFactory.PROVIDER,
                        ZhipuModelFactory.DEFAULT_EMBEDDING_MODEL_NAME));
    }

    private static List<Parameter.Dependency> getModelPathDependency() {
        return getDependency(EMBEDDING_MODEL_PROVIDER.getName(),
                Lists.newArrayList(InMemoryModelFactory.PROVIDER),
                ImmutableMap.of(InMemoryModelFactory.PROVIDER, ""));
    }

    private static List<Parameter.Dependency> getSecretKeyDependency() {
        return getDependency(EMBEDDING_MODEL_PROVIDER.getName(),
                Lists.newArrayList(QianfanModelFactory.PROVIDER),
                ImmutableMap.of(QianfanModelFactory.PROVIDER, DEMO));
    }
}
