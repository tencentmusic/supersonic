package com.tencent.supersonic.headless.core.chat.parser.llm;


import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * LLMProxy based on langchain4j Java version.
 */
@Slf4j
@Component
public class JavaLLMProxy implements LLMProxy {

    public LLMResp text2sql(LLMReq llmReq) {

        SqlGenStrategy sqlGenStrategy = SqlGenStrategyFactory.get(llmReq.getSqlGenType());
        String modelName = llmReq.getSchema().getDataSetName();
        LLMResp result = sqlGenStrategy.generate(llmReq);
        result.setQuery(llmReq.getQueryText());
        result.setModelName(modelName);
        return result;
    }

}
