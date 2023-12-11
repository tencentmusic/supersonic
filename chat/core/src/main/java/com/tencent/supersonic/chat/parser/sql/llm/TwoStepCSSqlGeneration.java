package com.tencent.supersonic.chat.parser.sql.llm;


import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq.SqlGenerationMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TwoStepCSSqlGeneration implements SqlGeneration, InitializingBean {

    @Override
    public String generation(LLMReq llmReq, String modelClusterKey) {
        //TODO
        return "";
    }

    @Override
    public void afterPropertiesSet() {
        SqlGenerationFactory.addSqlGenerationForFactory(SqlGenerationMode.TWO_STEP_CS, this);
    }
}
