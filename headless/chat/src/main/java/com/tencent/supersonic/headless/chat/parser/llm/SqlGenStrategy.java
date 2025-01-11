package com.tencent.supersonic.headless.chat.parser.llm;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMResp;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.provider.ModelProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
