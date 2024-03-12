package com.tencent.supersonic.headless.core.chat.parser;


import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.core.chat.parser.llm.SqlGeneration;
import com.tencent.supersonic.headless.core.chat.parser.llm.SqlGenerationFactory;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMReq.SqlGenerationMode;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.headless.core.pojo.QueryContext;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * LLMProxy based on langchain4j Java version.
 */
@Slf4j
@Component
public class JavaLLMProxy implements LLMProxy {

    private static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");

    @Override
    public boolean isSkip(QueryContext queryContext) {
        ChatLanguageModel chatLanguageModel = ContextUtils.getBean(ChatLanguageModel.class);
        if (Objects.isNull(chatLanguageModel)) {
            log.warn("chatLanguageModel is null, skip :{}", JavaLLMProxy.class.getName());
            return true;
        }
        return false;
    }

    public LLMResp query2sql(LLMReq llmReq, Long dataSetId) {

        SqlGeneration sqlGeneration = SqlGenerationFactory.get(
                SqlGenerationMode.getMode(llmReq.getSqlGenerationMode()));
        String modelName = llmReq.getSchema().getDataSetName();
        LLMResp result = sqlGeneration.generation(llmReq, dataSetId);
        result.setQuery(llmReq.getQueryText());
        result.setModelName(modelName);
        return result;
    }

}
