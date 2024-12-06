package com.tencent.supersonic.util;

import com.tencent.supersonic.common.pojo.ChatModelConfig;

public class LLMConfigUtils {
    public enum LLMType {
        OPENAI_GPT(false),
        OPENAI_MOONSHOT(false),
        OPENAI_DEEPSEEK(false),
        OPENAI_QWEN(false),
        OPENAI_GLM(false),
        OLLAMA_LLAMA3(true),
        OLLAMA_QWEN2(true),
        OLLAMA_QWEN25_7B(true),
        OLLAMA_QWEN25_14B(true),
        OLLAMA_QWEN25_CODE_7B(true),
        OLLAMA_QWEN25_CODE_3B(true),
        OLLAMA_GLM4(true);


        public boolean isOllam;

        LLMType(boolean isOllam) {
            this.isOllam = isOllam;
        }
    }

    public static ChatModelConfig getLLMConfig(LLMType type) {
        String baseUrl;
        String apiKey = "";
        String modelName;
        double temperature = 0.0;

        switch (type) {
            case OLLAMA_LLAMA3:
                baseUrl = "http://localhost:11434";
                modelName = "llama3.1:8b";
                break;
            case OLLAMA_QWEN2:
                baseUrl = "http://localhost:11434";
                modelName = "qwen2:7b";
                break;
            case OLLAMA_QWEN25_7B:
                baseUrl = "http://localhost:11434";
                modelName = "qwen2.5:7b";
                break;
            case OLLAMA_QWEN25_14B:
                baseUrl = "http://localhost:11434";
                modelName = "qwen2.5:14b";
                break;
            case OLLAMA_QWEN25_CODE_7B:
                baseUrl = "http://localhost:11434";
                modelName = "qwen2.5-coder:7b";
                break;
            case OLLAMA_QWEN25_CODE_3B:
                baseUrl = "http://localhost:11434";
                modelName = "qwen2.5-coder:3b";
                break;
            case OLLAMA_GLM4:
                baseUrl = "http://localhost:11434";
                modelName = "glm4:latest";
                break;
            case OPENAI_GLM:
                baseUrl = "https://open.bigmodel.cn/api/pas/v4/";
                apiKey = "REPLACE_WITH_YOUR_KEY";
                modelName = "glm-4";
                break;
            case OPENAI_MOONSHOT:
                baseUrl = "https://api.moonshot.cn/v1";
                apiKey = "REPLACE_WITH_YOUR_KEY";
                modelName = "moonshot-v1-8k";
                break;
            case OPENAI_DEEPSEEK:
                baseUrl = "https://api.deepseek.com";
                apiKey = "REPLACE_WITH_YOUR_KEY";
                modelName = "deepseek-coder";
                break;
            case OPENAI_QWEN:
                baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
                apiKey = "REPLACE_WITH_YOUR_KEY";
                modelName = "qwen-turbo";
                temperature = 0.01;
                break;
            case OPENAI_GPT:
            default:
                baseUrl = "https://api.openai.com/v1";
                apiKey = "REPLACE_WITH_YOUR_KEY";
                modelName = "gpt-4o";
                temperature = 0.0;
        }

        ChatModelConfig chatModelConfig;
        if (type.isOllam) {
            chatModelConfig = ChatModelConfig.builder().provider("OLLAMA").baseUrl(baseUrl)
                    .modelName(modelName).temperature(temperature).timeOut(60000L).build();
        } else {
            chatModelConfig =
                    ChatModelConfig.builder().provider("OPEN_AI").baseUrl(baseUrl).apiKey(apiKey)
                            .modelName(modelName).temperature(temperature).timeOut(60000L).build();
        }

        return chatModelConfig;
    }
}
