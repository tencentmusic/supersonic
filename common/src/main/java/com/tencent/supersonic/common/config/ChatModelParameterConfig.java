package com.tencent.supersonic.common.config;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.Parameter;
import dev.ai4j.openai4j.chat.ChatCompletionModel;
import dev.langchain4j.model.dashscope.QwenModelName;
import dev.langchain4j.provider.AzureModelFactory;
import dev.langchain4j.provider.DashscopeModelFactory;
import dev.langchain4j.provider.LocalAiModelFactory;
import dev.langchain4j.provider.OllamaModelFactory;
import dev.langchain4j.provider.OpenAiModelFactory;
import dev.langchain4j.provider.QianfanModelFactory;
import dev.langchain4j.provider.ZhipuModelFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("ChatModelParameterConfig")
@Slf4j
public class ChatModelParameterConfig extends ParameterConfig {

    public static final Parameter CHAT_MODEL_PROVIDER =
            new Parameter("s2.chat.model.provider", OpenAiModelFactory.PROVIDER,
                    "接口协议", "", "list",
                    "对话模型配置", getCandidateValues());

    public static final Parameter CHAT_MODEL_BASE_URL =
            new Parameter("s2.chat.model.base.url", OpenAiModelFactory.DEFAULT_BASE_URL,
                    "BaseUrl", "", "string",
                    "对话模型配置", null, getBaseUrlDependency());
    public static final Parameter CHAT_MODEL_ENDPOINT =
            new Parameter("s2.chat.model.endpoint", "llama_2_70b",
                    "Endpoint", "", "string",
                    "对话模型配置", null, getEndpointDependency());
    public static final Parameter CHAT_MODEL_API_KEY =
            new Parameter("s2.chat.model.api.key", DEMO,
                    "ApiKey", "", "password",
                    "对话模型配置", null, getApiKeyDependency()
            );
    public static final Parameter CHAT_MODEL_SECRET_KEY =
            new Parameter("s2.chat.model.secretKey", "demo",
                    "SecretKey", "", "password",
                    "对话模型配置", null, getSecretKeyDependency());

    public static final Parameter CHAT_MODEL_NAME =
            new Parameter("s2.chat.model.name", "gpt-3.5-turbo",
                    "ModelName", "", "string",
                    "对话模型配置", null, getModelNameDependency());

    public static final Parameter CHAT_MODEL_TEMPERATURE =
            new Parameter("s2.chat.model.temperature", "0.0",
                    "Temperature", "",
                    "slider", "对话模型配置");

    public static final Parameter CHAT_MODEL_TIMEOUT =
            new Parameter("s2.chat.model.timeout", "60",
                    "超时时间(秒)", "",
                    "number", "对话模型配置");

    @Override
    public List<Parameter> getSysParameters() {
        return Lists.newArrayList(
                CHAT_MODEL_PROVIDER, CHAT_MODEL_BASE_URL, CHAT_MODEL_ENDPOINT,
                CHAT_MODEL_API_KEY, CHAT_MODEL_SECRET_KEY, CHAT_MODEL_NAME,
                CHAT_MODEL_TEMPERATURE, CHAT_MODEL_TIMEOUT
        );
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

        return ChatModelConfig.builder()
                .provider(chatModelProvider)
                .baseUrl(chatModelBaseUrl)
                .apiKey(chatModelApiKey)
                .modelName(chatModelName)
                .temperature(Double.valueOf(chatModelTemperature))
                .timeOut(Long.valueOf(chatModelTimeout))
                .endpoint(endpoint)
                .secretKey(secretKey)
                .build();
    }

    private static List<String> getCandidateValues() {
        List<String> candidateValues = getBaseUrlCandidateValues();
        candidateValues.add(AzureModelFactory.PROVIDER);
        return candidateValues;
    }

    private static ArrayList<String> getBaseUrlCandidateValues() {
        return Lists.newArrayList(
                OpenAiModelFactory.PROVIDER,
                OllamaModelFactory.PROVIDER,
                QianfanModelFactory.PROVIDER,
                ZhipuModelFactory.PROVIDER,
                LocalAiModelFactory.PROVIDER,
                DashscopeModelFactory.PROVIDER);
    }

    private static List<Parameter.Dependency> getBaseUrlDependency() {
        return getDependency(CHAT_MODEL_PROVIDER.getName(),
                getBaseUrlCandidateValues(),
                ImmutableMap.of(
                        OpenAiModelFactory.PROVIDER, OpenAiModelFactory.DEFAULT_BASE_URL,
                        OllamaModelFactory.PROVIDER, OllamaModelFactory.DEFAULT_BASE_URL,
                        QianfanModelFactory.PROVIDER, QianfanModelFactory.DEFAULT_BASE_URL,
                        ZhipuModelFactory.PROVIDER, ZhipuModelFactory.DEFAULT_BASE_URL,
                        LocalAiModelFactory.PROVIDER, LocalAiModelFactory.DEFAULT_BASE_URL,
                        DashscopeModelFactory.PROVIDER, DashscopeModelFactory.DEFAULT_BASE_URL)
        );
    }

    private static List<Parameter.Dependency> getApiKeyDependency() {
        return getDependency(CHAT_MODEL_PROVIDER.getName(),
                Lists.newArrayList(
                        OpenAiModelFactory.PROVIDER,
                        QianfanModelFactory.PROVIDER,
                        ZhipuModelFactory.PROVIDER,
                        LocalAiModelFactory.PROVIDER,
                        AzureModelFactory.PROVIDER,
                        DashscopeModelFactory.PROVIDER
                ),
                ImmutableMap.of(
                        OpenAiModelFactory.PROVIDER, DEMO,
                        QianfanModelFactory.PROVIDER, DEMO,
                        ZhipuModelFactory.PROVIDER, DEMO,
                        LocalAiModelFactory.PROVIDER, DEMO,
                        AzureModelFactory.PROVIDER, DEMO,
                        DashscopeModelFactory.PROVIDER, DEMO
                ));
    }

    private static List<Parameter.Dependency> getModelNameDependency() {
        return getDependency(CHAT_MODEL_PROVIDER.getName(),
                getCandidateValues(),
                ImmutableMap.of(
                        OpenAiModelFactory.PROVIDER, "gpt-3.5-turbo",
                        OllamaModelFactory.PROVIDER, "qwen:0.5b",
                        QianfanModelFactory.PROVIDER, "Llama-2-70b-chat",
                        ZhipuModelFactory.PROVIDER, ChatCompletionModel.GPT_4.toString(),
                        LocalAiModelFactory.PROVIDER, "ggml-gpt4all-j",
                        AzureModelFactory.PROVIDER, "gpt-35-turbo",
                        DashscopeModelFactory.PROVIDER, QwenModelName.QWEN_PLUS
                )
        );
    }

    private static List<Parameter.Dependency> getEndpointDependency() {
        return getDependency(CHAT_MODEL_PROVIDER.getName(),
                Lists.newArrayList(
                        AzureModelFactory.PROVIDER,
                        QianfanModelFactory.PROVIDER
                ),
                ImmutableMap.of(
                        AzureModelFactory.PROVIDER, AzureModelFactory.DEFAULT_BASE_URL,
                        QianfanModelFactory.PROVIDER, "llama_2_70b"
                )
        );
    }

    private static List<Parameter.Dependency> getSecretKeyDependency() {
        return getDependency(CHAT_MODEL_PROVIDER.getName(),
                Lists.newArrayList(QianfanModelFactory.PROVIDER),
                ImmutableMap.of(
                        QianfanModelFactory.PROVIDER, DEMO
                )
        );
    }
}
