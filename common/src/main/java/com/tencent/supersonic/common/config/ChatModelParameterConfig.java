package com.tencent.supersonic.common.config;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.Parameter;
import dev.langchain4j.provider.AzureModelFactory;
import dev.langchain4j.provider.DashscopeModelFactory;
import dev.langchain4j.provider.LocalAiModelFactory;
import dev.langchain4j.provider.OllamaModelFactory;
import dev.langchain4j.provider.OpenAiModelFactory;
import dev.langchain4j.provider.QianfanModelFactory;
import dev.langchain4j.provider.ZhipuModelFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("ChatModelParameterConfig")
@Slf4j
public class ChatModelParameterConfig extends ParameterConfig {

    public static final Parameter CHAT_MODEL_PROVIDER =
            new Parameter(
                    "s2.chat.model.provider",
                    OpenAiModelFactory.PROVIDER,
                    "接口协议",
                    "",
                    "list",
                    "对话模型配置",
                    getCandidateValues());

    public static final Parameter CHAT_MODEL_BASE_URL =
            new Parameter(
                    "s2.chat.model.base.url",
                    OpenAiModelFactory.DEFAULT_BASE_URL,
                    "BaseUrl",
                    "",
                    "string",
                    "对话模型配置",
                    null,
                    getBaseUrlDependency());
    public static final Parameter CHAT_MODEL_ENDPOINT =
            new Parameter(
                    "s2.chat.model.endpoint",
                    "llama_2_70b",
                    "Endpoint",
                    "",
                    "string",
                    "对话模型配置",
                    null,
                    getEndpointDependency());
    public static final Parameter CHAT_MODEL_API_KEY =
            new Parameter(
                    "s2.chat.model.api.key",
                    DEMO,
                    "ApiKey",
                    "",
                    "password",
                    "对话模型配置",
                    null,
                    getApiKeyDependency());
    public static final Parameter CHAT_MODEL_SECRET_KEY =
            new Parameter(
                    "s2.chat.model.secretKey",
                    "demo",
                    "SecretKey",
                    "",
                    "password",
                    "对话模型配置",
                    null,
                    getSecretKeyDependency());

    public static final Parameter CHAT_MODEL_NAME =
            new Parameter(
                    "s2.chat.model.name",
                    "gpt-4o-mini",
                    "ModelName",
                    "",
                    "string",
                    "对话模型配置",
                    null,
                    getModelNameDependency());

    public static final Parameter CHAT_MODEL_ENABLE_SEARCH =
            new Parameter(
                    "s2.chat.model.enableSearch",
                    "false",
                    "是否启用搜索增强功能，设为false表示不启用",
                    "",
                    "bool",
                    "对话模型配置",
                    null,
                    getEnableSearchDependency());

    public static final Parameter CHAT_MODEL_TEMPERATURE =
            new Parameter(
                    "s2.chat.model.temperature", "0.0", "Temperature", "", "slider", "对话模型配置");

    public static final Parameter CHAT_MODEL_TIMEOUT =
            new Parameter("s2.chat.model.timeout", "60", "超时时间(秒)", "", "number", "对话模型配置");

    @Override
    public List<Parameter> getSysParameters() {
        return Lists.newArrayList(
                CHAT_MODEL_PROVIDER,
                CHAT_MODEL_BASE_URL,
                CHAT_MODEL_ENDPOINT,
                CHAT_MODEL_API_KEY,
                CHAT_MODEL_SECRET_KEY,
                CHAT_MODEL_NAME,
                CHAT_MODEL_ENABLE_SEARCH,
                CHAT_MODEL_TEMPERATURE,
                CHAT_MODEL_TIMEOUT);
    }

    public ChatModelConfig convert() {
        String chatModelProvider = getParameterValue(CHAT_MODEL_PROVIDER);
        String chatModelBaseUrl = getParameterValue(CHAT_MODEL_BASE_URL);
        String chatModelApiKey = getParameterValue(CHAT_MODEL_API_KEY);
        String chatModelName = getParameterValue(CHAT_MODEL_NAME);
        String chatModelTemperature = getParameterValue(CHAT_MODEL_TEMPERATURE);
        String chatModelTimeout = getParameterValue(CHAT_MODEL_TIMEOUT);
        String endpoint = getParameterValue(CHAT_MODEL_ENDPOINT);
        String secretKey = getParameterValue(CHAT_MODEL_SECRET_KEY);
        String enableSearch = getParameterValue(CHAT_MODEL_ENABLE_SEARCH);

        return ChatModelConfig.builder()
                .provider(chatModelProvider)
                .baseUrl(chatModelBaseUrl)
                .apiKey(chatModelApiKey)
                .modelName(chatModelName)
                .enableSearch(Boolean.valueOf(enableSearch))
                .temperature(Double.valueOf(chatModelTemperature))
                .timeOut(Long.valueOf(chatModelTimeout))
                .endpoint(endpoint)
                .secretKey(secretKey)
                .build();
    }

