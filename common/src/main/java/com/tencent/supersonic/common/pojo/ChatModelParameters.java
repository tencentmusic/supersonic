package com.tencent.supersonic.common.pojo;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import dev.langchain4j.provider.AzureModelFactory;
import dev.langchain4j.provider.DashscopeModelFactory;
import dev.langchain4j.provider.DifyModelFactory;
import dev.langchain4j.provider.LocalAiModelFactory;
import dev.langchain4j.provider.ModelProvider;
import dev.langchain4j.provider.OllamaModelFactory;
import dev.langchain4j.provider.OpenAiModelFactory;
import dev.langchain4j.provider.QianfanModelFactory;
import dev.langchain4j.provider.ZhipuModelFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatModelParameters {
    private static final String MODULE_NAME = "对话模型配置";

    public static final Parameter CHAT_MODEL_PROVIDER =
            new Parameter("provider", ModelProvider.DEMO_CHAT_MODEL.getProvider(), "接口协议", "",
                    "list", MODULE_NAME, getCandidateValues());

    public static final Parameter CHAT_MODEL_BASE_URL =
            new Parameter("baseUrl", ModelProvider.DEMO_CHAT_MODEL.getBaseUrl(), "BaseUrl", "",
                    "string", MODULE_NAME, null, getBaseUrlDependency());

    public static final Parameter CHAT_MODEL_NAME =
            new Parameter("modelName", ModelProvider.DEMO_CHAT_MODEL.getModelName(), "ModelName",
                    "", "string", MODULE_NAME, null, getModelNameDependency());

    public static final Parameter CHAT_MODEL_API_KEY = new Parameter("apiKey", "", "ApiKey", "",
            "password", MODULE_NAME, null, getApiKeyDependency());

    public static final Parameter CHAT_MODEL_API_VERSION = new Parameter("apiVersion", "2024-02-01",
            "ApiVersion", "", "string", MODULE_NAME, null, getApiVersionDependency());

    public static final Parameter CHAT_MODEL_ENDPOINT = new Parameter("endpoint", "llama_2_70b",
            "Endpoint", "", "string", MODULE_NAME, null, getEndpointDependency());

    public static final Parameter CHAT_MODEL_SECRET_KEY = new Parameter("secretKey", "demo",
            "SecretKey", "", "password", MODULE_NAME, null, getSecretKeyDependency());

    public static final Parameter CHAT_MODEL_ENABLE_SEARCH = new Parameter("enableSearch", "false",
            "是否启用搜索增强功能，设为false表示不启用", "", "bool", MODULE_NAME, null, getEnableSearchDependency());

    public static final Parameter CHAT_MODEL_TEMPERATURE =
            new Parameter("temperature", "0.0", "Temperature", "", "slider", MODULE_NAME);

    public static final Parameter CHAT_MODEL_TIMEOUT =
            new Parameter("timeOut", "60", "超时时间(秒)", "", "number", MODULE_NAME);

    public static List<Parameter> getParameters() {
        return Lists.newArrayList(CHAT_MODEL_PROVIDER, CHAT_MODEL_BASE_URL, CHAT_MODEL_ENDPOINT,
                CHAT_MODEL_API_KEY, CHAT_MODEL_SECRET_KEY, CHAT_MODEL_NAME, CHAT_MODEL_API_VERSION,
                CHAT_MODEL_ENABLE_SEARCH, CHAT_MODEL_TEMPERATURE, CHAT_MODEL_TIMEOUT);
    }

    private static List<String> getCandidateValues() {
        return Lists.newArrayList(OpenAiModelFactory.PROVIDER, OllamaModelFactory.PROVIDER,
                QianfanModelFactory.PROVIDER, ZhipuModelFactory.PROVIDER,
                LocalAiModelFactory.PROVIDER, DashscopeModelFactory.PROVIDER,
                AzureModelFactory.PROVIDER, DifyModelFactory.PROVIDER);
    }

    private static List<Parameter.Dependency> getBaseUrlDependency() {
        return getDependency(CHAT_MODEL_PROVIDER.getName(), getCandidateValues(),
                ImmutableMap.of(OpenAiModelFactory.PROVIDER, OpenAiModelFactory.DEFAULT_BASE_URL,
                        AzureModelFactory.PROVIDER, AzureModelFactory.DEFAULT_BASE_URL,
                        OllamaModelFactory.PROVIDER, OllamaModelFactory.DEFAULT_BASE_URL,
                        QianfanModelFactory.PROVIDER, QianfanModelFactory.DEFAULT_BASE_URL,
                        ZhipuModelFactory.PROVIDER, ZhipuModelFactory.DEFAULT_BASE_URL,
                        LocalAiModelFactory.PROVIDER, LocalAiModelFactory.DEFAULT_BASE_URL,
                        DashscopeModelFactory.PROVIDER, DashscopeModelFactory.DEFAULT_BASE_URL,
                        DifyModelFactory.PROVIDER, DifyModelFactory.DEFAULT_BASE_URL));
    }

    private static List<Parameter.Dependency> getApiKeyDependency() {
        return getDependency(CHAT_MODEL_PROVIDER.getName(),
                Lists.newArrayList(OpenAiModelFactory.PROVIDER, QianfanModelFactory.PROVIDER,
                        ZhipuModelFactory.PROVIDER, LocalAiModelFactory.PROVIDER,
                        AzureModelFactory.PROVIDER, DashscopeModelFactory.PROVIDER,
                        DifyModelFactory.PROVIDER),
                ImmutableMap.of(OpenAiModelFactory.PROVIDER,
                        ModelProvider.DEMO_CHAT_MODEL.getApiKey(), QianfanModelFactory.PROVIDER,
                        ModelProvider.DEMO_CHAT_MODEL.getApiKey(), ZhipuModelFactory.PROVIDER,
                        ModelProvider.DEMO_CHAT_MODEL.getApiKey(), LocalAiModelFactory.PROVIDER,
                        ModelProvider.DEMO_CHAT_MODEL.getApiKey(), AzureModelFactory.PROVIDER,
                        ModelProvider.DEMO_CHAT_MODEL.getApiKey(), DashscopeModelFactory.PROVIDER,
                        ModelProvider.DEMO_CHAT_MODEL.getApiKey(), DifyModelFactory.PROVIDER,
                        ModelProvider.DEMO_CHAT_MODEL.getApiKey()));
    }

    private static List<Parameter.Dependency> getApiVersionDependency() {
        return getDependency(CHAT_MODEL_PROVIDER.getName(),
                Lists.newArrayList(OpenAiModelFactory.PROVIDER), ImmutableMap
                        .of(OpenAiModelFactory.PROVIDER, OpenAiModelFactory.DEFAULT_API_VERSION));
    }

    private static List<Parameter.Dependency> getModelNameDependency() {
        return getDependency(CHAT_MODEL_PROVIDER.getName(), getCandidateValues(),
                ImmutableMap.of(OpenAiModelFactory.PROVIDER, OpenAiModelFactory.DEFAULT_MODEL_NAME,
                        OllamaModelFactory.PROVIDER, OllamaModelFactory.DEFAULT_MODEL_NAME,
                        QianfanModelFactory.PROVIDER, QianfanModelFactory.DEFAULT_MODEL_NAME,
                        ZhipuModelFactory.PROVIDER, ZhipuModelFactory.DEFAULT_MODEL_NAME,
                        LocalAiModelFactory.PROVIDER, LocalAiModelFactory.DEFAULT_MODEL_NAME,
                        AzureModelFactory.PROVIDER, AzureModelFactory.DEFAULT_MODEL_NAME,
                        DashscopeModelFactory.PROVIDER, DashscopeModelFactory.DEFAULT_MODEL_NAME,
                        DifyModelFactory.PROVIDER, DifyModelFactory.DEFAULT_MODEL_NAME));
    }

    private static List<Parameter.Dependency> getEndpointDependency() {
        return getDependency(CHAT_MODEL_PROVIDER.getName(),
                Lists.newArrayList(QianfanModelFactory.PROVIDER), ImmutableMap
                        .of(QianfanModelFactory.PROVIDER, QianfanModelFactory.DEFAULT_ENDPOINT));
    }

    private static List<Parameter.Dependency> getEnableSearchDependency() {
        return getDependency(CHAT_MODEL_PROVIDER.getName(),
                Lists.newArrayList(DashscopeModelFactory.PROVIDER),
                ImmutableMap.of(DashscopeModelFactory.PROVIDER, "false"));
    }

    private static List<Parameter.Dependency> getSecretKeyDependency() {
        return getDependency(CHAT_MODEL_PROVIDER.getName(),
                Lists.newArrayList(QianfanModelFactory.PROVIDER), ImmutableMap.of(
                        QianfanModelFactory.PROVIDER, ModelProvider.DEMO_CHAT_MODEL.getApiKey()));
    }

    private static List<Parameter.Dependency> getDependency(String dependencyParameterName,
            List<String> includesValue, Map<String, String> setDefaultValue) {

        Parameter.Dependency.Show show = new Parameter.Dependency.Show();
        show.setIncludesValue(includesValue);

        Parameter.Dependency dependency = new Parameter.Dependency();
        dependency.setName(dependencyParameterName);
        dependency.setShow(show);
        dependency.setSetDefaultValue(setDefaultValue);
        List<Parameter.Dependency> dependencies = new ArrayList<>();
        dependencies.add(dependency);
        return dependencies;
    }
}
