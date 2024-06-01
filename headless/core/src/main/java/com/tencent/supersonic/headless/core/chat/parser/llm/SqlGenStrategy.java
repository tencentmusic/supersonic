package com.tencent.supersonic.headless.core.chat.parser.llm;

import com.tencent.supersonic.headless.api.pojo.LLMConfig;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.headless.core.config.ParserConfig;
import com.tencent.supersonic.headless.core.utils.S2ChatModelProvider;
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
    protected ExemplarManager exemplarManager;

    @Autowired
    protected ParserConfig parserConfig;

    @Autowired
    protected PromptGenerator promptGenerator;

    protected ChatLanguageModel getChatLanguageModel(LLMConfig llmConfig) {
        return S2ChatModelProvider.provide(llmConfig);
    }

    abstract LLMResp generate(LLMReq llmReq);
}
