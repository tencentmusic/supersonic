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

    public static final Parameter EMBEDDING_MODEL_PROVIDER =
            new Parameter("s2.embedding.model.provider", InMemoryModelFactory.PROVIDER,
                    "接口协议", "",
                    "list", "向量模型配置",
                    getCandidateValues());
    public static final Parameter EMBEDDING_MODEL_BASE_URL =
            new Parameter("s2.embedding.model.base.url", "",
                    "BaseUrl", "",
                    "string", "向量模型配置", null,
                    getDependency(EMBEDDING_MODEL_PROVIDER.getName(),
                    Lists.newArrayList(
                            OpenAiModelFactory.PROVIDER,
                            OllamaModelFactory.PROVIDER,
                            AzureModelFactory.PROVIDER,
                            DashscopeModelFactory.PROVIDER,
                            QianfanModelFactory.PROVIDER,
                            ZhipuModelFactory.PROVIDER
                    ),
                    ImmutableMap.of(
                            OpenAiModelFactory.PROVIDER, "https://api.openai.com/v1",
                            OllamaModelFactory.PROVIDER, "http://localhost:11434",
                            AzureModelFactory.PROVIDER, "https://xxxx.openai.azure.com/",
                            DashscopeModelFactory.PROVIDER, "https://dashscope.aliyuncs.com/compatible-mode/v1",
                            QianfanModelFactory.PROVIDER, "https://aip.baidubce.com",
                            ZhipuModelFactory.PROVIDER, "https://open.bigmodel.cn/api/paas/v4/"
                    )
            )
            );

    public static final Parameter EMBEDDING_MODEL_API_KEY =
            new Parameter("s2.embedding.model.api.key", "",
                    "ApiKey", "",
                    "string", "向量模型配置", null,
                    getDependency(EMBEDDING_MODEL_PROVIDER.getName(),
                    Lists.newArrayList(
                            OpenAiModelFactory.PROVIDER,
                            OllamaModelFactory.PROVIDER,
                            AzureModelFactory.PROVIDER,
                            DashscopeModelFactory.PROVIDER,
                            QianfanModelFactory.PROVIDER,
                            ZhipuModelFactory.PROVIDER
                    ),
                    ImmutableMap.of(
                            OpenAiModelFactory.PROVIDER, "demo",
                            OllamaModelFactory.PROVIDER, "demo",
                            AzureModelFactory.PROVIDER, "demo",
                            DashscopeModelFactory.PROVIDER, "demo",
                            QianfanModelFactory.PROVIDER, "demo",
                            ZhipuModelFactory.PROVIDER, "demo"
                    )
            ));


    public static final Parameter EMBEDDING_MODEL_NAME =
            new Parameter("s2.embedding.model.name", EmbeddingModelConstant.BGE_SMALL_ZH,
                    "ModelName", "",
                    "string", "向量模型配置", null,
                    getDependency(EMBEDDING_MODEL_PROVIDER.getName(),
                    Lists.newArrayList(
                            InMemoryModelFactory.PROVIDER,
                            OpenAiModelFactory.PROVIDER,
                            OllamaModelFactory.PROVIDER,
                            AzureModelFactory.PROVIDER,
                            DashscopeModelFactory.PROVIDER,
                            QianfanModelFactory.PROVIDER,
                            ZhipuModelFactory.PROVIDER
                    ),
                    ImmutableMap.of(
                            InMemoryModelFactory.PROVIDER, EmbeddingModelConstant.BGE_SMALL_ZH,
                            OpenAiModelFactory.PROVIDER, "text-embedding-ada-002",
                            OllamaModelFactory.PROVIDER, "all-minilm",
                            AzureModelFactory.PROVIDER, "text-embedding-ada-002",
                            DashscopeModelFactory.PROVIDER, "text-embedding-ada-002",
                            QianfanModelFactory.PROVIDER, "text-embedding-ada-002",
                            ZhipuModelFactory.PROVIDER, "text-embedding-ada-002"
                    )
            ));

    public static final Parameter EMBEDDING_MODEL_PATH =
            new Parameter("s2.embedding.model.path", "",
                    "模型路径", "",
                    "string", "向量模型配置", null,
                    getDependency(EMBEDDING_MODEL_PROVIDER.getName(),
                    Lists.newArrayList(
                            InMemoryModelFactory.PROVIDER
                    ),
                    ImmutableMap.of(
                            InMemoryModelFactory.PROVIDER, ""
                    )
            ));

    public static final Parameter EMBEDDING_MODEL_VOCABULARY_PATH =
            new Parameter("s2.embedding.model.vocabulary.path", "",
                    "词汇表路径", "",
                    "string", "向量模型配置", null,
                    getDependency(EMBEDDING_MODEL_PROVIDER.getName(),
                    Lists.newArrayList(
                            InMemoryModelFactory.PROVIDER
                    ),
                    ImmutableMap.of(
                            InMemoryModelFactory.PROVIDER, ""
                    )));

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

    private static ArrayList<String> getCandidateValues() {
        return Lists.newArrayList(InMemoryModelFactory.PROVIDER,
                OpenAiModelFactory.PROVIDER,
                OllamaModelFactory.PROVIDER,
                AzureModelFactory.PROVIDER,
                DashscopeModelFactory.PROVIDER,
                QianfanModelFactory.PROVIDER,
                ZhipuModelFactory.PROVIDER);
    }

}
