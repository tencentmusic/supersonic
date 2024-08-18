package com.tencent.supersonic.util;

import com.tencent.supersonic.common.pojo.ChatModelConfig;

public class LLMConfigUtils {
    public enum LLMType {
        GPT,
        MOONSHOT,
        DEEPSEEK,
        QWEN,
        GLM
    }

    public static ChatModelConfig getLLMConfig(LLMType type) {
        String baseUrl;
        String apiKey;
        String modelName;
        double temperature = 0.0;

        switch (type) {
            case GLM:
                baseUrl = "https://open.bigmodel.cn/api/pas/v4/";
                apiKey = "REPLACE_WITH_YOUR_KEY";
                modelName = "glm-4";
                break;
            case MOONSHOT:
                baseUrl = "https://api.moonshot.cn/v1";
                apiKey = "REPLACE_WITH_YOUR_KEY";
                modelName = "moonshot-v1-8k";
                break;
            case DEEPSEEK:
                baseUrl = "https://api.deepseek.com";
                apiKey = "REPLACE_WITH_YOUR_KEY";
                modelName = "deepseek-coder";
                break;
            case QWEN:
                baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
                apiKey = "REPLACE_WITH_YOUR_KEY";
                modelName = "qwen-turbo";
                temperature = 0.01;
                break;
            case GPT:
            default:
                baseUrl = "https://api.openai.com/v1";
                apiKey = "REPLACE_WITH_YOUR_KEY";
                modelName = "gpt-3.5-turbo";
                temperature = 0.0;
        }
        ChatModelConfig chatModel = new ChatModelConfig();
        chatModel.setModelName(modelName);
        chatModel.setBaseUrl(baseUrl);
        chatModel.setApiKey(apiKey);
        chatModel.setTemperature(temperature);
        chatModel.setProvider("open_ai");

        return chatModel;
    }
}