    private static List<String> getCandidateValues() {
        return Lists.newArrayList(
                OpenAiModelFactory.PROVIDER,
                AzureModelFactory.PROVIDER,
                OllamaModelFactory.PROVIDER,
                QianfanModelFactory.PROVIDER,
                ZhipuModelFactory.PROVIDER,
                LocalAiModelFactory.PROVIDER,
                DashscopeModelFactory.PROVIDER);
    }

    private static List<Parameter.Dependency> getBaseUrlDependency() {
        return getDependency(
                CHAT_MODEL_PROVIDER.getName(),
                getCandidateValues(),
                ImmutableMap.of(
                        OpenAiModelFactory.PROVIDER, OpenAiModelFactory.DEFAULT_BASE_URL,
                        AzureModelFactory.PROVIDER, AzureModelFactory.DEFAULT_BASE_URL,
                        OllamaModelFactory.PROVIDER, OllamaModelFactory.DEFAULT_BASE_URL,
                        QianfanModelFactory.PROVIDER, QianfanModelFactory.DEFAULT_BASE_URL,
                        ZhipuModelFactory.PROVIDER, ZhipuModelFactory.DEFAULT_BASE_URL,
                        LocalAiModelFactory.PROVIDER, LocalAiModelFactory.DEFAULT_BASE_URL,
                        DashscopeModelFactory.PROVIDER, DashscopeModelFactory.DEFAULT_BASE_URL));
    }

    private static List<Parameter.Dependency> getApiKeyDependency() {
        return getDependency(
                CHAT_MODEL_PROVIDER.getName(),
                Lists.newArrayList(
                        OpenAiModelFactory.PROVIDER,
                        QianfanModelFactory.PROVIDER,
                        ZhipuModelFactory.PROVIDER,
                        LocalAiModelFactory.PROVIDER,
                        AzureModelFactory.PROVIDER,
                        DashscopeModelFactory.PROVIDER),
                ImmutableMap.of(
                        OpenAiModelFactory.PROVIDER, DEMO,
                        QianfanModelFactory.PROVIDER, DEMO,
                        ZhipuModelFactory.PROVIDER, DEMO,
                        LocalAiModelFactory.PROVIDER, DEMO,
                        AzureModelFactory.PROVIDER, DEMO,
                        DashscopeModelFactory.PROVIDER, DEMO));
    }

    private static List<Parameter.Dependency> getModelNameDependency() {
        return getDependency(
                CHAT_MODEL_PROVIDER.getName(),
                getCandidateValues(),
                ImmutableMap.of(
                        OpenAiModelFactory.PROVIDER, OpenAiModelFactory.DEFAULT_MODEL_NAME,
                        OllamaModelFactory.PROVIDER, OllamaModelFactory.DEFAULT_MODEL_NAME,
                        QianfanModelFactory.PROVIDER, QianfanModelFactory.DEFAULT_MODEL_NAME,
                        ZhipuModelFactory.PROVIDER, ZhipuModelFactory.DEFAULT_MODEL_NAME,
                        LocalAiModelFactory.PROVIDER, LocalAiModelFactory.DEFAULT_MODEL_NAME,
                        AzureModelFactory.PROVIDER, AzureModelFactory.DEFAULT_MODEL_NAME,
                        DashscopeModelFactory.PROVIDER, DashscopeModelFactory.DEFAULT_MODEL_NAME));
    }

    private static List<Parameter.Dependency> getEndpointDependency() {
        return getDependency(
                CHAT_MODEL_PROVIDER.getName(),
                Lists.newArrayList(QianfanModelFactory.PROVIDER),
                ImmutableMap.of(
                        QianfanModelFactory.PROVIDER, QianfanModelFactory.DEFAULT_ENDPOINT));
    }

    private static List<Parameter.Dependency> getEnableSearchDependency() {
        return getDependency(
                CHAT_MODEL_PROVIDER.getName(),
                Lists.newArrayList(DashscopeModelFactory.PROVIDER),
                ImmutableMap.of(DashscopeModelFactory.PROVIDER, "false"));
    }

    private static List<Parameter.Dependency> getSecretKeyDependency() {
        return getDependency(
                CHAT_MODEL_PROVIDER.getName(),
                Lists.newArrayList(QianfanModelFactory.PROVIDER),
                ImmutableMap.of(QianfanModelFactory.PROVIDER, DEMO));
    }
}
