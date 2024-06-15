package com.tencent.supersonic.headless.chat.parser.llm;

import com.tencent.supersonic.common.config.LLMConfig;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.common.util.S2ChatModelProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * SqlGenStrategy abstracts generation step so that
 * different LLM prompting strategies can be implemented.
 */
@Service
public abstract class SqlGenStrategy implements InitializingBean {

    protected static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");

    @Autowired
    protected PromptHelper promptHelper;

    protected ChatLanguageModel getChatLanguageModel(LLMConfig llmConfig) {
        return S2ChatModelProvider.provide(llmConfig);
    }

    abstract LLMResp generate(LLMReq llmReq);
}
