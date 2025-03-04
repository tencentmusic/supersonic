package com.tencent.supersonic.headless.chat.parser.llm;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMResp;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.provider.ModelProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * SqlGenStrategy abstracts generation step so that different LLM prompting strategies can be
 * implemented.
 */
@Service
public abstract class SqlGenStrategy implements InitializingBean {

    @Autowired
    protected PromptHelper promptHelper;

    protected ChatLanguageModel getChatLanguageModel(ChatModelConfig modelConfig) {
        return ModelProvider.getChatModel(modelConfig);
    }

    public abstract LLMResp generate(LLMReq llmReq);

    public StreamingChatLanguageModel createStreamChatModel(ChatModelConfig modelConfig) {
        return OpenAiStreamingChatModel.builder().baseUrl(modelConfig.getBaseUrl())
                .modelName(modelConfig.getModelName()).apiKey(modelConfig.keyDecrypt())
                .temperature(modelConfig.getTemperature())
                .topP(modelConfig.getTopP())
                .timeout(Duration.ofSeconds(modelConfig.getTimeOut()))
                .logRequests(modelConfig.getLogRequests())
                .logResponses(modelConfig.getLogResponses()).build();
    }
}
