package com.tencent.supersonic.headless.core.chat.parser.llm;

import com.tencent.supersonic.headless.api.pojo.LLMConfig;
import com.tencent.supersonic.headless.core.config.OptimizationConfig;
import com.tencent.supersonic.headless.core.utils.S2ChatModelProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public abstract class BaseSqlGeneration implements SqlGeneration, InitializingBean {

    protected static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");

    @Autowired
    protected SqlExamplarLoader sqlExamplarLoader;

    @Autowired
    protected OptimizationConfig optimizationConfig;

    @Autowired
    protected SqlPromptGenerator sqlPromptGenerator;

    protected ChatLanguageModel getChatLanguageModel(LLMConfig llmConfig) {
        return S2ChatModelProvider.provide(llmConfig);
    }

}
